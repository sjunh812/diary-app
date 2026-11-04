package org.sjhstudio.diary.core.platform

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.sjhstudio.diary.core.model.BodyTextSize
import org.sjhstudio.diary.core.model.DiaryFont
import org.sjhstudio.diary.core.model.MigrationFailureKind
import org.sjhstudio.diary.core.model.MigrationStep
import org.sjhstudio.diary.core.model.RelockAfter
import org.sjhstudio.diary.core.model.ThemeMode

/**
 * 사용 통계로 보낼 수 있는 이벤트와 값의 고정 표(contracts/app-shell.md, contracts/migration-flow.md).
 * 값은 열거형 ID와 개수 구간뿐이다.
 * 일기 글, 장소, 기분, 경로, 예외 메시지는 표에 들어올 자리가 없다(헌법 II).
 */
object AnalyticsEvents {
    const val SCREEN_VIEW = "screen_view"
    const val SETTINGS_CHANGED = "settings_changed"
    const val LOCK_RESULT = "lock_result"
    const val MIGRATION_STARTED = "migration_started"
    const val MIGRATION_FINISHED = "migration_finished"
    const val MIGRATION_FAILED = "migration_failed"

    /** contracts/app-shell.md 화면 키 이름. */
    val SCREENS =
        setOf(
            "HomeKey",
            "CalendarKey",
            "InsightKey",
            "WriteKey",
            "SettingsKey",
            "FontSettingsKey",
            "TextSizeSettingsKey",
            "ThemeSettingsKey",
            "LockSettingsKey",
            "PinSetupKey",
            "ReminderSettingsKey",
            "MigrationReportKey",
            "LicensesKey",
            "FontLicenseKey",
            "LibraryLicenseKey",
            "WriteKey",
            "EntryDetailKey",
            "PhotoViewerKey"
        )

    val COUNT_BUCKETS = listOf("0", "1-9", "10-99", "100-999", "1000-9999", "10000+")
    val DURATION_BUCKETS = listOf("<10s", "<30s", "<2m", "<10m", "10m+")

    fun countBucket(count: Int): String = when {
        count <= 0 -> "0"
        count < 10 -> "1-9"
        count < 100 -> "10-99"
        count < 1_000 -> "100-999"
        count < 10_000 -> "1000-9999"
        else -> "10000+"
    }

    fun durationBucket(duration: Duration): String = when {
        duration < 10.seconds -> "<10s"
        duration < 30.seconds -> "<30s"
        duration < 2.minutes -> "<2m"
        duration < 10.minutes -> "<10m"
        else -> "10m+"
    }

    private val BOOLEAN = setOf("true", "false")
    private val ATTEMPTS = (1..99).map(Int::toString).toSet()

    /** `settings_changed`의 `name`마다 받을 수 있는 `value`. */
    private val SETTING_VALUES: Map<String, Set<String>> =
        mapOf(
            "font" to DiaryFont.entries.map { it.id }.toSet(),
            "text_size" to BodyTextSize.entries.map { it.id }.toSet(),
            "theme" to ThemeMode.entries.map { it.id }.toSet(),
            "reminder" to BOOLEAN,
            "lock" to BOOLEAN,
            "biometric" to BOOLEAN,
            "relock_after" to RelockAfter.entries.map { it.id }.toSet()
        )

    /** 이벤트마다 받을 수 있는 키와 값. */
    private val TABLE: Map<String, Map<String, Set<String>>> =
        mapOf(
            SCREEN_VIEW to mapOf("screen" to SCREENS),
            SETTINGS_CHANGED to mapOf("name" to SETTING_VALUES.keys, "value" to SETTING_VALUES.values.flatten().toSet()),
            LOCK_RESULT to mapOf("result" to setOf("ok", "wrong", "waiting", "biometric_ok", "biometric_cancel")),
            MIGRATION_STARTED to
                mapOf("entries_bucket" to COUNT_BUCKETS.toSet(), "photos_bucket" to COUNT_BUCKETS.toSet(), "attempt" to ATTEMPTS),
            MIGRATION_FINISHED to
                mapOf(
                    "outcome" to setOf("done", "partial"),
                    "duration_bucket" to DURATION_BUCKETS.toSet(),
                    "missing_photos_bucket" to COUNT_BUCKETS.toSet(),
                    "unreadable_bucket" to COUNT_BUCKETS.toSet()
                ),
            MIGRATION_FAILED to
                mapOf(
                    "step" to MigrationStep.entries.map { it.id }.toSet(),
                    "kind" to MigrationFailureKind.entries.map { it.name }.toSet(),
                    "attempt" to ATTEMPTS
                )
        )

    /**
     * 표에 맞으면 null, 아니면 무엇이 어긋났는지.
     * 설명에는 이벤트와 키 이름만 넣고 값은 넣지 않는다.
     */
    fun violation(event: String, params: Map<String, String>): String? {
        val allowed = TABLE[event] ?: return "표에 없는 이벤트: $event"
        params.forEach { (key, value) ->
            val values = allowed[key] ?: return "$event 에 없는 키: $key"
            if (value !in values) return "$event.$key 에 맞지 않는 값"
        }
        if (event == SETTINGS_CHANGED) {
            val name = params["name"] ?: return "$event 에 name이 없다"
            val value = params["value"] ?: return "$event 에 value가 없다"
            if (value !in SETTING_VALUES.getValue(name)) return "$event.value 가 $name 에 맞지 않는다"
        }
        return null
    }
}
