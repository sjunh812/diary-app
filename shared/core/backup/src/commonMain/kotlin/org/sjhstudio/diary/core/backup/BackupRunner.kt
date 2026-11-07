package org.sjhstudio.diary.core.backup

import androidx.room3.withWriteTransaction
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import okio.FileSystem
import okio.IOException
import okio.Path
import org.sjhstudio.diary.core.database.BackupMediaEntity
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.database.LocalPhotoRow
import org.sjhstudio.diary.core.datastore.BackupPrefs
import org.sjhstudio.diary.core.datastore.BackupStore
import org.sjhstudio.diary.core.drive.ChunkResult
import org.sjhstudio.diary.core.drive.DriveApi
import org.sjhstudio.diary.core.drive.DriveException
import org.sjhstudio.diary.core.drive.DriveFile
import org.sjhstudio.diary.core.drive.DriveQuery
import org.sjhstudio.diary.core.drive.NewFile
import org.sjhstudio.diary.core.drive.SMALL_UPLOAD_LIMIT
import org.sjhstudio.diary.core.drive.listAll
import org.sjhstudio.diary.core.model.BackupFailure
import org.sjhstudio.diary.core.model.BackupProgress
import org.sjhstudio.diary.core.model.BackupStep
import org.sjhstudio.diary.core.platform.BackupNotifier
import org.sjhstudio.diary.core.platform.DeviceName
import org.sjhstudio.diary.core.platform.DriveAccount
import org.sjhstudio.diary.core.platform.DriveAuthorizer
import org.sjhstudio.diary.core.platform.NetworkState
import org.sjhstudio.diary.core.platform.TokenResult

/**
 * 앱 정보.
 * 목록 파일에 적는다.
 */
data class BackupAppInfo(val versionName: String, val versionCode: Int, val platform: String)

/** 한 번 돈 결과. */
sealed interface BackupOutcome {
    /** [photosUnusable]은 기기에서 깨지거나 없어져 올릴 수 없는 사진이다. [photosLeft]에 들지 않는다. */
    data class Done(val photosUploaded: Int, val photosLeft: Int, val photosUnusable: Int = 0) : BackupOutcome

    data class Failed(val kind: BackupFailure) : BackupOutcome

    /** 다른 백업이나 복원이 도는 중이거나 기록 이전이 끝나지 않았다. */
    data object Busy : BackupOutcome

    /** 자동 백업인데 마지막 성공 뒤로 바뀐 것이 없다. */
    data object Unchanged : BackupOutcome

    /**
     * 휴지통까지 비어 일기가 한 편도 없다.
     * 새 폰이나 실수로 비운 앱의 빈 백업이 앞선 백업을 가리지 않게 올리지 않는다.
     */
    data object Empty : BackupOutcome
}

/**
 * 오래 실패했을 때의 알림.
 * [text]는 까닭에 맞는 제목과 본문을 준다.
 * 까닭을 모르면 null이다.
 */
class BackupAlerts(
    private val notifier: BackupNotifier,
    private val folderText: suspend () -> Pair<String, String>,
    private val text: suspend (BackupFailure?) -> Pair<String, String>
) {
    suspend fun schedule(at: Instant, failure: BackupFailure?) {
        val (title, body) = text(failure)
        notifier.schedule(at, title, body)
    }

    /** 사용자가 지운 백업 폴더를 새로 만들었다고 한 번 알린다(`docs/backup.md` "지운 폴더"). */
    suspend fun folderRecreated() {
        val (title, body) = folderText()
        notifier.show(title, body)
    }
}

/**
 * 백업 상태 기계(data-model.md).
 * 사진 → 스냅샷 → 목록 파일 순서로 올리고, 파일마다 드라이브의 확인값을 견준 뒤에만 완료라고 한다(FR-007, FR-008).
 * 사진 이름이 내용 확인값이라 몇 번을 다시 올려도 결과가 같다.
 */
