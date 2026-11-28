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

        /**
         * 이 앱이 읽을 수 있는 가장 새 형식
         * 2는 처음 형식, 3은 드라이브 스냅샷 gzip, 4는 기분 15종
         */
        const val FORMAT_VERSION = 4

        /** 드라이브 백업 형식. 스냅샷을 gzip으로 올림 */
        private const val DRIVE_FORMAT_VERSION = 3

        /** 묶음 파일 형식. 스냅샷을 그대로 담아 옛 앱도 읽게 2로 둠 */
        private const val BUNDLE_FORMAT_VERSION = 2

        /** 새 6종 기분이 든 백업. 옛 앱이 새 기분을 "기분 없음"으로 읽고 고쳐 저장하면 지워져 막게 함 */
        private const val NEW_MOODS_FORMAT_VERSION = 4

        /** 드라이브 백업에 적는 형식. 새 기분이 없으면 옛 앱도 읽게 3 */
        fun driveFormat(newMoods: Int): Int = if (newMoods > 0) NEW_MOODS_FORMAT_VERSION else DRIVE_FORMAT_VERSION

        /** 묶음 파일에 적는 형식. 새 기분이 없으면 옛 앱도 읽게 2 */
        fun bundleFormat(newMoods: Int): Int = if (newMoods > 0) NEW_MOODS_FORMAT_VERSION else BUNDLE_FORMAT_VERSION

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

/**
 * [byteSize]와 [sha256]은 올린 파일 그대로의 값이라 드라이브 확인값과 견줌
 * [encoding]이 gzip이면 풀어서 열고, 없으면 그대로 연 SQLite 파일
 */
@Serializable
data class ManifestSnapshot(
    val fileName: String = "",
    val byteSize: Long = 0,
    val sha256: String = "",
    val dbVersion: Int = 0,
    val encoding: String? = null
) {
    companion object {
        const val GZIP = "gzip"
    }
}

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
    /** 있어도 그만인 칸(스펙 008). 옛 앱은 무시하고, 없으면 복원 기기 값을 둠 */
    val previewLength: String? = null,
    val showListFormatting: Boolean? = null,
    val askLocationAgain: Boolean? = null,
    @SerialName("lockEnabled") val lockEnabled: Boolean = false,
    /** 있어도 그만인 칸(스펙 010). 테마 색 이름과 직접 고른 색 `#RRGGBB` */
    val themeColor: String? = null,
    val customThemeColor: String? = null,
    /** 있어도 그만인 칸(스펙 013). 기분 테마 id, 클레이 꼴, 도형 스티커 효과 */
    val moodTheme: String? = null,
    val clayHair: String? = null,
    val claySkin: String? = null,
    val clayGlasses: Boolean? = null,
    val shapeSticker: Boolean? = null
)

/**
 * 읽을 수 있는지
 * 복원 목록의 이유로 씀
 */
enum class ManifestCheck { OK, BROKEN, NEEDS_NEWER_APP }

object ManifestCodec {
    private val READABLE_ENCODINGS = setOf(null, ManifestSnapshot.GZIP)

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

        // 모르는 방식으로 줄인 스냅샷은 더 새 앱이 만든 것
        manifest.snapshot.encoding !in READABLE_ENCODINGS -> ManifestCheck.NEEDS_NEWER_APP

        else -> ManifestCheck.OK
    }
}

/**
 * 밖으로 내보내는 스냅샷 파일
 * 드라이브는 줄인 파일, 묶음 파일은 원래 파일
 */
internal data class OutgoingSnapshot(val fileName: String, val hashes: FileHashes, val encoding: String?)

/** 드라이브 목록 파일과 묶음 파일의 `manifest.json`을 같은 모양으로 만듦 */
internal fun manifestOf(
    formatVersion: Int,
    uploaded: OutgoingSnapshot,
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
    formatVersion = formatVersion,
    createdAt = now.toEpochMilliseconds(),
    timeZone = timeZone.id,
    app = ManifestApp(app.versionName, app.versionCode, app.platform),
    device = ManifestDevice(deviceId, deviceName),
    snapshot =
        ManifestSnapshot(uploaded.fileName, uploaded.hashes.byteSize, uploaded.hashes.sha256, Manifest.MAX_DB_VERSION, uploaded.encoding),
    counts = ManifestCounts(snapshot.entries, snapshot.entriesInTrash, snapshot.mediaRows, snapshot.mediaMissing, snapshot.mediaPending),
    media =
        (local + pending).distinctBy { it.sha256 }.map {
            ManifestMedia(it.sha256, it.fileName?.substringAfterLast('.', "jpg") ?: "jpg", it.byteSize ?: 0, it.sha256 in inBackup)
        },
    settings = settings
)
