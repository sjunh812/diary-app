package org.sjhstudio.diary.core.backup

import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.sjhstudio.diary.core.database.LocalPhotoRow

/**
 * 목록 파일(contracts/backup-format.md)
 * 일기 내용, 장소, 기분, 사진 경로, 잠금 확인값은 담지 않음
 */
@Serializable
data class Manifest(
    val format: String = FORMAT,
    val formatVersion: Int = FORMAT_VERSION,
    val createdAt: Long = 0,
    val timeZone: String = "",
    val app: ManifestApp = ManifestApp(),
    val device: ManifestDevice = ManifestDevice(),
    val snapshot: ManifestSnapshot = ManifestSnapshot(),
    val counts: ManifestCounts = ManifestCounts(),
    val media: List<ManifestMedia> = emptyList(),
    val settings: ManifestSettings = ManifestSettings()
) {
    companion object {
        const val FORMAT = "org.sjhstudio.diary.backup"
        const val FORMAT_VERSION = 2

        /** 이 앱이 읽을 수 있는 스냅샷 DB 버전 */
        // 스냅샷에 든 표의 판
        // 임시 저장본은 스냅샷에 싣지 않아 그 표만 바뀐 DB 버전 3은 이 값을 올리지 않음
        const val MAX_DB_VERSION = 2
    }
}

@Serializable
data class ManifestApp(val versionName: String = "", val versionCode: Int = 0, val platform: String = "")

@Serializable
data class ManifestDevice(val id: String = "", val name: String = "")

@Serializable
data class ManifestSnapshot(val fileName: String = "", val byteSize: Long = 0, val sha256: String = "", val dbVersion: Int = 0)

@Serializable
data class ManifestCounts(
    val entries: Int = 0,
    val entriesInTrash: Int = 0,
    val mediaRows: Int = 0,
    val mediaMissing: Int = 0,
    val mediaPending: Int = 0
)

@Serializable
data class ManifestMedia(val sha256: String, val ext: String, val byteSize: Long, val inBackup: Boolean)

@Serializable
data class ManifestSettings(
    val font: String? = null,
    val bodyTextSize: String? = null,
    val themeMode: String? = null,
    val reminderEnabled: Boolean? = null,
    val reminderMinuteOfDay: Int? = null,
    val listStyle: String? = null,
    val favoritesOnly: Boolean? = null,
    val askLocationAgain: Boolean? = null,
    @SerialName("lockEnabled") val lockEnabled: Boolean = false
)

/**
 * 읽을 수 있는지
 * 복원 목록의 이유로 씀
 */
enum class ManifestCheck { OK, BROKEN, NEEDS_NEWER_APP }

object ManifestCodec {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            explicitNulls = false
            coerceInputValues = true
        }

    fun encode(manifest: Manifest): String = json.encodeToString(Manifest.serializer(), manifest)

    /**
     * 깨졌으면 null
     * 모르는 키는 무시하고 없는 키는 기본값으로 봄
     */
    fun decode(text: String): Manifest? = runCatching { json.decodeFromString(Manifest.serializer(), text) }.getOrNull()?.takeIf {
        it.format ==
            Manifest.FORMAT
    }

    fun check(manifest: Manifest?): ManifestCheck = when {
        manifest == null -> ManifestCheck.BROKEN
        manifest.formatVersion > Manifest.FORMAT_VERSION -> ManifestCheck.NEEDS_NEWER_APP
        manifest.snapshot.dbVersion > Manifest.MAX_DB_VERSION -> ManifestCheck.NEEDS_NEWER_APP
        else -> ManifestCheck.OK
    }
}

/** 드라이브 목록 파일과 묶음 파일의 `manifest.json`을 같은 모양으로 만듦 */
internal fun manifestOf(
    now: Instant,
    timeZone: TimeZone,
    app: BackupAppInfo,
    deviceId: String,
    deviceName: String,
    snapshot: Snapshot,
    local: List<LocalPhotoRow>,
    pending: List<LocalPhotoRow>,
    inBackup: Set<String>,
    settings: ManifestSettings
) = Manifest(
    createdAt = now.toEpochMilliseconds(),
    timeZone = timeZone.id,
    app = ManifestApp(app.versionName, app.versionCode, app.platform),
    device = ManifestDevice(deviceId, deviceName),
    snapshot = ManifestSnapshot(snapshot.path.name, snapshot.hashes.byteSize, snapshot.hashes.sha256, Manifest.MAX_DB_VERSION),
    counts = ManifestCounts(snapshot.entries, snapshot.entriesInTrash, snapshot.mediaRows, snapshot.mediaMissing, snapshot.mediaPending),
    media =
        (local + pending).distinctBy { it.sha256 }.map {
            ManifestMedia(it.sha256, it.fileName?.substringAfterLast('.', "jpg") ?: "jpg", it.byteSize ?: 0, it.sha256 in inBackup)
        },
    settings = settings
)
