package org.sjhstudio.diary.core.model

/**
 * 앱 설정(`settings.preferences_pb`). 기본값은 data-model.md 설정 표 그대로다.
 */
data class AppSettings(
    val font: DiaryFont = DiaryFont.DEFAULT,
    val bodyTextSize: BodyTextSize = BodyTextSize.DEFAULT,
    val themeMode: ThemeMode = ThemeMode.DEFAULT,
    /** 레거시 `ask_location`. 위치를 쓸 때 다시 물을지. */
    val askLocationAgain: Boolean = true,
    val reminderEnabled: Boolean = false,
    /** 0시부터 센 분. 기본 22:00. */
    val reminderMinuteOfDay: Int = DEFAULT_REMINDER_MINUTE_OF_DAY,
    /** 옛 글꼴이 대응표로 바뀌었을 때 옛 글꼴 이름. 처음 열 때 한 번 알리고 지운다. */
    val fontReplacedFrom: String? = null,
    /** 옛 앱이 드라이브에 올린 백업(스펙 003이 이전 백업 찾기에 쓴다). */
    val legacyBackup: LegacyBackupLink? = null,
    /** 홈 목록 모양과 즐겨찾기만 보기(스펙 002 FR-018). */
    val list: ListPreferences = ListPreferences()
) {
    companion object {
        const val DEFAULT_REMINDER_MINUTE_OF_DAY = 22 * 60
    }
}
