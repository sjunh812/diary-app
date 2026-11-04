package org.sjhstudio.diary.core.backup

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import okio.FileSystem
import okio.IOException
import okio.Path
import org.sjhstudio.diary.core.database.BackupMediaEntity
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.database.LocalPhotoRow
import org.sjhstudio.diary.core.database.SyncBaseEntity
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
import org.sjhstudio.diary.core.platform.DriveAuthorizer
import org.sjhstudio.diary.core.platform.NetworkState
import org.sjhstudio.diary.core.platform.TokenResult

/** 앱 정보. 목록 파일에 적는다. */
data class BackupAppInfo(val versionName: String, val versionCode: Int, val platform: String)

/** 한 번 돈 결과. */
sealed interface BackupOutcome {
    data class Done(val photosUploaded: Int, val photosLeft: Int) : BackupOutcome

    data class Failed(val kind: BackupFailure) : BackupOutcome

    /** 다른 백업이 도는 중이다. */
    data object Busy : BackupOutcome
}

/** 오래 실패했을 때의 알림. [text]는 까닭에 맞는 제목과 본문을 준다. 까닭을 모르면 null이다. */
class BackupAlerts(private val notifier: BackupNotifier, private val text: suspend (BackupFailure?) -> Pair<String, String>) {
    suspend fun schedule(at: Instant, failure: BackupFailure?) {
        val (title, body) = text(failure)
        notifier.schedule(at, title, body)
    }
}

