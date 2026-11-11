package org.sjhstudio.diary.core.backup

import androidx.room3.withWriteTransaction
import kotlin.concurrent.Volatile
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
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
import org.sjhstudio.diary.core.datastore.BackupPrefs
import org.sjhstudio.diary.core.datastore.BackupStore
import org.sjhstudio.diary.core.drive.DriveException
import org.sjhstudio.diary.core.model.BackupEntry
import org.sjhstudio.diary.core.model.BackupEntryState
import org.sjhstudio.diary.core.model.BackupFailure
import org.sjhstudio.diary.core.model.BundleEstimate
import org.sjhstudio.diary.core.model.PLATFORM_LEGACY
import org.sjhstudio.diary.core.model.PhotoProgress
import org.sjhstudio.diary.core.model.PhotoRun
import org.sjhstudio.diary.core.model.PhotoStop
import org.sjhstudio.diary.core.model.RESTORE_FROM_FILE
import org.sjhstudio.diary.core.model.RestoreFailure
import org.sjhstudio.diary.core.model.RestorePreparation
import org.sjhstudio.diary.core.model.RestoreResult
import org.sjhstudio.diary.core.model.RestoreState
import org.sjhstudio.diary.core.model.RollbackInfo
import org.sjhstudio.diary.core.model.RollbackRun
import org.sjhstudio.diary.core.model.WaitingPhotos
import org.sjhstudio.diary.core.model.orElseOnFailure
import org.sjhstudio.diary.core.platform.BackupScheduler
import org.sjhstudio.diary.core.platform.FreeSpace
import org.sjhstudio.diary.core.platform.NetworkState
import org.sjhstudio.diary.core.platform.NoBackupScheduler

