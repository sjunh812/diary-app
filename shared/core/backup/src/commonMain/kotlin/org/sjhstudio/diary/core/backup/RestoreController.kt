package org.sjhstudio.diary.core.backup

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import okio.FileSystem
import org.sjhstudio.diary.core.data.RestoreRepository
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.datastore.BackupStore
import org.sjhstudio.diary.core.model.BackupEntry
import org.sjhstudio.diary.core.model.RestoreFailure
import org.sjhstudio.diary.core.model.RestorePreparation
import org.sjhstudio.diary.core.model.RestoreResult
import org.sjhstudio.diary.core.model.RestoreState
import org.sjhstudio.diary.core.platform.NetworkState

/** [RestoreRepository]의 구현. 글을 먼저 합치고 사진은 그 뒤에 받아서 받는 동안에도 일기를 볼 수 있다. */
class RestoreController(
    private val catalog: Lazy<RestoreCatalog>,
    private val planner: Lazy<RestorePlanner>,
    private val applier: Lazy<MergeApplier>,
    private val fetcher: Lazy<PhotoFetcher>,
    private val legacy: Lazy<LegacyBackupImporter>,
    private val db: Lazy<DiaryDatabase>,
    private val store: BackupStore,
    private val network: NetworkState,
    private val fileSystem: FileSystem,
    private val applySettings: suspend (ManifestSettings) -> Unit,
    private val scope: CoroutineScope
) : RestoreRepository {
    private val _state = MutableStateFlow<RestoreState>(RestoreState.Idle)
    override val state: StateFlow<RestoreState> = _state.asStateFlow()

    private val busy = Mutex()
    private var candidates = emptyList<RestoreCandidate>()
    private var prepared: PreparedRestore? = null
    private var preparedLegacy: LegacyPlan? = null

    override suspend fun backups(): List<BackupEntry>? = guarded {
        candidates = catalog.value.list()
        candidates.map { it.entry }
    }

    override suspend fun prepare(snapshotName: String): RestorePreparation {
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

    override fun merge() {
        val ready = prepared
        val old = preparedLegacy
        if (ready == null && old == null) return
        if (!busy.tryLock()) return
        scope.launch {
            try {
                _state.value = RestoreState.Merging
                _state.value = RestoreState.Done(if (ready != null) mergeBackup(ready) else importLegacy(requireNotNull(old)))
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _state.value = RestoreState.Failed(RestoreFailure.OTHER)
            } finally {
                busy.unlock()
            }
        }
    }

    private suspend fun importLegacy(plan: LegacyPlan): RestoreResult {
        val result = legacy.value.apply(plan)
        preparedLegacy = null
        return result
    }

    private suspend fun mergeBackup(ready: PreparedRestore): RestoreResult {
        // 미리보기 뒤에 일기를 고쳤을 수 있어 합치기 직전의 기기 상태로 다시 가른다.
        val plan = applier.value.plan(ready.plan.items.map { it.first })
        val counts = applier.value.apply(plan, SOURCE_DRIVE)
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
            val result = fetcher.value.fetch { done, all -> _state.value = RestoreState.FetchingPhotos(entries, done, all) }
            fetched = result.first
            broken = result.second
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

    override fun dismiss() {
        _state.value = RestoreState.Idle
    }

    override suspend fun resumePhotos() {
        val prefs = store.current()
        if (prefs.accountKey == null || !network.isConnected) return
        if (!network.isUnmetered && !prefs.photosOnCellular) return
        if (!busy.tryLock()) return
        try {
            if (db.value.backupDao().pendingFiles().isNotEmpty()) guarded { fetcher.value.fetch() }
        } finally {
            busy.unlock()
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
    }
}