/**
 * 백업 상태 기계(data-model.md). 사진 → 스냅샷 → 목록 파일 순서로 올리고, 파일마다 드라이브의 확인값을 견준 뒤에만 완료라고 한다(FR-007, FR-008).
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
    private val alerts: BackupAlerts? = null
) {
    private val lock = Mutex()
    private val _progress = MutableStateFlow<BackupProgress?>(null)

    /** 도는 동안의 단계와 숫자. 홈과 설정이 같이 본다. */
    val progress: StateFlow<BackupProgress?> = _progress.asStateFlow()

    /** [cellularOnce]는 사용자가 이번만 모바일 데이터로 사진을 올리겠다고 고른 경우다. */
    suspend fun run(cellularOnce: Boolean = false): BackupOutcome {
        if (!lock.tryLock()) return BackupOutcome.Busy
        try {
            store.update { it.copy(lastAttemptAt = clock.now()) }
            return runCatchingFailure { backUp(cellularOnce) }
        } finally {
            _progress.value = null
            fileSystem.deleteRecursively(workDir / OUTGOING, mustExist = false)
            lock.unlock()
        }
    }

    private suspend fun runCatchingFailure(block: suspend () -> BackupOutcome.Done): BackupOutcome = try {
        block()
    } catch (error: CancellationException) {
        throw error
    } catch (error: DriveException) {
        fail(error.kind)
    } catch (_: IOException) {
        fail(BackupFailure.LOCAL_IO)
    }

    private suspend fun fail(kind: BackupFailure): BackupOutcome {
        val now = clock.now()
        store.update { it.copy(lastFailure = kind, failingSince = it.failingSince ?: now) }
        val prefs = store.current()
        val since = prefs.lastSuccessAt ?: prefs.failingSince ?: now
        // 까닭이 바뀌면 문구만 바꾼다. 때가 지났으면 이미 한 번 알렸다.
        if (since + ALERT_AFTER > now) alerts?.schedule(since + ALERT_AFTER, kind)
        return BackupOutcome.Failed(kind)
    }

    private fun step(step: BackupStep, done: Int = 0, total: Int = 0, bytesDone: Long = 0, bytesTotal: Long = 0) {
        _progress.value = BackupProgress(step, done, total, bytesDone, bytesTotal)
    }

    private suspend fun backUp(cellularOnce: Boolean): BackupOutcome.Done {
        step(BackupStep.Preparing)
        val account =
            when (val token = authorizer.accessToken()) {
                is TokenResult.Token -> token.account
                TokenResult.NeedsUser -> throw DriveException(BackupFailure.AUTH)
                is TokenResult.Failed -> throw DriveException(token.kind)
            }
        val deviceId = store.deviceId()
        val folders = DriveLayout(drive, fileSystem).ensure(workDir / OUTGOING, readme())
        store.update { it.copy(rootFolderId = folders.root) }

        step(BackupStep.Snapshotting)
        val now = clock.now()
        val local = now.toLocalDateTime(timeZone())
        val base = snapshotBaseName(stamp(local), deviceName.current(), deviceId)
        val snapshot = SnapshotWriter(db, fileSystem).write(workDir / OUTGOING / "$base.db")

        val onDrive = drive.listAll(DriveQuery(parentId = folders.media, props = mapOf(DriveLayout.KEY_KIND to DriveLayout.KIND_PHOTO)))
        val driveHashes = onDrive.associateBy { it.name.substringBefore('.') }
        val dao = db.backupDao()
        // 드라이브가 원본이다. 표에는 있는데 드라이브에 없어진 사진은 잊고 다시 올린다.
        dao.forget(account.key, dao.confirmed(account.key).map { it.sha256 }.filter { it !in driveHashes })
        dao.confirm(
            onDrive.map {
                BackupMediaEntity(account.key, it.name.substringBefore('.'), it.id, it.size ?: 0, now.toEpochMilliseconds())
            }
        )

        val local2 = dao.localPhotos()
        val missing = local2.filter { it.sha256 !in driveHashes && it.fileName != null }
        val allowed = cellularOnce || network.isUnmetered || store.current().photosOnCellular
        val quotaLimit = drive.about().free
        val reserve = snapshot.hashes.byteSize + MANIFEST_RESERVE
        if (quotaLimit != null && quotaLimit < reserve) throw DriveException(BackupFailure.NO_SPACE)
        val planned = if (allowed) fitting(missing, quotaLimit?.minus(reserve)) else emptyList()
        val uploaded = uploadPhotos(planned, folders, account.key)

        step(BackupStep.UploadingSnapshot)
        val snapshotFile =
            upload(
                snapshot.path,
                NewFile(snapshot.path.name, folders.snapshots, "application/x-sqlite3", deviceProps(DriveLayout.KIND_SNAPSHOT, deviceId)),
                snapshot.hashes
            )

        step(BackupStep.UploadingManifest)
        val inBackup = driveHashes.keys + uploaded
        val manifest =
            manifestOf(now, timeZone(), app, deviceId, deviceName.current(), snapshot, local2, dao.pendingPhotos(), inBackup, settings())
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

        step(BackupStep.Verifying)
        check(snapshotFile.id.isNotEmpty())
        dao.setBase(syncBases(snapshot.path))
        val left = local2.count { it.sha256 !in inBackup }
        store.update {
            it.copy(
                lastSuccessAt = now,
                lastSuccessSnapshot = base,
                failingSince = null,
                lastFailure = null,
                failureNotifiedAt = null,
                photosNotBackedUp = left
            )
        }

        alerts?.schedule(now + ALERT_AFTER, null)

        step(BackupStep.Pruning)
        prune(folders, deviceId, now)
        return BackupOutcome.Done(uploaded.size, left)
    }

    /** 공간이 모자라면 앞(최근 일기)에서부터 들어가는 만큼만 고른다. */
    private fun fitting(photos: List<LocalPhotoRow>, free: Long?): List<LocalPhotoRow> {
        if (free == null) return photos
        var left: Long = free
        return photos.filter { photo ->
            val size = photo.byteSize ?: 0L
            (size <= left).also { if (it) left -= size }
        }
    }

    private suspend fun uploadPhotos(photos: List<LocalPhotoRow>, folders: DriveFolders, accountKey: String): Set<String> {
        val done = LinkedHashSet<String>()
        val bytesTotal = photos.sumOf { it.byteSize ?: 0L }
        var bytesDone = 0L
        step(BackupStep.UploadingPhotos, 0, photos.size, 0, bytesTotal)
        for (photo in photos) {
            val path = mediaDir / requireNotNull(photo.fileName)
            if (!fileSystem.exists(path)) continue
            val hashes = fileSystem.hashes(path)
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

    /** 올리고 드라이브의 확인값을 견준다. 다르면 지우고 한 번 더 올린다. */
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
        while (true) {
            when (val result = drive.uploadChunk(session, from, path)) {
                is ChunkResult.Done -> return result.file

                is ChunkResult.Progress -> from = result.confirmedBytes

                // 세션이 없어졌으면 처음부터 다시 올린다.
                ChunkResult.Expired -> {
                    session = drive.startResumable(meta, size)
                    from = 0
                }
            }
        }
    }

    private fun deviceProps(kind: String, deviceId: String) = mapOf(DriveLayout.KEY_KIND to kind, DriveLayout.KEY_DEVICE to deviceId)

    private fun syncBases(snapshot: Path): List<SyncBaseEntity> {
        val connection = BundledSQLiteDriver().open(snapshot.toString())
        try {
            return connection.prepare("SELECT id, updated_at FROM entry").use { statement ->
                buildList {
                    while (statement.step()) add(SyncBaseEntity(statement.getText(0), statement.getLong(1)))
                }
            }
        } finally {
            connection.close()
        }
    }

    /** 끝까지 된 내 백업만 정리한다. 지우지 않고 드라이브 휴지통에 넣고, 목록 파일을 먼저 넣는다. */
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
    }
}

private val ALERT_AFTER = 7.days
