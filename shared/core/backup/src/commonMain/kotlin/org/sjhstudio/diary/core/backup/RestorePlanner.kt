package org.sjhstudio.diary.core.backup

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import okio.openZip
import okio.use
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.database.LocalPhotoRow
import org.sjhstudio.diary.core.database.MediaEntity
import org.sjhstudio.diary.core.drive.DriveApi
import org.sjhstudio.diary.core.drive.DriveException
import org.sjhstudio.diary.core.drive.DriveFile
import org.sjhstudio.diary.core.drive.DriveQuery
import org.sjhstudio.diary.core.drive.listAll
import org.sjhstudio.diary.core.model.BackupFailure
import org.sjhstudio.diary.core.model.MediaStatus
import org.sjhstudio.diary.core.model.RestorePreview
import org.sjhstudio.diary.core.platform.FreeSpace

/**
 * 받은 스냅샷이 목록 파일과 맞지 않는다.
 * 기기의 기록은 그대로다(FR-021).
 */
class RestoreMismatch : Exception("backup does not match its manifest")

/** 더 새 앱이 만든 묶음 파일이다. */
class RestoreNeedsUpdate : Exception("backup needs a newer app")

/**
 * 복원 파일에 적힌 이름은 남이 만든 값일 수 있다.
 * 경로에 붙이기 전에 여기서 본다.
 */
internal object RestoreNames {
    private val PHOTO = Regex("[0-9a-f]{64}\\.[A-Za-z0-9]{1,16}")

    /**
     * 사진 파일 이름은 `<내용 SHA-256>.<확장자>`다.
     * 다른 사진의 이름이면 그 사진을 덮으니 해시도 맞아야 한다.
     */
    fun isPhoto(name: String, sha256: String?): Boolean = PHOTO.matches(name) && (sha256 == null || name.substringBefore('.') == sha256)

    /**
     * 스냅샷 이름에는 기기 이름이 들어가 글자를 좁힐 수 없다.
     * 폴더를 넘나들지만 못하게 한다.
     */
    fun isSingleName(name: String): Boolean = name.isNotEmpty() && name != "." && name != ".." && name.none { it in "/\\\u0000" }
}

/**
 * 받아서 확인까지 끝난 백업.
 * 사용자가 확인하면 이 계획대로 합친다.
 */
data class PreparedRestore(
    val manifest: Manifest,
    val plan: MergePlan,
    val preview: RestorePreview,
    val snapshot: Path,
    /**
     * 파일에서 복원할 때의 묶음 파일.
     * 사진을 여기서 꺼낸다.
     */
    val bundle: Path? = null,
    /** 다른 기기가 만든 백업이면 기준값 없는 일기를 둘 다 남긴다(contracts/merge-rules.md 경우 6). */
    val fromOtherDevice: Boolean = false
)

