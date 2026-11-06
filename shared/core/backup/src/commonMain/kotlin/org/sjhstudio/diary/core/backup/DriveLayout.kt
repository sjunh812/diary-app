package org.sjhstudio.diary.core.backup

import okio.FileSystem
import okio.Path
import org.sjhstudio.diary.core.drive.DriveApi
import org.sjhstudio.diary.core.drive.DriveFile
import org.sjhstudio.diary.core.drive.DriveQuery
import org.sjhstudio.diary.core.drive.NewFile
import org.sjhstudio.diary.core.drive.listAll

/** 드라이브의 백업 폴더 셋. */
data class DriveFolders(val root: String, val snapshots: String, val media: String)

/**
 * 폴더를 이름이 아니라 `appProperties`로 찾는다(contracts/backup-format.md).
 * 사용자가 이름을 바꿔도 이어진다.
 */
class DriveLayout(private val drive: DriveApi, private val fileSystem: FileSystem) {
    suspend fun ensure(workDir: Path, readme: String): DriveFolders {
        val root =
            find(KIND_ROOT, null)
                ?: drive.createFolder(ROOT_NAME, null, mapOf(KEY_KIND to KIND_ROOT)).also { uploadReadme(it.id, workDir, readme) }
        val snapshots = find(KIND_SNAPSHOTS, root.id) ?: drive.createFolder("snapshots", root.id, mapOf(KEY_KIND to KIND_SNAPSHOTS))
        val media = find(KIND_MEDIA, root.id) ?: drive.createFolder("media", root.id, mapOf(KEY_KIND to KIND_MEDIA))
        return DriveFolders(root.id, snapshots.id, media.id)
    }

    /** 같은 종류가 여럿이면 가장 먼저 만든 것을 쓴다. */
    private suspend fun find(kind: String, parent: String?): DriveFile? =
        drive.listAll(DriveQuery(parentId = parent, props = mapOf(KEY_KIND to kind))).minByOrNull { it.createdAt }

    private suspend fun uploadReadme(root: String, workDir: Path, text: String) {
        fileSystem.createDirectories(workDir)
        val path = workDir / "README.txt"
        fileSystem.write(path) { writeUtf8(text) }
        drive.uploadSmall(NewFile("README.txt", root, "text/plain"), path)
        fileSystem.delete(path)
    }

    companion object {
        const val ROOT_NAME = "1일 1일기 백업"
        const val KEY_KIND = "diary.kind"
        const val KEY_DEVICE = "diary.device"
        const val KEY_FORMAT = "diary.format"
        const val KEY_UNREF_SINCE = "diary.unrefSince"
        const val KIND_ROOT = "root"
        const val KIND_SNAPSHOTS = "snapshots"
        const val KIND_MEDIA = "media"
        const val KIND_SNAPSHOT = "snapshot"
        const val KIND_MANIFEST = "manifest"
        const val KIND_PHOTO = "photo"
    }
}

/**
 * `<기기 현지 시각>_<기기 이름, 공백은 ->_<기기 ID 앞 4자>`.
 * 드라이브에서 사람이 보고 고를 수 있게 한다.
 * 이 이름이 파일 이름도 되니 기기 이름이 백업을 막지 못하게 줄인다.
 * 이모지처럼 두 글자로 쓰는 문자는 SQLite 드라이버가 경로로 넘기면서 바이트가 달라져 파일을 열지 못한다.
 * 긴 이름은 파일 시스템의 이름 한도에 걸린다.
 * 기기 이름 원문은 목록 파일에 그대로 적는다.
 */
fun snapshotBaseName(localTime: String, deviceName: String, deviceId: String): String {
    val device =
        deviceName.filterNot { it.isSurrogate() || it.isISOControl() }.trim().replace(Regex("""[\s/\\:]+"""), "-")
            .take(MAX_DEVICE_CHARS).ifEmpty { "device" }
    return "${localTime}_${device}_${deviceId.take(4)}"
}

/** 한글 한 글자가 3바이트라 시각과 확장자를 더해도 255바이트 안에 든다. */
private const val MAX_DEVICE_CHARS = 40
