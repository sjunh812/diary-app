package org.sjhstudio.diary.core.backup

import kotlin.time.Instant
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import okio.FileSystem
import okio.Path
import org.sjhstudio.diary.core.drive.DriveApi
import org.sjhstudio.diary.core.drive.DriveFile
import org.sjhstudio.diary.core.drive.DriveNotFound
import org.sjhstudio.diary.core.drive.DriveQuery
import org.sjhstudio.diary.core.drive.listAll
import org.sjhstudio.diary.core.model.BackupEntry
import org.sjhstudio.diary.core.model.BackupEntryState
import org.sjhstudio.diary.core.model.DamageReason
import org.sjhstudio.diary.core.model.PLATFORM_LEGACY

/** 목록 파일을 한꺼번에 받는 수. 드라이브 요청 한도를 넘지 않게 묶는다. */
private const val MAX_DOWNLOADS = 4

/** 복원 목록의 한 줄과 받을 때 쓸 정보. */
data class RestoreCandidate(val entry: BackupEntry, val manifest: Manifest?, val snapshot: DriveFile?, val legacy: DriveFile? = null)

/**
 * 목록 파일을 모아 읽고 끝까지 된 백업인지 본다(contracts/backup-format.md).
 * 목록 파일이 없는 스냅샷은 목록에 없다.
 */
class RestoreCatalog(
    private val drive: DriveApi,
    private val fileSystem: FileSystem,
    private val workDir: Path,
    private val legacy: suspend () -> List<DriveFile> = { emptyList() }
) {
    /** 받은 목록 파일. 파일 번호가 같으면 내용도 같다. */
    private val seen = mutableMapOf<String, Manifest>()
    private val seenLock = Mutex()

    suspend fun list(): List<RestoreCandidate> {
        // 세 목록과 백업마다 받는 목록 파일을 차례로 읽으면 백업 수만큼 늦어진다.
        // 목록 파일은 한 번 올리면 바뀌지 않으니 한 번 받은 것은 다시 받지 않는다.
        val (manifests, snapshots, oldBackups) =
            coroutineScope {
                val manifests = async { drive.listAll(DriveQuery(props = mapOf(DriveLayout.KEY_KIND to DriveLayout.KIND_MANIFEST))) }
                val snapshots = async { drive.listAll(DriveQuery(props = mapOf(DriveLayout.KEY_KIND to DriveLayout.KIND_SNAPSHOT))) }
                val old = async { legacy() }
                Triple(manifests.await(), snapshots.await().associateBy { it.name }, old.await())
            }
        fileSystem.createDirectories(workDir)
        val slots = Semaphore(MAX_DOWNLOADS)
        val read =
            coroutineScope {
                manifests.map { file -> async { file to slots.withPermit { manifestOf(file) } } }.awaitAll()
            }
        val backups =
            read.map { (file, manifest) -> candidate(file, manifest, manifest?.let { snapshots[it.snapshot.fileName] }) }
                .sortedByDescending { it.entry.createdAt }
        return backups + oldBackups.map(::legacyCandidate)
    }

    private suspend fun manifestOf(file: DriveFile): Manifest? {
        val known = seenLock.withLock { seen[file.id] }
        return known ?: read(file)?.also { manifest -> seenLock.withLock { seen[file.id] = manifest } }
    }

    /** 목록을 읽는 사이에 없어진 파일 하나가 다른 백업까지 가리지 않게 깨진 파일로 본다. */
    private suspend fun read(file: DriveFile): Manifest? {
        val path = workDir / "manifest-${file.id}.tmp"
        try {
            drive.download(file.id, path)
        } catch (_: DriveNotFound) {
            return null
        }
        return ManifestCodec.decode(fileSystem.read(path) { readUtf8() }).also { fileSystem.delete(path, mustExist = false) }
    }

    private fun legacyCandidate(file: DriveFile) = RestoreCandidate(
        BackupEntry(
            snapshotName = LEGACY_PREFIX + file.id,
            deviceId = PLATFORM_LEGACY,
            deviceName = "",
            platform = PLATFORM_LEGACY,
            createdAt = Instant.fromEpochMilliseconds(file.createdAt),
            entryCount = 0,
            photoCount = 0,
            photosNotInBackup = 0,
            appVersion = "",
            state = BackupEntryState.Ready
        ),
        null,
        null,
        file
    )

    private fun candidate(file: DriveFile, manifest: Manifest?, snapshot: DriveFile?): RestoreCandidate {
        val state =
            when {
                ManifestCodec.check(manifest) == ManifestCheck.BROKEN -> BackupEntryState.Damaged(DamageReason.MANIFEST)

                ManifestCodec.check(manifest) == ManifestCheck.NEEDS_NEWER_APP -> BackupEntryState.NeedsNewerApp

                snapshot == null -> BackupEntryState.Damaged(DamageReason.SNAPSHOT_MISSING)

                snapshot.sha256 != null && snapshot.sha256 != manifest?.snapshot?.sha256 -> BackupEntryState.Damaged(
                    DamageReason.SNAPSHOT_MISMATCH
                )

                else -> BackupEntryState.Ready
            }
        val media = manifest?.media.orEmpty()
        return RestoreCandidate(
            BackupEntry(
                snapshotName = manifest?.snapshot?.fileName ?: file.name,
                deviceId = manifest?.device?.id.orEmpty(),
                deviceName = manifest?.device?.name.orEmpty(),
                platform = manifest?.app?.platform.orEmpty(),
                createdAt = Instant.fromEpochMilliseconds(manifest?.createdAt?.takeIf { it > 0 } ?: file.createdAt),
                entryCount = manifest?.counts?.entries ?: 0,
                photoCount = media.count { it.inBackup },
                photosNotInBackup = media.count { !it.inBackup },
                appVersion = manifest?.app?.versionName.orEmpty(),
                state = state
            ),
            manifest,
            snapshot
        )
    }
}

private const val LEGACY_PREFIX = "legacy:"
