package org.sjhstudio.diary.core.backup

import okio.FileSystem
import okio.Path
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.drive.DriveApi
import org.sjhstudio.diary.core.drive.DriveFile
import org.sjhstudio.diary.core.drive.DriveQuery
import org.sjhstudio.diary.core.drive.listAll
import org.sjhstudio.diary.core.model.RestorePreview
import org.sjhstudio.diary.core.platform.FreeSpace

/** 받은 스냅샷이 목록 파일과 맞지 않는다. 기기의 기록은 그대로다(FR-021). */
class RestoreMismatch : Exception("backup does not match its manifest")

/** 받아서 확인까지 끝난 백업. 사용자가 확인하면 이 계획대로 합친다. */
data class PreparedRestore(val manifest: Manifest, val plan: MergePlan, val preview: RestorePreview, val snapshot: Path)

/** 스냅샷 받기 → 확인값·일기 수 확인 → 쓰기 없이 합치기를 돌려 숫자를 낸다(contracts/merge-rules.md). */
class RestorePlanner(
    private val drive: DriveApi,
    private val applier: MergeApplier,
    private val freeSpace: FreeSpace,
    private val fileSystem: FileSystem,
    private val workDir: Path
) {
    suspend fun prepare(candidate: RestoreCandidate): PreparedRestore {
        val manifest = requireNotNull(candidate.manifest)
        val file = candidate.snapshot ?: throw RestoreMismatch()
        val incoming = workDir / INCOMING
        fileSystem.createDirectories(incoming)
        val path = incoming / manifest.snapshot.fileName
        drive.download(file.id, path)
        if (fileSystem.hashes(path).sha256 != manifest.snapshot.sha256) {
            fileSystem.delete(path, mustExist = false)
            throw RestoreMismatch()
        }
        val entries = SnapshotReader.read(path)
        if (entries.count { it.entry.deletedAt == null } != manifest.counts.entries) {
            fileSystem.delete(path, mustExist = false)
            throw RestoreMismatch()
        }
        val plan = applier.plan(entries)
        val needed = neededPhotos(plan, manifest)
        val preview =
            RestorePreview(
                entriesAdded = plan.count(MergeAction.ADD),
                entriesUpdated = plan.count(MergeAction.REPLACE),
                entriesKeptBoth = plan.count(MergeAction.KEEP_BOTH),
                entriesUnchanged = plan.count(MergeAction.KEEP),
                photosToDownload = needed.size,
                bytesToDownload = needed.sumOf { it.byteSize },
                photosNotInBackup = manifest.media.count { !it.inBackup },
                settingsChange = true,
                deviceBytesFree = freeSpace.bytes()
            )
        return PreparedRestore(manifest, plan, preview, path)
    }

    private fun neededPhotos(plan: MergePlan, manifest: Manifest): List<ManifestMedia> {
        val local = plan.local.values.flatMap { it.media }
        val here = local.filter { it.status == "normal" }.mapNotNull { it.sha256 }.toSet()
        // 일기가 그대로여도 전에 못 받은 사진은 이번 백업에서 채운다.
        val waiting = local.filter { it.status == "pending" }.mapNotNull { it.sha256 }
        val incoming = plan.items.filter { it.second != MergeAction.KEEP }.flatMap { it.first.media }.mapNotNull { it.sha256 }
        return manifest.media.filter { it.inBackup && it.sha256 !in here && (it.sha256 in incoming || it.sha256 in waiting) }
    }

    private companion object {
        const val INCOMING = "incoming"
    }
}

/** 합친 뒤 대기 중인 사진을 받는다. 끊겨도 다음에 대기 줄만 다시 받는다(FR-024). */
class PhotoFetcher(
    private val db: DiaryDatabase,
    private val drive: DriveApi,
    private val fileSystem: FileSystem,
    private val mediaDir: Path,
    private val workDir: Path
) {
    /** 받은 수와 깨져서 쓸 수 없던 수. */
    suspend fun fetch(onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }): Pair<Int, Int> {
        val dao = db.backupDao()
        val waiting = dao.pendingFiles()
        if (waiting.isEmpty()) return 0 to 0
        val onDrive: Map<String, DriveFile> =
            drive.listAll(DriveQuery(props = mapOf(DriveLayout.KEY_KIND to DriveLayout.KIND_PHOTO))).associateBy {
                it.name.substringBefore('.')
            }
        fileSystem.createDirectories(workDir)
        fileSystem.createDirectories(mediaDir)
        var fetched = 0
        var broken = 0
        waiting.forEachIndexed { index, photo ->
            val file = onDrive[photo.sha256]
            val name = photo.fileName
            // 백업에 없는 사진은 대기로 두어 "원래 기기에 있는 사진"으로 보인다.
            if (file != null && name != null) {
                val partial = workDir / "$name.part"
                drive.download(file.id, partial)
                if (fileSystem.hashes(partial).sha256 == photo.sha256) {
                    fileSystem.atomicMove(partial, mediaDir / name)
                    dao.settlePending(photo.sha256, "normal")
                    fetched++
                } else {
                    fileSystem.delete(partial, mustExist = false)
                    dao.settlePending(photo.sha256, "missing")
                    broken++
                }
            }
            onProgress(index + 1, waiting.size)
        }
        return fetched to broken
    }
}
