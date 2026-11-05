package org.sjhstudio.diary.core.backup

import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.BufferedSink
import okio.ByteString
import okio.ByteString.Companion.decodeHex
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import okio.Source
import okio.openZip
import org.sjhstudio.diary.core.data.RestoreRepository
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.datastore.BackupStore
import org.sjhstudio.diary.core.model.BackupEntry
import org.sjhstudio.diary.core.model.BackupEntryState
import org.sjhstudio.diary.core.model.BundleEstimate
import org.sjhstudio.diary.core.model.PLATFORM_LEGACY
import org.sjhstudio.diary.core.model.RESTORE_FROM_FILE
import org.sjhstudio.diary.core.model.RestoreFailure
import org.sjhstudio.diary.core.model.RestorePreparation
import org.sjhstudio.diary.core.model.RestoreResult
import org.sjhstudio.diary.core.model.RestoreState
import org.sjhstudio.diary.core.model.RollbackInfo
import org.sjhstudio.diary.core.platform.NetworkState

/** [RestoreRepository]의 구현. 글을 먼저 합치고 사진은 그 뒤에 받아서 받는 동안에도 일기를 볼 수 있다. */
class RestoreController(
    private val catalog: Lazy<RestoreCatalog>,
    private val planner: Lazy<RestorePlanner>,
    private val applier: Lazy<MergeApplier>,
    private val fetcher: Lazy<PhotoFetcher>,
    private val legacy: Lazy<LegacyBackupImporter>,
    private val rollbackManager: Lazy<RollbackManager>,
    private val exporter: Lazy<BundleExporter>,
    private val db: Lazy<DiaryDatabase>,
    private val store: BackupStore,
    private val network: NetworkState,
    private val fileSystem: FileSystem,
    private val workDir: Path,
    private val applySettings: suspend (ManifestSettings) -> Unit,
    private val scope: CoroutineScope,
    private val gate: BackupGate = BackupGate()
) : RestoreRepository {
    private val _state = MutableStateFlow<RestoreState>(RestoreState.Idle)
    override val state: StateFlow<RestoreState> = _state.asStateFlow()

    private var candidates = emptyList<RestoreCandidate>()
    private var prepared: PreparedRestore? = null
    private var preparedLegacy: LegacyPlan? = null
    private var preparedFile: RestorePreparation? = null

    override val rollback: Flow<RollbackInfo?> = flow { emitAll(rollbackManager.value.info) }

    override suspend fun rollBack(): Boolean {
        if (!gate.tryEnter()) return false
        return try {
            guarded { rollbackManager.value.rollBack() } ?: false
        } finally {
            gate.leave()
        }
    }

    override suspend fun backups(): List<BackupEntry>? = guarded {
        candidates = catalog.value.list()
        candidates.map { it.entry }
    }

    override suspend fun bundleEstimate(): BundleEstimate = exporter.value.estimate()

    override suspend fun writeBundle(sink: BufferedSink, onProgress: (done: Int, total: Int) -> Unit) =
        exporter.value.write(sink, onProgress)

    override suspend fun prepareFile(source: Source): RestorePreparation {
        prepared = null
        preparedLegacy = null
        val result =
            try {
                withContext(Dispatchers.IO) { readFile(source) }
            } catch (_: RestoreMismatch) {
                RestorePreparation.Failed(RestoreFailure.MISMATCH)
            } catch (_: RestoreNeedsUpdate) {
                RestorePreparation.Failed(RestoreFailure.NEEDS_UPDATE)
            } catch (error: CancellationException) {
                throw error
            } catch (_: IOException) {
                RestorePreparation.Failed(RestoreFailure.MISMATCH)
            }
        preparedFile = result
        return result
    }

    private suspend fun readFile(source: Source): RestorePreparation {
        val incoming = workDir / INCOMING
        fileSystem.createDirectories(incoming)
        val path = incoming / PICKED
        fileSystem.delete(path, mustExist = false)
        fileSystem.write(path) { writeAll(source) }
        val head = fileSystem.read(path) { if (request(HEAD)) readByteString(HEAD) else ByteString.EMPTY }
        return when {
            head == ZIP_HEAD -> readZip(path)
            head == SQLITE_HEAD -> readLegacy(path, zipped = false)
            else -> throw RestoreMismatch()
        }
    }

    private suspend fun readZip(path: Path): RestorePreparation {
        val root = "/".toPath()
        val archive = fileSystem.openZip(path)
        if (!archive.exists(root / BundleExporter.MANIFEST)) {
            if (!archive.exists(root / BundleExporter.LEGACY_DB)) throw RestoreMismatch()
            return readLegacy(path, zipped = true)
        }
        val ready = planner.value.prepareBundle(path)
        prepared = ready
        return RestorePreparation.Ready(fileEntry(ready), ready.preview)
    }

    private suspend fun readLegacy(path: Path, zipped: Boolean): RestorePreparation {
        val plan = legacy.value.prepareFile(path, zipped)
        preparedLegacy = plan
        val now = Instant.fromEpochMilliseconds(fileSystem.metadata(path).lastModifiedAtMillis ?: 0)
        return RestorePreparation.Ready(
            BackupEntry(RESTORE_FROM_FILE, PLATFORM_LEGACY, "", PLATFORM_LEGACY, now, 0, 0, 0, "", BackupEntryState.Ready),
            plan.preview
        )
    }

    private fun fileEntry(ready: PreparedRestore): BackupEntry {
        val manifest = ready.manifest
        return BackupEntry(
            snapshotName = RESTORE_FROM_FILE,
            deviceId = manifest.device.id,
            deviceName = manifest.device.name,
            platform = manifest.app.platform,
            createdAt = Instant.fromEpochMilliseconds(manifest.createdAt),
            entryCount = manifest.counts.entries,
            photoCount = manifest.media.count { it.inBackup },
            photosNotInBackup = manifest.media.count { !it.inBackup },
            appVersion = manifest.app.versionName,
            state = BackupEntryState.Ready
        )
    }

    override suspend fun prepare(snapshotName: String): RestorePreparation {
        if (snapshotName == RESTORE_FROM_FILE) return preparedFile ?: RestorePreparation.Failed(RestoreFailure.OTHER)
        // 앱이 꺼졌다 돌아오면 목록이 비어 있다.
        if (candidates.isEmpty()) backups()
        val candidate =
            candidates.firstOrNull { it.entry.snapshotName == snapshotName } ?: return RestorePreparation.Failed(RestoreFailure.OTHER)
        return try {
            prepared = null
            preparedLegacy = null
            val file = candidate.legacy
            if (file != null) {
                val plan = legacy.value.prepare(file)
                preparedLegacy = plan
                RestorePreparation.Ready(candidate.entry, plan.preview)
            } else {
                val ready = planner.value.prepare(candidate)
                prepared = ready
                RestorePreparation.Ready(candidate.entry, ready.preview)
            }
        } catch (_: RestoreMismatch) {
            RestorePreparation.Failed(RestoreFailure.MISMATCH)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            RestorePreparation.Failed(RestoreFailure.OTHER)
        }
    }

    override fun merge() = run { ready ->
        if (ready != null) mergeBackup(ready) else importLegacy(requireNotNull(preparedLegacy))
    }

    override fun replace() = run { ready -> replaceWith(requireNotNull(ready)) }

    private fun run(work: suspend (PreparedRestore?) -> RestoreResult) {
        val ready = prepared
        if (ready == null && preparedLegacy == null) return
        if (_state.value == RestoreState.Merging) return
        _state.value = RestoreState.Merging
        scope.launch {
            // 도는 백업이 있으면 끝날 때까지 기다린다. 복원을 조용히 버리지 않는다.
            gate.enter()
            try {
                _state.value = RestoreState.Done(work(ready))
            } catch (error: CancellationException) {
                throw error
            } catch (_: NoRoomForRollback) {
                _state.value = RestoreState.Failed(RestoreFailure.NO_SPACE)
            } catch (_: Exception) {
                _state.value = RestoreState.Failed(RestoreFailure.OTHER)
            } finally {
                gate.leave()
            }
        }
    }

    private suspend fun importLegacy(plan: LegacyPlan): RestoreResult {
        val result = legacy.value.apply(plan)
        preparedLegacy = null
        return result
    }

    private suspend fun replaceWith(ready: PreparedRestore): RestoreResult {
        val backup = ready.plan.items.map { it.first }
        rollbackManager.value.replace(backup, SOURCE_DRIVE)
        return settle(MergeCounts(backup.count { it.entry.deletedAt == null }, 0, 0), ready).copy(replaced = true)
    }

    private suspend fun mergeBackup(ready: PreparedRestore): RestoreResult {
        // 미리보기 뒤에 일기를 고쳤을 수 있어 합치기 직전의 기기 상태로 다시 가른다.
        val plan = applier.value.plan(ready.plan.items.map { it.first })
        return settle(applier.value.apply(plan, SOURCE_DRIVE), ready)
    }

    private suspend fun settle(counts: MergeCounts, ready: PreparedRestore): RestoreResult {
        applySettings(ready.manifest.settings)
        fileSystem.delete(ready.snapshot, mustExist = false)
        prepared = null
        return fetchPhotos(counts, ready)
    }

    private suspend fun fetchPhotos(counts: MergeCounts, ready: PreparedRestore): RestoreResult {
        val dao = db.value.backupDao()
        val entries = dao.allEntries().count { it.deletedAt == null }
        val total = dao.pendingFiles().size
        _state.value = RestoreState.FetchingPhotos(entries, 0, total)
        var fetched = 0
        var broken = 0
        try {
            val progress = { done: Int, all: Int -> _state.value = RestoreState.FetchingPhotos(entries, done, all) }
            val zip = ready.bundle
            val result = if (zip != null) fetcher.value.fetchFrom(zip, progress) else fetcher.value.fetch(progress)
            fetched = result.first
            broken = result.second
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            // 글은 이미 합쳤다. 남은 사진은 다음에 앱을 열 때 이어 받는다.
        }
        ready.bundle?.let { fileSystem.delete(it, mustExist = false) }
        val left = dao.pendingFiles().size
        val onOriginal = minOf(left, ready.manifest.media.count { !it.inBackup })
        return RestoreResult(
            entriesAdded = counts.added,
            entriesUpdated = counts.updated,
            entriesKeptBoth = counts.keptBoth,
            photosRestored = fetched,
            photosMissing = broken,
            photosPending = left - onOriginal,
            photosOnOriginal = onOriginal,
            lockWasOn = ready.manifest.settings.lockEnabled
        )
    }

    override fun dismiss() {
        _state.value = RestoreState.Idle
    }

    override suspend fun resumePhotos() {
        guarded { rollbackManager.value.expire() }
        val prefs = store.current()
        if (prefs.accountKey == null || !network.isConnected) return
        if (!network.isUnmetered && !prefs.photosOnCellular) return
        if (!gate.tryEnter()) return
        try {
            if (db.value.backupDao().pendingFiles().isNotEmpty()) guarded { fetcher.value.fetch() }
        } finally {
            gate.leave()
        }
    }

    private inline fun <T> guarded(block: () -> T): T? = try {
        block()
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        null
    }

    private companion object {
        const val SOURCE_DRIVE = "drive"
        const val INCOMING = "incoming"
        const val PICKED = "picked"
        const val HEAD = 4L
        val ZIP_HEAD = "504b0304".decodeHex()
        val SQLITE_HEAD = "53514c69".decodeHex()
    }
}
