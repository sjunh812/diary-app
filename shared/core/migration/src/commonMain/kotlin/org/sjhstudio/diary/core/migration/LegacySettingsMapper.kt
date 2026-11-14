package org.sjhstudio.diary.core.migration

import org.sjhstudio.diary.core.model.AppSettings
import org.sjhstudio.diary.core.model.BodyTextSize
import org.sjhstudio.diary.core.model.DiaryFont
import org.sjhstudio.diary.core.model.LegacyBackupLink
import org.sjhstudio.diary.core.model.MigratedSetting
import org.sjhstudio.diary.core.model.ThemeMode

/**
 * 옛 앱 설정(`pref.xml`, `pref2.xml`)을 새 설정으로 옮김(contracts/legacy-mapping.md "설정", data-model.md DiaryFont·BodyTextSize)
 * 잠금은 [LegacyLockBootstrap]이 따로 옮김
 */
object LegacySettingsMapper {
    fun map(pref: Map<String, Any>, pref2: Map<String, Any>, base: AppSettings = AppSettings()): AppSettings {
        val fontKey = pref[FONT_KEY] as? Int ?: -1
        val font = font(fontKey)
        val hour = pref2[HOUR_KEY] as? Int
        val minute = pref2[MINUTE_KEY] as? Int
        val reminderMinute =
            if (hour != null && minute != null && hour in 0..23 &&
                minute in 0..59
            ) {
                hour * 60 + minute
            } else {
                AppSettings.DEFAULT_REMINDER_MINUTE_OF_DAY
            }
        return base.copy(
            font = font,
            fontReplacedFrom = LEGACY_FONT_NAMES[fontKey].takeIf { fontKey !in SAME_FONT_KEYS },
            bodyTextSize = textSize(pref[FONT_SIZE] as? Int),
            themeMode =
                when (pref[MODE_KEY] as? Int) {
                    1 -> ThemeMode.LIGHT
                    2 -> ThemeMode.DARK
                    else -> ThemeMode.SYSTEM
                },
            askLocationAgain = pref[ASK_LOCATION] as? Boolean ?: true,
            reminderEnabled = pref2[IS_ALARM_KEY] as? Boolean ?: false,
            reminderMinuteOfDay = reminderMinute,
            legacyBackup = backupLink(pref)
        )
    }

    /**
     * 옛 파일에 실제로 있던 설정(이전 결과의 `settingsMigrated`)
     * 값이 없어 기본값을 쓴 설정은 넣지 않음
     * 잠금과 생체 인증은 켜 두었을 때만 넣음
     */
    fun migrated(pref: Map<String, Any>, pref2: Map<String, Any>): List<MigratedSetting> = buildList {
        if (FONT_KEY in pref) add(MigratedSetting.FONT)
        if (FONT_SIZE in pref) add(MigratedSetting.BODY_TEXT_SIZE)
        if (MODE_KEY in pref) add(MigratedSetting.THEME_MODE)
        if (ASK_LOCATION in pref) add(MigratedSetting.ASK_LOCATION)
        if (pref[USE_PW] == true) add(MigratedSetting.LOCK)
        if (pref[USE_PW] == true && pref[FINGER_PRINT] == true) add(MigratedSetting.BIOMETRIC)
        if (IS_ALARM_KEY in pref2) add(MigratedSetting.REMINDER)
        if (HOUR_KEY in pref2 && MINUTE_KEY in pref2) add(MigratedSetting.REMINDER_TIME)
    }

    private fun font(key: Int): DiaryFont = when (key) {
        100 -> DiaryFont.SYSTEM
        1, 5 -> DiaryFont.NANUM_CHEER
        2, 7, 11 -> DiaryFont.KYOBO_2020
        3 -> DiaryFont.NANUM_JEOMGGOL
        4, 10 -> DiaryFont.RIX_WATERMELON
        6, 13 -> DiaryFont.SEJONG_GEULGGOT
        8 -> DiaryFont.GOWUN_BATANG
        9, 12 -> DiaryFont.CHANGWON_ROUND
        else -> DiaryFont.PRETENDARD
    }

    private fun textSize(key: Int?): BodyTextSize = when (key) {
        0, 1 -> BodyTextSize.SMALL
        3 -> BodyTextSize.LARGE
        4 -> BodyTextSize.XLARGE
        else -> BodyTextSize.MEDIUM
    }

    /** 값이 true인 이메일 모양 키가 계정, `"<계정>'s id"` 글이 드라이브 파일 ID(레거시 `BackupActivity`) */
    private fun backupLink(pref: Map<String, Any>): LegacyBackupLink? {
        val account = pref.entries.firstOrNull { (key, value) -> value == true && EMAIL.matches(key) }?.key ?: return null
        return LegacyBackupLink(account, (pref["$account's id"] as? String)?.takeIf { it.isNotBlank() })
    }

    private const val FONT_KEY = "font_key"
    private const val FONT_SIZE = "font_size"
    private const val MODE_KEY = "mode_key"
    private const val ASK_LOCATION = "ask_location"
    private const val IS_ALARM_KEY = "is_alarm_key"
    private const val HOUR_KEY = "hour_key"
    private const val MINUTE_KEY = "minute_key"

    // 잠금은 LegacyLockBootstrap이 옮기고, 여기서는 옮겼는지만 확인
    private const val USE_PW = "use_pw"
    private const val FINGER_PRINT = "finger_print"
    private val EMAIL = Regex("""[^@\s]+@[^@\s]+\.[^@\s]+""")

    /** 새 앱에도 같은 글꼴이 있어 바뀌었다고 알리지 않는 키(프리텐다드, 시스템, 점꼴체) */
    private val SAME_FONT_KEYS = setOf(-1, 0, 3, 100)

    /**
     * 1.7.4 글꼴 고르기 화면의 이름(`activity_font.xml`)
     * 바뀐 글꼴을 처음 열 때 한 번 알리는 데 씀
     */
    private val LEGACY_FONT_NAMES =
        mapOf(
            1 to "국립공원 꼬미체",
            2 to "교보손글씨체",
            4 to "넥슨배찌체",
            5 to "미니콩다방체",
            6 to "꼬마나비체",
            7 to "심경하체",
            8 to "강원교육모두체",
            9 to "쿠키런체",
            10 to "온글잎 만두몽키체",
            11 to "온글잎 윤우체",
            12 to "코트라 희망체",
            13 to "ACC 어린이 마음고운체"
        )
}