/** 스냅샷 받기 → 확인값·일기 수 확인 → 쓰기 없이 합치기를 돌려 숫자를 낸다(contracts/merge-rules.md). */
class RestorePlanner(
    private val drive: DriveApi,
    private val applier: MergeApplier,
    private val freeSpace: FreeSpace,
    private val fileSystem: FileSystem,
    private val workDir: Path,
    private val thisDevice: (suspend () -> String)? = null
) {
    suspend fun prepare(candidate: RestoreCandidate): PreparedRestore {
        val manifest = readable(candidate.manifest)
        val file = candidate.snapshot ?: throw RestoreMismatch()
        if (!RestoreNames.isSingleName(manifest.snapshot.fileName)) throw RestoreMismatch()
        val incoming = workDir / INCOMING
        fileSystem.createDirectories(incoming)
        // 다른 기기가 지은 이름에는 SQLite가 열지 못하는 글자가 들어 있을 수 있어 받을 때는 이름을 정해 둔다.
        val path = incoming / BundleExporter.SNAPSHOT
        drive.download(file.id, path)
        return checked(manifest, path)
    }

    /** 묶음 파일의 `manifest.json`을 먼저 읽고 스냅샷을 꺼내 드라이브와 같은 확인을 거친다. */
    suspend fun prepareBundle(zip: Path): PreparedRestore {
        val archive = fileSystem.openZip(zip)
        val root = "/".toPath()
        val text = archive.read(root / BundleExporter.MANIFEST) { readUtf8() }
        val manifest = readable(ManifestCodec.decode(text))
        val incoming = workDir / INCOMING
        fileSystem.createDirectories(incoming)
        val path = incoming / BundleExporter.SNAPSHOT
        fileSystem.delete(path, mustExist = false)
        archive.source(root / BundleExporter.SNAPSHOT).use { source -> fileSystem.write(path) { writeAll(source) } }
        return checked(manifest, path).copy(bundle = zip)
    }

    /**
     * 목록 화면이 막아도 이름으로 바로 들어올 수 있어 여기서도 본다.
     * 더 새 앱이 만든 스냅샷은 열지 않는다.
     */
    private fun readable(manifest: Manifest?): Manifest = when (ManifestCodec.check(manifest)) {
        ManifestCheck.OK -> requireNotNull(manifest)
        ManifestCheck.NEEDS_NEWER_APP -> throw RestoreNeedsUpdate()
        ManifestCheck.BROKEN -> throw RestoreMismatch()
    }

    private suspend fun checked(manifest: Manifest, path: Path): PreparedRestore {
        if (fileSystem.hashes(path).sha256 != manifest.snapshot.sha256) {
            fileSystem.delete(path, mustExist = false)
            throw RestoreMismatch()
        }
        val entries = SnapshotReader.read(path)
        val namesOk = entries.all { entry -> entry.media.all { row -> row.fileName?.let { RestoreNames.isPhoto(it, row.sha256) } ?: true } }
        if (!namesOk || entries.count { it.entry.deletedAt == null } != manifest.counts.entries) {
            fileSystem.delete(path, mustExist = false)
            throw RestoreMismatch()
        }
        val fromOtherDevice = thisDevice?.let { manifest.device.id != it() } ?: false
        val plan = applier.plan(entries, fromOtherDevice)
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
        return PreparedRestore(manifest, plan, preview, path, fromOtherDevice = fromOtherDevice)
    }

    private fun neededPhotos(plan: MergePlan, manifest: Manifest): List<ManifestMedia> {
        val local = plan.local.values.flatMap { it.media }
        val here = local.filter { it.status == MediaStatus.NORMAL.id }.mapNotNull { it.sha256 }.toSet()
        // 일기가 그대로여도 전에 못 받은 사진은 이번 백업에서 채운다.
        // 찾을 수 없는 사진도 확인값이 있으면 다시 받는다.
        val waiting = local.filter(::canArrive).mapNotNullTo(HashSet()) { it.sha256 }
        val incoming = plan.items.filter { it.second != MergeAction.KEEP }.flatMap { it.first.media }.mapNotNullTo(HashSet()) { it.sha256 }
        return manifest.media.filter { it.inBackup && it.sha256 !in here && (it.sha256 in incoming || it.sha256 in waiting) }
    }

    private fun canArrive(row: MediaEntity) =
        row.status == MediaStatus.PENDING.id || (row.status == MediaStatus.MISSING.id && row.fileName != null)

    private companion object {
        const val INCOMING = "incoming"
    }
}

/** 휴대폰에 사진을 쓸 공간이 없다. 끊겨도 받은 사진은 그대로다. */
class NoRoomForPhotos : Exception("no room for photos")

/**
 * 사진 받기 한 번의 숫자.
 * 중간에 멈춰도 그때까지의 값이 남는다.
 * [notThere]는 백업에 없어 다시 시도해도 받을 수 없는 장수다.
 */
data class PhotoTally(var fetched: Int = 0, var broken: Int = 0, var notThere: Int = 0)

/**
 * 드라이브에서 찾아본 대기 사진.
 * 드라이브에 없는 사진은 받을 길이 없어 [notThere]로 따로 센다.
 */
class DrivePhotos(val files: Map<String, DriveFile>, val reachable: Int, val notThere: Int)

/**
 * 합친 뒤 대기 중인 사진을 받는다.
 * 끊겨도 다음에 대기 줄만 다시 받는다(FR-024).
 */