class BackupRunner(
    private val db: DiaryDatabase,
    private val drive: DriveApi,
    private val authorizer: DriveAuthorizer,
    private val store: BackupStore,
    private val network: NetworkState,
    private val deviceName: DeviceName,
    private val fileSystem: FileSystem,
    private val mediaDir: Path,
    private val workDir: Path,
    private val app: BackupAppInfo,
    private val readme: suspend () -> String,
    private val settings: suspend () -> ManifestSettings,
    private val clock: Clock = Clock.System,
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() },
    private val alerts: BackupAlerts? = null,
    private val gate: BackupGate = BackupGate(),
    private val dataReady: () -> Boolean = { true }
) {
    private val _progress = MutableStateFlow<BackupProgress?>(null)

    /**
     * 도는 동안의 단계와 숫자.
     * 홈과 설정이 같이 본다.
     */
    val progress: StateFlow<BackupProgress?> = _progress.asStateFlow()

    /** [cellularOnce]는 사용자가 이번만 모바일 데이터로 사진을 올리겠다고 고른 경우다. */
    suspend fun run(cellularOnce: Boolean = false): BackupOutcome = running(cellularOnce, automatic = false)

    private suspend fun running(cellularOnce: Boolean, automatic: Boolean): BackupOutcome = runCatchingFailure {
        // 기록 이전이 끝나기 전의 일기는 일부뿐이다.
        // 그 상태를 가장 새 백업으로 남기지 않는다.
        if (!dataReady() || !gate.tryEnter()) return@runCatchingFailure BackupOutcome.Busy
        try {
            if (db.backupDao().entryRows() == 0) return@runCatchingFailure BackupOutcome.Empty
            store.update { it.copy(lastAttemptAt = clock.now()) }
            runCatchingFailure { backUp(cellularOnce, automatic) }
        } finally {
            _progress.value = null
            clearOutgoing()
            gate.leave()
        }
    }

    /**
     * 자동 백업.
     * 일기, 사진, 설정이 마지막 성공 뒤로 그대로이고 그 백업이 드라이브에 있으면 올리지 않는다(contracts/backup-flow.md).
     * 올리지 못한 사진이 남아 있어도 이번에 더 올릴 수 없으면 같다.
     * 같은 내용 백업이 쌓이면 최근 5개 시점이 밀려난다.
     */
    suspend fun runIfChanged(): BackupOutcome = runCatchingFailure {
        val prefs = store.current()
        val nothingToUpload = prefs.photosNotBackedUp == 0 || !mayUploadPhotos(prefs.photosOnCellular)
        if (nothingToUpload && !repaired(prefs.unusablePhotos) && sameAsLastBackup(prefs, fingerprint())) {
            BackupOutcome.Unchanged
        } else {
            running(cellularOnce = false, automatic = true)
        }
    }

    private fun mayUploadPhotos(onCellular: Boolean) = network.isUnmetered || onCellular

    private suspend fun sameAsLastBackup(prefs: BackupPrefs, fingerprint: String): Boolean =
        prefs.lastSuccessFingerprint == fingerprint && stillOnDrive(prefs.lastSuccessSnapshot)

    /** 깨졌던 사진 파일이 돌아왔으면 올릴 수 있다. */
    private suspend fun repaired(unusable: Set<String>): Boolean {
        if (unusable.isEmpty()) return false
        return db.backupDao().localPhotos().any { row ->
            row.sha256 in unusable && row.fileName?.let { intact(mediaDir / it, row.sha256) } == true
        }
    }

    private fun intact(path: Path, sha256: String) = fileSystem.exists(path) && fileSystem.hashes(path).sha256 == sha256

    /**
     * 사용자가 드라이브에서 지웠으면 일기가 그대로여도 백업이 없는 것이다.
     * 드라이브를 볼 수 없으면 있다고 보고 다음 기회를 기다린다.
     */
    private suspend fun stillOnDrive(snapshot: String?): Boolean {
        if (snapshot == null) return false
        return try {
            drive.list(
                DriveQuery(name = "$snapshot.json", props = mapOf(DriveLayout.KEY_KIND to DriveLayout.KIND_MANIFEST))
            ).files.isNotEmpty()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            true
        }
    }

    /**
     * 임시 사본을 지우지 못해도 잠금은 풀어야 한다.
     * 남은 사본은 다음 백업이 덮어쓴다.
     */
    private fun clearOutgoing() {
        try {
            fileSystem.deleteRecursively(workDir / OUTGOING, mustExist = false)
        } catch (_: IOException) {
            return
        }
    }

    private suspend fun fingerprint(): String {
        val dao = db.backupDao()
        return "${dao.entriesFingerprint()}|${dao.mediaRows()}|${settings()}"
    }

    /**
     * 어떤 실패도 앱을 끄지 않게 결과로 바꾼다.
     * 멈추라는 신호만 그대로 내보낸다.
     * 저장 공간이 차면 SQLite 예외가, 공용 와이파이의 안내 페이지를 받으면 JSON 예외가 온다.
     * 예외 메시지에는 일기 내용이 섞일 수 있어 어디에도 적지 않고 종류만 남긴다.
     */
    private suspend fun runCatchingFailure(block: suspend () -> BackupOutcome): BackupOutcome = try {
        block()
    } catch (error: CancellationException) {
        throw error
    } catch (error: DriveException) {
        fail(error.kind)
    } catch (_: IOException) {
        fail(BackupFailure.LOCAL_IO)
    } catch (_: Exception) {
        fail(BackupFailure.UNKNOWN)
    }

    private suspend fun fail(kind: BackupFailure): BackupOutcome {
        try {
            val now = clock.now()
            store.update { it.copy(lastFailure = kind, failingSince = it.failingSince ?: now) }
            val prefs = store.current()
            val since = prefs.lastSuccessAt ?: prefs.failingSince ?: now
            // 까닭이 바뀌면 문구만 바꾼다.
            // 때가 지났으면 이미 한 번 알렸다.
            if (since + ALERT_AFTER > now) alerts?.schedule(since + ALERT_AFTER, kind)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            // 공간이 차면 실패 기록도 못 쓸 수 있다.
            // 그래도 실패는 실패로 돌려준다.
        }
        return BackupOutcome.Failed(kind)
    }

    private fun step(step: BackupStep, done: Int = 0, total: Int = 0, bytesDone: Long = 0, bytesTotal: Long = 0) {
        _progress.value = BackupProgress(step, done, total, bytesDone, bytesTotal)
    }

    private suspend fun backUp(cellularOnce: Boolean, automatic: Boolean): BackupOutcome {
        val fingerprint = fingerprint()
        step(BackupStep.Preparing)
        val account = signedIn()
        val deviceId = store.deviceId()
        val folders = openFolders(account)

        step(BackupStep.Snapshotting)
        val now = clock.now()
        val local = now.toLocalDateTime(timeZone())
        val base = snapshotBaseName(stamp(local), deviceName.current(), deviceId)
        val snapshot = SnapshotWriter(db, fileSystem).write(workDir / OUTGOING / "$base.db")

        val photos = sendPhotos(snapshot, folders, account, now, cellularOnce)
        // 사진을 더 올리지 못했고 글도 그대로면 같은 내용 백업이다.
        if (automatic && photos.uploaded.isEmpty() && sameAsLastBackup(store.current(), fingerprint)) {
            store.update {
                it.copy(
                    bytesShort = photos.bytesShort,
                    lastFailure = BackupFailure.NO_SPACE.takeIf { photos.bytesShort > 0 } ?: it.lastFailure,
                    photosNotBackedUp = photos.left,
                    unusablePhotos = photos.unusable
                )
            }
            return BackupOutcome.Unchanged
        }

        val snapshotFile = sendSnapshot(snapshot, folders, deviceId)
        sendManifest(base, snapshot, photos, folders, deviceId, now)

        step(BackupStep.Verifying)
        check(snapshotFile.id.isNotEmpty())
        db.rememberBases(snapshot.path)
        store.update {
            it.copy(
                lastSuccessAt = now,
                lastSuccessSnapshot = base,
                lastSuccessFingerprint = fingerprint,
                bytesShort = photos.bytesShort,
                failingSince = null,
                lastFailure = BackupFailure.NO_SPACE.takeIf { photos.bytesShort > 0 },
                failureNotifiedAt = null,
                photosNotBackedUp = photos.left,
                unusablePhotos = photos.unusable
            )
        }

        step(BackupStep.Pruning)
        // 성공은 이미 적었다.
        // 뒷정리가 실패해도 백업은 끝났고 정리는 다음 백업이 다시 한다.
        quietly {
            alerts?.schedule(now + ALERT_AFTER, null)
            prune(folders, deviceId, now)
        }
        return BackupOutcome.Done(photos.uploaded.size, photos.left, photos.unusable.size)
    }

    private suspend fun signedIn(): DriveAccount = when (val token = authorizer.accessToken()) {
        is TokenResult.Token -> token.account
        TokenResult.NeedsUser -> throw DriveException(BackupFailure.AUTH)
        is TokenResult.Failed -> throw DriveException(token.kind)
    }

    private suspend fun openFolders(account: DriveAccount): DriveFolders {
        val before = store.current()
        val folders = DriveLayout(drive, fileSystem).ensure(workDir / OUTGOING, readme())
        // 처음 백업이나 다른 계정이면 지운 폴더가 아니라 원래 없던 폴더다.
        val recreated = folders.created && before.rootFolderId != null && before.rootFolderAccount == account.key
        store.update {
            it.copy(rootFolderId = folders.root, rootFolderAccount = account.key, folderRecreated = it.folderRecreated || recreated)
        }
        if (recreated) quietly { alerts?.folderRecreated() }
        return folders
    }

    /** 사진을 올린 결과. */
    private class PhotoRound(
        val local: List<LocalPhotoRow>,
        val inBackup: Set<String>,
        val uploaded: Set<String>,
        val unusable: Set<String>,
        val left: Int,
        val bytesShort: Long
    )

    private suspend fun sendPhotos(
        snapshot: Snapshot,
        folders: DriveFolders,
        account: DriveAccount,
        now: Instant,
        cellularOnce: Boolean
    ): PhotoRound {
        val onDrive = drive.listAll(DriveQuery(parentId = folders.media, props = mapOf(DriveLayout.KEY_KIND to DriveLayout.KIND_PHOTO)))
        val driveHashes = onDrive.associateBy { it.name.substringBefore('.') }
        val dao = db.backupDao()
        // 드라이브가 원본이다.
        // 표에는 있는데 드라이브에 없어진 사진은 잊고 다시 올린다.
        dao.forget(account.key, dao.confirmed(account.key).map { it.sha256 }.filter { it !in driveHashes })
        dao.confirm(
            onDrive.map {
                BackupMediaEntity(account.key, it.name.substringBefore('.'), it.id, it.size ?: 0, now.toEpochMilliseconds())
            }
        )

        val local = dao.localPhotos()
        val missing = local.filter { it.sha256 !in driveHashes && it.fileName != null }
        val allowed = cellularOnce || mayUploadPhotos(store.current().photosOnCellular)
        val quotaLimit = drive.about().free
        val reserve = snapshot.hashes.byteSize + MANIFEST_RESERVE
        if (quotaLimit != null && quotaLimit < reserve) {
            store.update { it.copy(bytesShort = reserve - quotaLimit) }
            throw DriveException(BackupFailure.NO_SPACE)
        }
        val planned = if (allowed) fitting(missing, quotaLimit?.minus(reserve)) else emptyList()
        val unusable = HashSet<String>()
        val uploaded = uploadPhotos(planned, folders, account.key, unusable)
        // 이번에 살펴보지 않은 사진은 지난번 판단을 이어 간다.
        val tried = planned.mapTo(HashSet()) { it.sha256 }
        val hashes = local.mapTo(HashSet()) { it.sha256 }
        unusable += store.current().unusablePhotos.filter { it !in tried && it in hashes }
        val inBackup = driveHashes.keys + uploaded
        // 글은 올렸지만 공간 때문에 사진이 남았으면 숫자와 할 일을 보인다(FR-018, FR-019).
        val short = if (allowed) missing.filter { it.sha256 !in uploaded && it.sha256 !in unusable }.sumOf { it.byteSize ?: 0 } else 0
        return PhotoRound(local, inBackup, uploaded, unusable, local.count { it.sha256 !in inBackup && it.sha256 !in unusable }, short)
    }

    private suspend fun sendSnapshot(snapshot: Snapshot, folders: DriveFolders, deviceId: String): DriveFile {
        step(BackupStep.UploadingSnapshot)
        return upload(
            snapshot.path,
            NewFile(snapshot.path.name, folders.snapshots, "application/x-sqlite3", deviceProps(DriveLayout.KIND_SNAPSHOT, deviceId)),
            snapshot.hashes
        )
    }

    private suspend fun sendManifest(
        base: String,
        snapshot: Snapshot,
        photos: PhotoRound,
        folders: DriveFolders,
        deviceId: String,
        now: Instant
    ) {
        step(BackupStep.UploadingManifest)
        val pending = db.backupDao().pendingFiles()
        val manifest =
            manifestOf(now, timeZone(), app, deviceId, deviceName.current(), snapshot, photos.local, pending, photos.inBackup, settings())
        val manifestPath = workDir / OUTGOING / "$base.json"
        fileSystem.write(manifestPath) { writeUtf8(ManifestCodec.encode(manifest)) }
        upload(
            manifestPath,
            NewFile(
                manifestPath.name,
                folders.snapshots,
                "application/json",
                deviceProps(DriveLayout.KIND_MANIFEST, deviceId) + (DriveLayout.KEY_FORMAT to "${Manifest.FORMAT_VERSION}")
            ),
            fileSystem.hashes(manifestPath)
        )
    }

    /** 알림과 뒷정리가 실패해도 백업을 멈추거나 실패로 만들지 않는다. */
    private suspend fun quietly(block: suspend () -> Unit) {
        try {
            block()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            return
        }
    }

    /** 공간이 부족하면 앞(최근 일기)에서부터 들어가는 만큼만 고른다. */
    private fun fitting(photos: List<LocalPhotoRow>, free: Long?): List<LocalPhotoRow> {
        if (free == null) return photos
        var left: Long = free
        return photos.filter { photo ->
            val size = photo.byteSize ?: 0L
            (size <= left).also { if (it) left -= size }
        }
    }

    /**
     * 올린 사진의 확인값을 돌려준다.
     * 파일이 없거나 내용이 이름과 다르면 [unusable]에 적고 건너뛴다.
     * 깨진 내용을 이 이름으로 올리면 복원할 수 없는 사진을 백업됐다고 말하게 되고, 드라이브에 이미 있다고 보여 좋은 사본도 못 올라간다.
     */
    private suspend fun uploadPhotos(
        photos: List<LocalPhotoRow>,
        folders: DriveFolders,
        accountKey: String,
        unusable: MutableSet<String>
    ): Set<String> {
        val done = LinkedHashSet<String>()
        val bytesTotal = photos.sumOf { it.byteSize ?: 0L }
        var bytesDone = 0L
        step(BackupStep.UploadingPhotos, 0, photos.size, 0, bytesTotal)
        for (photo in photos) {
            val path = mediaDir / requireNotNull(photo.fileName)
            val hashes = if (fileSystem.exists(path)) fileSystem.hashes(path) else null
            if (hashes == null || hashes.sha256 != photo.sha256) {
                unusable += photo.sha256
                continue
            }
            try {
                val file =
                    upload(
                        path,
                        NewFile(path.name, folders.media, mimeOf(path.name), mapOf(DriveLayout.KEY_KIND to DriveLayout.KIND_PHOTO)),
                        hashes
                    )
                db.backupDao().confirm(
                    listOf(BackupMediaEntity(accountKey, photo.sha256, file.id, hashes.byteSize, clock.now().toEpochMilliseconds()))
                )
                done += photo.sha256
            } catch (error: DriveException) {
                // 공간이 찼으면 올린 만큼으로 스냅샷과 목록 파일을 올려 본다(contracts/backup-format.md).
                if (error.kind == BackupFailure.NO_SPACE) return done
                throw error
            }
            bytesDone += hashes.byteSize
            step(BackupStep.UploadingPhotos, done.size, photos.size, bytesDone, bytesTotal)
        }
        return done
    }

    /**
     * 올리고 드라이브의 확인값을 견준다.
     * 다르면 지우고 한 번 더 올린다.
     */
    private suspend fun upload(path: Path, meta: NewFile, hashes: FileHashes): DriveFile {
        val first = send(path, meta, hashes.byteSize)
        if (matches(first, hashes)) return first
        drive.trash(first.id)
        val second = send(path, meta, hashes.byteSize)
        if (matches(second, hashes)) return second
        drive.trash(second.id)
        throw DriveException(BackupFailure.DRIVE_ERROR)
    }

    private fun matches(file: DriveFile, hashes: FileHashes): Boolean = file.sha256?.let { it == hashes.sha256 } ?: (file.md5 == hashes.md5)

    private suspend fun send(path: Path, meta: NewFile, size: Long): DriveFile {
        if (size <= SMALL_UPLOAD_LIMIT) return drive.uploadSmall(meta, path)
        var session = drive.startResumable(meta, size)
        var from = 0L
        var restarts = 0
        var stalls = 0
        // 드라이브가 끝을 주지 않아도 백업이 잠금을 쥔 채 머물지 않게 횟수를 센다.
        while (true) {
            when (val result = drive.uploadChunk(session, from, path)) {
                is ChunkResult.Done -> return result.file

                is ChunkResult.Progress -> {
                    stalls = if (result.confirmedBytes > from) 0 else stalls + 1
                    if (stalls > MAX_STALLS) throw DriveException(BackupFailure.DRIVE_ERROR)
                    from = result.confirmedBytes
                }

                // 세션이 없어졌으면 처음부터 다시 올린다.
                ChunkResult.Expired -> {
                    if (++restarts > MAX_RESTARTS) throw DriveException(BackupFailure.DRIVE_ERROR)
                    session = drive.startResumable(meta, size)
                    from = 0
                }
            }
        }
    }

    private fun deviceProps(kind: String, deviceId: String) = mapOf(DriveLayout.KEY_KIND to kind, DriveLayout.KEY_DEVICE to deviceId)

    /**
     * 끝까지 된 내 백업만 정리한다.
     * 지우지 않고 드라이브 휴지통에 넣고, 목록 파일을 먼저 넣는다.
     */
    private suspend fun prune(folders: DriveFolders, deviceId: String, now: Instant) {
        val mine = drive.listAll(DriveQuery(parentId = folders.snapshots, props = mapOf(DriveLayout.KEY_DEVICE to deviceId)))
        val manifests = mine.filter { it.props[DriveLayout.KEY_KIND] == DriveLayout.KIND_MANIFEST }
        val snapshots = mine.filter { it.props[DriveLayout.KEY_KIND] == DriveLayout.KIND_SNAPSHOT }
        val backups =
            manifests.map {
                val created = Instant.fromEpochMilliseconds(it.createdAt)
                RetainedBackup(
                    it.name.removeSuffix(".json"),
                    it.createdAt,
                    created.toLocalDateTime(timeZone()).let { t ->
                        "${t.year}-${t.month.ordinal}"
                    }
                )
            }
        for (name in RetentionPolicy.toDelete(backups)) {
            manifests.firstOrNull { it.name == "$name.json" }?.let { drive.trash(it.id) }
            snapshots.firstOrNull { it.name == "$name.db" }?.let { drive.trash(it.id) }
        }
        // 목록 파일 없이 7일 넘게 남은 스냅샷은 끊긴 백업이다.
        val named = manifests.map { it.name.removeSuffix(".json") }.toSet()
        val cutoff = (now - 7.days).toEpochMilliseconds()
        snapshots.filter { it.name.removeSuffix(".db") !in named && it.createdAt < cutoff }.forEach { drive.trash(it.id) }
    }

    private fun stamp(t: LocalDateTime): String {
        fun two(value: Int) = value.toString().padStart(2, '0')
        return "${t.year}-${two(t.month.ordinal + 1)}-${two(t.day)}_${two(t.hour)}-${two(t.minute)}-${two(t.second)}"
    }

    private fun extOf(name: String?): String = name?.substringAfterLast('.', "jpg") ?: "jpg"

    private fun mimeOf(name: String): String = when (extOf(name).lowercase()) {
        "png" -> "image/png"
        "webp" -> "image/webp"
        "heic" -> "image/heic"
        else -> "image/jpeg"
    }

    private companion object {
        const val OUTGOING = "outgoing"
        const val MANIFEST_RESERVE = 1024L * 1024

        /** 이어 올리기 세션을 다시 여는 횟수. */
        const val MAX_RESTARTS = 3

        /** 조각을 보내도 받은 양이 늘지 않을 때 더 보내 보는 횟수. */
        const val MAX_STALLS = 3
    }
}

private val ALERT_AFTER = 7.days
