package org.sjhstudio.diary.core.backup

import kotlin.concurrent.Volatile
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.BufferedSink
import okio.ByteString
import okio.ByteString.Companion.decodeHex
import okio.FileSystem
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
import org.sjhstudio.diary.core.model.PhotoProgress
import org.sjhstudio.diary.core.model.RESTORE_FROM_FILE
import org.sjhstudio.diary.core.model.RestoreFailure
import org.sjhstudio.diary.core.model.RestorePreparation
import org.sjhstudio.diary.core.model.RestoreResult
import org.sjhstudio.diary.core.model.RestoreState
import org.sjhstudio.diary.core.model.RollbackInfo
import org.sjhstudio.diary.core.platform.BackupScheduler
import org.sjhstudio.diary.core.platform.NetworkState
import org.sjhstudio.diary.core.platform.NoBackupScheduler

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
    private val gate: BackupGate = BackupGate(),
    private val scheduler: BackupScheduler = NoBackupScheduler
) : RestoreRepository {
    private val _photoProgress = MutableStateFlow<PhotoProgress?>(null)
    override val photoProgress: StateFlow<PhotoProgress?> = _photoProgress.asStateFlow()

    private val _state = MutableStateFlow<RestoreState>(RestoreState.Idle)
    override val state: StateFlow<RestoreState> = _state.asStateFlow()

    @Volatile
    private var candidates = emptyList<RestoreCandidate>()

    @Volatile
    private var prepared: PreparedRestore? = null

    @Volatile
    private var preparedLegacy: LegacyPlan? = null

    @Volatile
    private var preparedFile: RestorePreparation? = null

    /** 받아 둔 사본을 만들고 지우는 일이 겹치지 않게 한다. */
    private val preparing = Mutex()

    /** 사본을 읽고 일기 수를 세는 일이 메인 스레드에서 돌지 않게 한다. */
    override val rollback: Flow<RollbackInfo?> = flow { emitAll(rollbackManager.value.info) }.flowOn(Dispatchers.IO)

    /** 화면을 떠나도 끝까지 돈다. 중간에 멈추면 일기는 돌아왔는데 되돌리기가 남는다. */
    override suspend fun rollBack(): Boolean = scope.async {
        if (!gate.tryEnter()) return@async false
        try {
            guarded { rollbackManager.value.rollBack() } ?: false
        } finally {
            gate.leave()
        }
    }.await()

    override suspend fun backups(): List<BackupEntry>? = guarded {
        candidates = catalog.value.list()
        candidates.map { it.entry }
    }

    override suspend fun bundleEstimate(): BundleEstimate = exporter.value.estimate()

    override suspend fun writeBundle(sink: BufferedSink, onProgress: (done: Int, total: Int) -> Unit) =
        exporter.value.write(sink, onProgress)

    override suspend fun prepareFile(source: Source): RestorePreparation = preparingCopy {
        val result =
            try {
                readFile(source)
            } catch (_: RestoreMismatch) {
                RestorePreparation.Failed(RestoreFailure.MISMATCH)
            } catch (_: RestoreNeedsUpdate) {
                RestorePreparation.Failed(RestoreFailure.NEEDS_UPDATE)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                RestorePreparation.Failed(RestoreFailure.MISMATCH)
            }
        if (result is RestorePreparation.Failed) clearCopies()
        preparedFile = result
        result
    }

    /**
     * 준비는 한 번에 하나만, 메인 스레드 밖에서 돈다. 지난번 사본을 지우고 시작하고 멈추면 쓰던 사본을 남기지 않는다.
     * 받는 파일은 일기 사본이라서 쓰지 않게 되면 바로 지운다.
     */
    private suspend fun <T> preparingCopy(block: suspend () -> T): T = preparing.withLock {
        withContext(Dispatchers.IO) {
            try {
                clearCopies()
                block()
            } catch (error: CancellationException) {
                clearCopies()
                throw error
            }
        }
    }

    /** 합치는 중이거나 사진을 꺼내는 중이면 그 일이 쓰는 파일은 두고 잊기만 한다. */
    private fun clearCopies() {
        prepared = null
        preparedLegacy = null
        preparedFile = null
        if (!restoring) fileSystem.deleteRecursively(workDir / INCOMING, mustExist = false)
    }

    private val restoring: Boolean
        get() = _state.value == RestoreState.Merging || _state.value is RestoreState.FetchingPhotos

    private suspend fun readFile(source: Source): RestorePreparation {
        val incoming = workDir / INCOMING
        fileSystem.createDirectories(incoming)
        val path = incoming / PICKED
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
        return preparingCopy {
            val result = prepareFromDrive(snapshotName)
            if (result is RestorePreparation.Failed) clearCopies()
            result
        }
    }

    private suspend fun prepareFromDrive(snapshotName: String): RestorePreparation {
        // 앱이 꺼졌다 돌아오면 목록이 비어 있다.
        if (candidates.isEmpty()) backups()
        val candidate =
            candidates.firstOrNull { it.entry.snapshotName == snapshotName } ?: return RestorePreparation.Failed(RestoreFailure.OTHER)
        return try {
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
        fileSystem.delete(workDir / INCOMING / PICKED, mustExist = false)
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
        val entries = dao.visibleEntryCount()
        val total = dao.pendingFiles().size
        _state.value = RestoreState.FetchingPhotos(entries, 0, total)
        var fetched = 0
        var broken = 0
        try {
            val zip = ready.bundle
            val result =
                watchingPhotos(total) { watch ->
                    val progress = { done: Int, all: Int ->
                        watch(done, all)
                        _state.value = RestoreState.FetchingPhotos(entries, done, all)
                    }
                    if (zip != null) fetcher.value.fetchFrom(zip, progress) else fetcher.value.fetch(progress)
                }
            fetched = result.first
            broken = result.second
            // 다 꺼냈을 때만 지운다. 아직 사진이 들어 있으면 결과를 닫을 때 지운다.
            ready.bundle?.let { fileSystem.delete(it, mustExist = false) }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            // 글은 이미 합쳤다. 남은 사진은 다음에 앱을 열 때 이어 받는다.
        }
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

    /** 앱을 나가도 사진을 계속 받게 시스템에 알리고 진행을 내보낸다. */
    private suspend fun <T> watchingPhotos(total: Int, work: suspend (watch: (done: Int, all: Int) -> Unit) -> T): T {
        _photoProgress.value = PhotoProgress(0, total)
        scheduler.keepRestoreRunning()
        return try {
            work { done, all -> _photoProgress.value = PhotoProgress(done, all) }
        } finally {
            _photoProgress.value = null
        }
    }

    /** 미리보기를 닫거나 결과를 닫을 때 부른다. 복원이 도는 중이면 뒤로 간 것뿐이라 그대로 둔다. */
    override fun dismiss() {
        if (restoring) return
        _state.value = RestoreState.Idle
        scope.launch { preparing.withLock { withContext(Dispatchers.IO) { if (!restoring) clearCopies() } } }
    }

    override suspend fun resumePhotos() {
        guarded { rollbackManager.value.expire() }
        guarded { clearAbandonedCopies() }
        val prefs = store.current()
        if (prefs.accountKey == null || !network.isConnected) return
        if (!network.isUnmetered && !prefs.photosOnCellular) return
        if (!gate.tryEnter()) return
        try {
            guarded {
                val total = db.value.backupDao().pendingFiles().size
                if (total > 0) watchingPhotos(total) { watch -> fetcher.value.fetch(watch) }
            }
        } finally {
            gate.leave()
        }
    }

    /** 앱이 꺼지면 받아 둔 사본을 지울 기회가 없었다. 쓰는 곳이 없을 때만 지운다. */
    private suspend fun clearAbandonedCopies() = preparing.withLock {
        val inUse = prepared != null || preparedLegacy != null || preparedFile != null || restoring
        if (!inUse) withContext(Dispatchers.IO) { clearCopies() }
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
