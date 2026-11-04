package org.sjhstudio.diary.core.backup

import kotlin.time.Instant
import okio.FileSystem
import okio.Path
import org.sjhstudio.diary.core.drive.DriveApi
import org.sjhstudio.diary.core.drive.DriveFile
import org.sjhstudio.diary.core.drive.DriveQuery
import org.sjhstudio.diary.core.drive.listAll
import org.sjhstudio.diary.core.model.BackupEntry
import org.sjhstudio.diary.core.model.BackupEntryState
import org.sjhstudio.diary.core.model.DamageReason

/** 복원 목록의 한 줄과 받을 때 쓸 정보. */
data class RestoreCandidate(val entry: BackupEntry, val manifest: Manifest?, val snapshot: DriveFile?)

/** 목록 파일을 모아 읽고 끝까지 된 백업인지 본다(contracts/backup-format.md). 목록 파일이 없는 스냅샷은 목록에 없다. */
class RestoreCatalog(private val drive: DriveApi, private val fileSystem: FileSystem, private val workDir: Path) {
    suspend fun list(): List<RestoreCandidate> {
        val manifests = drive.listAll(DriveQuery(props = mapOf(DriveLayout.KEY_KIND to DriveLayout.KIND_MANIFEST)))
        val snapshots = drive.listAll(DriveQuery(props = mapOf(DriveLayout.KEY_KIND to DriveLayout.KIND_SNAPSHOT))).associateBy { it.name }
        fileSystem.createDirectories(workDir)
        return manifests.map { file ->
            val path = workDir / "manifest.tmp"
            drive.download(file.id, path)
            val manifest = ManifestCodec.decode(fileSystem.read(path) { readUtf8() })
            fileSystem.delete(path, mustExist = false)
            candidate(file, manifest, manifest?.let { snapshots[it.snapshot.fileName] })
        }.sortedByDescending { it.entry.createdAt }
    }

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