class PhotoFetcher(
    private val db: DiaryDatabase,
    private val drive: DriveApi,
    private val fileSystem: FileSystem,
    private val mediaDir: Path,
    private val workDir: Path,
    private val freeSpace: FreeSpace = FreeSpace { Long.MAX_VALUE }
) {
    /** 대기 사진이 드라이브에 있는지 찾아본다. */
    suspend fun look(): DrivePhotos {
        val pending = db.backupDao().pendingFiles()
        if (pending.isEmpty()) return DrivePhotos(emptyMap(), 0, 0)
        val onDrive: Map<String, DriveFile> =
            drive.listAll(DriveQuery(props = mapOf(DriveLayout.KEY_KIND to DriveLayout.KIND_PHOTO))).associateBy {
                it.name.substringBefore('.')
            }
        val reachable = pending.count { it.sha256 in onDrive }
        return DrivePhotos(onDrive, reachable, pending.size - reachable)
    }

    /** 받은 수와 깨져서 쓸 수 없던 수를 [tally]에 적는다. */
    suspend fun fetch(tally: PhotoTally = PhotoTally(), onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }): PhotoTally =
        fetch(look(), tally, onProgress)

    suspend fun fetch(found: DrivePhotos, tally: PhotoTally, onProgress: (done: Int, total: Int) -> Unit): PhotoTally {
        // 드라이브에 없는 사진은 진행과 이어 받기의 숫자에 넣지 않는다.
        tally.notThere += found.notThere
        if (found.reachable == 0) return tally
        return settle(tally, onProgress, PARALLEL_DOWNLOADS, { it.sha256 in found.files }) { sha256, _, target ->
            drive.download(requireNotNull(found.files[sha256]).id, target)
            true
        }
    }

    /**
     * 묶음 파일에서 꺼낸다.
     * 맞지 않는 사진만 찾을 수 없는 사진이 되고 나머지는 합친다.
     */
    suspend fun fetchFrom(
        zip: Path,
        tally: PhotoTally = PhotoTally(),
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }
    ): PhotoTally {
        val archive = fileSystem.openZip(zip)
        val media = "/".toPath() / BundleExporter.MEDIA
        return settle(tally, onProgress) { _, name, target ->
            val entry = media / name
            val photo =
                try {
                    if (!archive.exists(entry)) return@settle false
                    archive.source(entry)
                } catch (_: IOException) {
                    // 이 항목의 머리가 깨졌다.
                    // 다른 사진까지 멈추지 않는다.
                    throw BrokenPhoto()
                }
            photo.use { source -> fileSystem.write(target) { writeAll(source) } }
            true
        }
    }

    private suspend fun settle(
        tally: PhotoTally,
        onProgress: (done: Int, total: Int) -> Unit,
        parallel: Int = 1,
        wanted: (LocalPhotoRow) -> Boolean = { true },
        copy: suspend (sha256: String, name: String, target: Path) -> Boolean
    ): PhotoTally {
        // 같은 이름이 두 줄이면 같은 `.part`를 함께 쓰게 되니 시작 전에 파일 이름으로 묶는다.
        val waiting = db.backupDao().pendingFiles().filter(wanted).distinctBy { it.fileName ?: it.sha256 }
        fileSystem.createDirectories(workDir)
        fileSystem.createDirectories(mediaDir)
        val results = Channel<Settled>(Channel.UNLIMITED)
        val gate = Semaphore(parallel)
        return coroutineScope {
            waiting.forEach { photo ->
                launch(Dispatchers.IO) { gate.withPermit { results.send(settleOne(photo, copy)) } }
            }
            repeat(waiting.size) { index ->
                when (results.receive()) {
                    Settled.FETCHED -> tally.fetched++
                    Settled.BROKEN -> tally.broken++
                    Settled.STILL_WAITING -> tally.notThere++
                }
                onProgress(index + 1, waiting.size)
            }
            tally
        }
    }

    private suspend fun settleOne(photo: LocalPhotoRow, copy: suspend (sha256: String, name: String, target: Path) -> Boolean): Settled {
        val dao = db.backupDao()
        // 백업에 없는 사진은 대기로 두어 "원래 기기에 있는 사진"으로 보인다.
        val name = photo.fileName ?: return Settled.STILL_WAITING
        // 전에 받아 둔 줄일 수 있어 여기서도 본다.
        // 이 이름으로는 파일을 쓰지 않는다.
        if (!RestoreNames.isPhoto(name, photo.sha256)) {
            dao.settlePending(photo.sha256, name, MediaStatus.MISSING.id)
            return Settled.BROKEN
        }
        val partial = workDir / "$name.part"
        val needed = (photo.byteSize ?: 0) + ROOM_MARGIN
        if (freeSpace.bytes() < needed) throw NoRoomForPhotos()
        try {
            if (!copy(photo.sha256, name, partial)) return Settled.STILL_WAITING
            if (fileSystem.hashes(partial).sha256 != photo.sha256) {
                dao.settlePending(photo.sha256, name, MediaStatus.MISSING.id)
                return Settled.BROKEN
            }
            fileSystem.atomicMove(partial, mediaDir / name)
            dao.settlePending(photo.sha256, name, MediaStatus.NORMAL.id)
            return Settled.FETCHED
        } catch (_: BrokenPhoto) {
            dao.settlePending(photo.sha256, name, MediaStatus.MISSING.id)
            return Settled.BROKEN
        } catch (error: DriveException) {
            // 이 사진만의 일이면 대기로 두고 다음 장으로 간다.
            // 어차피 다 실패할 일이면 여기서 멈춘다.
            if (error.stopsAllPhotos) throw error
            return Settled.STILL_WAITING
        } catch (error: IOException) {
            // 쓰다가 공간이 찼다.
            // 파일이 깨졌다고 말하지 않게 까닭을 바꿔 알린다.
            if (freeSpace.bytes() < needed) throw NoRoomForPhotos()
            throw error
        } finally {
            fileSystem.delete(partial, mustExist = false)
        }
    }

    /** 잠깐 끊겼거나 로그인이 풀리면 다음 장도 똑같이 실패한다. */
    private val DriveException.stopsAllPhotos: Boolean
        get() = retryable || kind == BackupFailure.AUTH || kind == BackupFailure.OFFLINE

    private enum class Settled { FETCHED, BROKEN, STILL_WAITING }

    private class BrokenPhoto : Exception("photo entry is damaged")

    private companion object {
        /**
         * 드라이브에서 동시에 받는 사진 수.
         * 사진 한 장은 작아서 기다리는 시간이 대부분이다.
         */
        const val PARALLEL_DOWNLOADS = 6

        /** 사진을 쓰고 남겨 둘 공간. */
        const val ROOM_MARGIN = 1024L * 1024
    }
}