/**
 * [RestoreRepository]의 구현
 * 글을 먼저 합치고 사진은 그 뒤에 받아 받는 동안에도 일기를 볼 수 있음
 */
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
    private val scheduler: BackupScheduler = NoBackupScheduler,
    private val freeSpace: FreeSpace = FreeSpace { Long.MAX_VALUE },
    private val clock: Clock = Clock.System
) : RestoreRepository {
    private val _photoProgress = MutableStateFlow<PhotoProgress?>(null)
    override val photoProgress: StateFlow<PhotoProgress?> = _photoProgress.asStateFlow()
    override var lastPhotoRun: PhotoRun? = null
        private set

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

    /** 받아 둔 사본을 만들고 지우는 일이 겹치지 않게 함 */
    private val preparing = Mutex()

    /** 사본을 읽고 일기 수를 세는 일이 메인 스레드에서 돌지 않게 함 */
    override val rollback: Flow<RollbackInfo?> = flow { emitAll(rollbackManager.value.info) }.flowOn(Dispatchers.IO)

    private val _rollbackRun = MutableStateFlow(RollbackRun.IDLE)
    override val rollbackRun: StateFlow<RollbackRun> = _rollbackRun.asStateFlow()

    /**
     * 화면을 떠나도 끝까지 돎
     * 중간에 멈추면 일기는 돌아왔는데 되돌리기가 남음
     * 사진을 받는 중에 누르면 조용히 버리지 않고 끝난 뒤에 이어서 돌림
     */
    override suspend fun rollBack(): Boolean = scope.async {
        val before = _rollbackRun.getAndUpdate { if (it.isGoing) it else RollbackRun.RUNNING }
        if (before.isGoing) return@async false
        if (!gate.tryEnter()) {
            _rollbackRun.value = RollbackRun.WAITING
            gate.enter()
            _rollbackRun.value = RollbackRun.RUNNING
        }
        try {
            val done = guarded { rollbackManager.value.rollBack() } ?: false
            _rollbackRun.value = if (done) RollbackRun.IDLE else RollbackRun.FAILED
            // 바꾸기 결과는 이제 사실이 아님
            if (done && _state.value is RestoreState.Done) _state.value = RestoreState.Idle
            done
        } finally {
            gate.leave()
        }
    }.await()

    private val RollbackRun.isGoing: Boolean
        get() = this == RollbackRun.WAITING || this == RollbackRun.RUNNING

    override suspend fun backups(): List<BackupEntry>? = guarded {
        candidates = catalog.value.list()
        // 어느 계정의 목록인지 적어 다른 계정을 연결했을 때 먼저 보이지 않게 함
        store.update { it.copy(listedAccount = it.accountKey) }
        candidates.map { it.entry }
    }

    override suspend fun savedBackups(): List<BackupEntry> = orElseOnFailure(emptyList()) {
        val prefs = store.current()
        if (prefs.accountKey == null || prefs.accountKey != prefs.listedAccount) emptyList() else catalog.value.saved()
    }

    override suspend fun bundleEstimate(): BundleEstimate = exporter.value.estimate()

    override val folderRecreated: Flow<Boolean> = store.prefs.map { it.folderRecreated }

    override fun dismissFolderRecreated() {
        scope.launch { store.update { it.copy(folderRecreated = false) } }
    }

    @Volatile
    internal var bundleBeingWritten = false
        private set

    /**
     * 쓰기 전에 표시를 남겨 둠
     * 앱이 꺼지면 지울 기회가 없어 다음에 멈췄음을 알 수 있음
     * 쓰다 실패하면 화면이 알리니 표시를 지움
     * 멈추라고 해서 끝났으면 사용자가 멈춘 것인지 화면이 정함
     */
    override suspend fun writeBundle(sink: BufferedSink, onProgress: (done: Int, total: Int) -> Unit) {
        store.update { it.copy(bundleWriting = true) }
        bundleBeingWritten = true
        var stopped = false
        try {
            exporter.value.write(sink, onProgress)
        } catch (error: CancellationException) {
            stopped = true
            throw error
        } finally {
            bundleBeingWritten = false
            if (!stopped) withContext(NonCancellable) { store.update { it.copy(bundleWriting = false) } }
        }
    }

    override suspend fun takeStoppedBundle(): Boolean {
        if (bundleBeingWritten || !store.current().bundleWriting) return false
        store.update { it.copy(bundleWriting = false) }
        return true
    }

    override fun forgetStoppedBundle() {
        scope.launch { store.update { it.copy(bundleWriting = false) } }
    }

    override suspend fun prepareFile(source: Source, byteSize: Long?): RestorePreparation = when {
        restoring -> busy()
        else -> lacking(byteSize) ?: readingFile(source)
    }

    /** 옮기기 전에 공간 확인 */
    private fun lacking(byteSize: Long?): RestorePreparation.Failed? {
        val needed = (byteSize ?: return null) + ROOM_MARGIN
        val free = freeSpace.bytes()
        return if (free < needed) RestorePreparation.Failed(RestoreFailure.NO_SPACE, needed - free) else null
    }

    /**
     * 옮기다 실패했을 때 공간이 없어서인지 확인
     * 공간이 찬 것을 파일이 깨졌다고 말하지 않음
     */
    private fun outOfRoom(): RestorePreparation.Failed? =
        if (freeSpace.bytes() < ROOM_MARGIN) RestorePreparation.Failed(RestoreFailure.NO_SPACE) else null

    /**
     * 복원은 고른 파일과 받은 사본을 끝까지 씀
     * 그 사이에 다른 백업을 받으면 같은 자리를 덮어써 남은 사진이 모두 깨진 사진이 됨
     */
    private fun busy() = RestorePreparation.Failed(RestoreFailure.BUSY)

    private suspend fun readingFile(source: Source): RestorePreparation = preparingCopy {
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
                outOfRoom() ?: RestorePreparation.Failed(RestoreFailure.MISMATCH)
            }
        if (result is RestorePreparation.Failed) clearCopies()
        preparedFile = result
        result
    }

    /**
     * 준비는 한 번에 하나만, 메인 스레드 밖에서 돎
     * 지난번 사본을 지우고 시작하고, 멈추면 쓰던 사본을 남기지 않음
     * 받는 파일은 일기 사본이라 쓰지 않게 되면 바로 지움
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

    /** 합치는 중이거나 사진을 꺼내는 중이면 그 일이 쓰는 파일은 두고 잊기만 함 */
    private fun clearCopies() {
        prepared = null
        preparedLegacy = null
        preparedFile = null
        if (!restoring) fileSystem.deleteRecursively(workDir / INCOMING, mustExist = false)
    }

    private val restoring: Boolean
        get() = _state.value.let { it == RestoreState.WaitingForBackup || it == RestoreState.Merging || it is RestoreState.FetchingPhotos }

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

    override suspend fun prepare(snapshotName: String): RestorePreparation = when {
        snapshotName == RESTORE_FROM_FILE -> preparedFile ?: RestorePreparation.Failed(RestoreFailure.OTHER)

        restoring -> busy()

        else -> preparingCopy {
            val result = prepareFromDrive(snapshotName)
            if (result is RestorePreparation.Failed) clearCopies()
            result
        }
    }

    private suspend fun prepareFromDrive(snapshotName: String): RestorePreparation {
        // 앱이 꺼졌다 돌아오면 목록이 비어 있음
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
        } catch (_: RestoreNeedsUpdate) {
            RestorePreparation.Failed(RestoreFailure.NEEDS_UPDATE)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            outOfRoom() ?: RestorePreparation.Failed(RestoreFailure.OTHER)
        }
    }

    private var lastWork: (suspend (PreparedRestore?) -> RestoreResult)? = null

    override fun merge() = run { ready ->
        if (ready != null) mergeBackup(ready) else importLegacy(requireNotNull(preparedLegacy))
    }

    override fun replace() = run { ready -> replaceWith(requireNotNull(ready)) }

    /**
     * 실패한 복원을 사용자가 고른 그대로 다시 함
     * 바꾸기가 실패했는데 합치기로 넘어가지 않음
     */
    override fun retry() {
        lastWork?.let { run(it) }
    }

    private fun run(work: suspend (PreparedRestore?) -> RestoreResult) {
        lastWork = work
        val ready = prepared
        if (ready == null && preparedLegacy == null) return
        if (restoring) return
        _state.value = RestoreState.Merging
        scope.launch {
            // 도는 백업이 있으면 끝날 때까지 기다림
            // 복원을 조용히 버리지 않고, 멈춘 것처럼 보이지 않게 기다린다고 알림
            if (!gate.tryEnter()) {
                _state.value = RestoreState.WaitingForBackup
                gate.enter()
                _state.value = RestoreState.Merging
            }
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
        // 미리보기 뒤에 일기를 고쳤을 수 있어 합치기 직전의 기기 상태로 다시 가름
        // 가른 뒤 쓰기 전에 저장한 일기를 덮어쓰지 않게 가르기부터 한 트랜잭션에 넣음
        val counts =
            db.value.withWriteTransaction {
                applier.value.apply(applier.value.plan(ready.plan.items.map { it.first }, ready.fromOtherDevice), SOURCE_DRIVE)
            }
        return settle(counts, ready)
    }

    private suspend fun settle(counts: MergeCounts, ready: PreparedRestore): RestoreResult {
        guarded { applier.value.revive(ready.manifest.media.filter { it.inBackup }.mapTo(HashSet()) { it.sha256 }) }
        // 일기는 이미 합침
        // 설정을 쓰지 못해도 복원이 실패했다고 하지 않고 사진을 마저 받음
        guarded { applySettings(ready.manifest.settings) }
        // 파일에서 복원한 것은 드라이브 백업이 있다는 뜻이 아님
        if (ready.bundle == null) {
            val at = Instant.fromEpochMilliseconds(ready.manifest.createdAt)
            guarded { store.update { it.copy(restoredBackupAt = at) } }
        }
        // 받아 둔 사본 지우기도 곁일. 실패라고 하면 다시 시도로 같은 백업을 또 합침
        guarded { fileSystem.delete(ready.snapshot, mustExist = false) }
        prepared = null
        return fetchPhotos(counts, ready)
    }

    private suspend fun fetchPhotos(counts: MergeCounts, ready: PreparedRestore): RestoreResult {
        val dao = db.value.backupDao()
        // 일기는 이미 합침
        // 여기서 세기에 실패해도 복원 실패로 하지 않음. 실패라고 하면 다시 시도가 할 일이 없어 멈춰 보임
        val entries = orElseOnFailure(counts.added + counts.updated + counts.keptBoth) { dao.visibleEntryCount() }
        val total = orElseOnFailure(0) { dao.pendingFiles().size }
        _state.value = RestoreState.FetchingPhotos(entries, 0, total)
        val tally = PhotoTally()
        // 글은 이미 합침
        // 남은 사진은 다음에 앱을 열 때 이어 받음
        val stop =
            receiving(if (ready.bundle == null) PhotoStop.NETWORK else PhotoStop.INTERRUPTED) {
                val zip = ready.bundle?.let { holdBundle(it) }
                watchingPhotos(total, tally) { watch ->
                    val progress = { done: Int, all: Int ->
                        watch(done, all)
                        _state.value = RestoreState.FetchingPhotos(entries, done, all)
                    }
                    if (zip != null) fetcher.value.fetchFrom(zip, tally, progress) else fetcher.value.fetch(tally, progress)
                }
                // 다 꺼냈을 때만 지움
                // 멈췄으면 파일을 두어 앱을 다시 열 때 이음
                if (zip != null) dropBundle()
            }
        guarded { rememberStop(stop, tally.notThere) }
        val left = orElseOnFailure((total - tally.fetched - tally.broken).coerceAtLeast(0)) { dao.pendingFiles().size }
        // 드라이브에 없는 사진은 앱을 다시 열어도 오지 않아 남은 사진으로 세지 않음
        val notInBackup = if (ready.bundle == null) tally.notThere else ready.manifest.media.count { !it.inBackup }
        val onOriginal = minOf(left, notInBackup)
        return RestoreResult(
            entriesAdded = counts.added,
            entriesUpdated = counts.updated,
            entriesKeptBoth = counts.keptBoth,
            photosRestored = tally.fetched,
            photosMissing = tally.broken,
            photosPending = left - onOriginal,
            fromFile = ready.bundle != null,
            photosOnOriginal = onOriginal,
            lockWasOn = ready.manifest.settings.lockEnabled,
            photosStop = stop
        )
    }

    /**
     * 사진을 받다 멈춘 까닭
     * 끝까지 돌았으면 null, 달리 까닭을 모르면 [cut]
     */
    private suspend fun receiving(cut: PhotoStop, work: suspend () -> Unit): PhotoStop? = try {
        work()
        null
    } catch (error: CancellationException) {
        throw error
    } catch (_: NoRoomForPhotos) {
        PhotoStop.NO_SPACE
    } catch (error: DriveException) {
        if (error.kind == BackupFailure.AUTH) PhotoStop.ACCOUNT else cut
    } catch (_: Exception) {
        cut
    }

    /** 멈춘 까닭을 남겨 앱을 다시 열어도 화면이 사실을 말하게 함 */
    private suspend fun rememberStop(stop: PhotoStop?, notThere: Int) {
        store.update { it.copy(photoStop = stop, photosUnreachable = notThere) }
        if (stop == PhotoStop.NETWORK) scheduler.waitForPhotoNetwork(wifiOnly = !store.current().photosOnCellular)
    }

    /**
     * 고른 파일을 사진을 다 꺼낼 때까지 둘 자리로 옮김
     * 새로 고른 파일과 자리를 나누고, 앱이 꺼져도 남은 사진을 이어서 꺼낼 수 있게 함
     */
    private suspend fun holdBundle(zip: Path): Path {
        val held = heldBundle
        held.parent?.let(fileSystem::createDirectories)
        fileSystem.atomicMove(zip, held)
        store.update { it.copy(unpackUntil = clock.now() + KEEP_BUNDLE) }
        return held
    }

    private suspend fun dropBundle() {
        fileSystem.delete(heldBundle, mustExist = false)
        store.update { it.copy(unpackUntil = null) }
    }

    /**
     * 앱이 꺼지거나 공간이 차서 다 꺼내지 못한 파일에서 이음
     * 일기 사본이라 오래 두지 않음
     * 지난 뒤에는 그 파일을 다시 고르면 남은 사진이 돌아옴
     */
    private suspend fun unpackLeftovers() {
        if (!fileSystem.exists(heldBundle)) return
        val until = store.current().unpackUntil
        val total = db.value.backupDao().pendingFiles().size
        if (total == 0 || until == null || clock.now() >= until) {
            dropBundle()
            store.update { it.copy(photoStop = null) }
            return
        }
        val tally = PhotoTally()
        val stop =
            receiving(PhotoStop.INTERRUPTED) {
                watchingPhotos(total, tally) { watch -> fetcher.value.fetchFrom(heldBundle, tally, watch) }
                dropBundle()
            }
        rememberStop(stop, tally.notThere)
    }

    /**
     * 드라이브에서 남은 사진을 받음
     * 네트워크를 기다려야 하면 true
     */
    private suspend fun fromDrive(cellularOnce: Boolean): Boolean {
        val prefs = store.current()
        val wifiOnly = !cellularOnce && !prefs.photosOnCellular
        val nothingLeft = db.value.backupDao().pendingFiles().isEmpty()
        return when {
            nothingLeft -> {
                if (prefs.photoStop != null || prefs.photosUnreachable != 0) rememberStop(null, 0)
                false
            }

            prefs.accountKey == null -> false

            !network.isConnected -> waitFor(PhotoStop.NETWORK, wifiOnly)

            wifiOnly && !network.isUnmetered -> waitFor(PhotoStop.WIFI, wifiOnly)

            else -> receiveFromDrive()
        }
    }

    private suspend fun waitFor(stop: PhotoStop, wifiOnly: Boolean): Boolean {
        store.update { it.copy(photoStop = stop) }
        scheduler.waitForPhotoNetwork(wifiOnly)
        return true
    }

    private suspend fun receiveFromDrive(): Boolean {
        val tally = PhotoTally()
        val stop =
            receiving(PhotoStop.NETWORK) {
                val found = fetcher.value.look()
                if (found.reachable > 0) {
                    watchingPhotos(found.reachable, tally) { watch -> fetcher.value.fetch(found, tally, watch) }
                } else {
                    tally.notThere = found.notThere
                }
            }
        rememberStop(stop, tally.notThere)
        return stop == PhotoStop.NETWORK
    }

    /** 앱을 나가도 사진을 계속 받게 시스템에 알리고 진행을 내보냄 */
    private suspend fun <T> watchingPhotos(total: Int, tally: PhotoTally, work: suspend (watch: (done: Int, all: Int) -> Unit) -> T): T {
        lastPhotoRun = null
        _photoProgress.value = PhotoProgress(0, total)
        scheduler.keepRestoreRunning()
        return try {
            work { done, all -> _photoProgress.value = PhotoProgress(done, all) }
        } finally {
            // 진행 값이 사라지는 것을 보고 끝을 읽으니 결과를 먼저 적음
            lastPhotoRun = withContext(NonCancellable) { runOf(tally) }
            _photoProgress.value = null
        }
    }

    /** 진행 숫자에는 받지 못한 장도 들어 있어 실제로 받은 수와 남은 수로 적음 */
    private suspend fun runOf(tally: PhotoTally): PhotoRun {
        val left = guarded { db.value.backupDao().pendingFiles().size } ?: 0
        return PhotoRun(tally.fetched, (left - tally.notThere).coerceAtLeast(0), tally.broken + tally.notThere)
    }

    /**
     * 미리보기나 결과를 닫을 때 호출
     * 복원이 도는 중이면 뒤로 간 것뿐이라 그대로 둠
     */
    override fun dismiss() {
        if (restoring) return
        _state.value = RestoreState.Idle
        scope.launch { preparing.withLock { withContext(Dispatchers.IO) { if (!restoring) clearCopies() } } }
    }

    override suspend fun resumePhotos(): Boolean = resuming(cellularOnce = false)

    override fun fetchPhotosNow() {
        scope.launch { resuming(cellularOnce = true) }
    }

    /** 백업이나 복원이 돌고 있으면 그 일이 끝난 뒤에 다시 불러야 함 */
    private suspend fun resuming(cellularOnce: Boolean): Boolean {
        guarded { rollbackManager.value.expire() }
        guarded { clearAbandonedCopies() }
        if (!gate.tryEnter()) return true
        return try {
            guarded { unpackLeftovers() }
            guarded { fromDrive(cellularOnce) } ?: false
        } finally {
            gate.leave()
        }
    }

    override val waitingPhotos: Flow<WaitingPhotos?> =
        flow {
            val pending = db.value.backupDao().pendingFileCount()
            emitAll(combine(pending, store.prefs, _photoProgress, ::waitingOf))
        }.flowOn(Dispatchers.IO)

    private fun waitingOf(pending: Int, prefs: BackupPrefs, running: PhotoProgress?): WaitingPhotos? {
        val left = if (running != null) running.total - running.done else pending - prefs.photosUnreachable
        // 드라이브를 쓰지 않으면 남은 사진은 파일에서만 옴
        val stopped = prefs.photoStop ?: if (prefs.accountKey == null) PhotoStop.FILE else PhotoStop.INTERRUPTED
        return WaitingPhotos(left, stopped.takeIf { running == null }).takeIf { left > 0 }
    }

    /**
     * 앱이 꺼지면 받아 둔 사본을 지울 기회가 없음
     * 쓰는 곳이 없을 때만 지움
     */
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

    private val heldBundle: Path
        get() = workDir / UNPACKING / BUNDLE

    private companion object {
        const val SOURCE_DRIVE = "drive"
        const val INCOMING = "incoming"
        const val PICKED = "picked"
        const val UNPACKING = "unpacking"
        const val BUNDLE = "bundle.zip"
        val KEEP_BUNDLE = 7.days
        const val HEAD = 4L

        /** 파일을 옮기고 일기 사본을 꺼낼 때 남겨 둘 여유 */
        const val ROOM_MARGIN = 10L * 1024 * 1024
        val ZIP_HEAD = "504b0304".decodeHex()
        val SQLITE_HEAD = "53514c69".decodeHex()
    }
}
