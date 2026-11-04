package org.sjhstudio.diary.core.backup

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.openZip
import okio.use
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.drive.DriveApi
import org.sjhstudio.diary.core.drive.DriveFile
import org.sjhstudio.diary.core.drive.DriveQuery
import org.sjhstudio.diary.core.drive.listAll
import org.sjhstudio.diary.core.model.RestorePreview
import org.sjhstudio.diary.core.platform.FreeSpace

/** 받은 스냅샷이 목록 파일과 맞지 않는다. 기기의 기록은 그대로다(FR-021). */
class RestoreMismatch : Exception("backup does not match its manifest")

/** 더 새 앱이 만든 묶음 파일이다. */
class RestoreNeedsUpdate : Exception("backup needs a newer app")

/** 받아서 확인까지 끝난 백업. 사용자가 확인하면 이 계획대로 합친다. */
data class PreparedRestore(
    val manifest: Manifest,
    val plan: MergePlan,
    val preview: RestorePreview,
    val snapshot: Path,
    /** 파일에서 되살릴 때의 묶음 파일. 사진을 여기서 꺼낸다. */
    val bundle: Path? = null
)

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
        return checked(manifest, path)
    }

    /** 묶음 파일의 `manifest.json`을 먼저 읽고 스냅샷을 꺼내 드라이브와 같은 확인을 거친다. */
    suspend fun prepareBundle(zip: Path): PreparedRestore {
        val archive = fileSystem.openZip(zip)
        val root = "/".toPath()
        val text = archive.read(root / BundleExporter.MANIFEST) { readUtf8() }
        val manifest = ManifestCodec.decode(text)
        when (ManifestCodec.check(manifest)) {
            ManifestCheck.OK -> Unit
            ManifestCheck.NEEDS_NEWER_APP -> throw RestoreNeedsUpdate()
            ManifestCheck.BROKEN -> throw RestoreMismatch()
        }
        val incoming = workDir / INCOMING
        fileSystem.createDirectories(incoming)
        val path = incoming / BundleExporter.SNAPSHOT
        fileSystem.delete(path, mustExist = false)
        archive.source(root / BundleExporter.SNAPSHOT).use { source -> fileSystem.write(path) { writeAll(source) } }
        return checked(requireNotNull(manifest), path).copy(bundle = zip)
    }

    private suspend fun checked(manifest: Manifest, path: Path): PreparedRestore {
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
                deviceBytesFree = freeSpace.bytes(),
                entriesHere = plan.local.values.count { it.entry.deletedAt == null }
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
        if (db.backupDao().pendingFiles().isEmpty()) return 0 to 0
        val onDrive: Map<String, DriveFile> =
            drive.listAll(DriveQuery(props = mapOf(DriveLayout.KEY_KIND to DriveLayout.KIND_PHOTO))).associateBy {
                it.name.substringBefore('.')
            }
        return settle(onProgress) { sha256, _, target ->
            val file = onDrive[sha256] ?: return@settle false
            drive.download(file.id, target)
            true
        }
    }

    /** 묶음 파일에서 꺼낸다. 맞지 않는 사진만 찾을 수 없는 사진이 되고 나머지는 합친다. */
    suspend fun fetchFrom(zip: Path, onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }): Pair<Int, Int> {
        val archive = fileSystem.openZip(zip)
        val media = "/".toPath() / BundleExporter.MEDIA
        return settle(onProgress) { _, name, target ->
            val entry = media / name
            if (!archive.exists(entry)) return@settle false
            archive.source(entry).use { source -> fileSystem.write(target) { writeAll(source) } }
            true
        }
    }

    private suspend fun settle(
        onProgress: (done: Int, total: Int) -> Unit,
        copy: suspend (sha256: String, name: String, target: Path) -> Boolean
    ): Pair<Int, Int> {
        val dao = db.backupDao()
        val waiting = dao.pendingFiles()
        fileSystem.createDirectories(workDir)
        fileSystem.createDirectories(mediaDir)
        var fetched = 0
        var broken = 0
        waiting.forEachIndexed { index, photo ->
            val name = photo.fileName
            val partial = name?.let { workDir / "$it.part" }
            // 백업에 없는 사진은 대기로 두어 "원래 기기에 있는 사진"으로 보인다.
            if (name != null && partial != null && copy(photo.sha256, name, partial)) {
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
