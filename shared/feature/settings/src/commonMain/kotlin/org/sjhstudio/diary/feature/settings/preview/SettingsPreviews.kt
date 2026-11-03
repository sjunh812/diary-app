package org.sjhstudio.diary.feature.settings.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.datetime.LocalDate
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.AppSettings
import org.sjhstudio.diary.core.model.BodyTextSize
import org.sjhstudio.diary.core.model.DiaryFont
import org.sjhstudio.diary.core.model.LockSettings
import org.sjhstudio.diary.core.model.RelockAfter
import org.sjhstudio.diary.core.model.ThemeMode
import org.sjhstudio.diary.feature.settings.FontSettingsScreen
import org.sjhstudio.diary.feature.settings.FontUiState
import org.sjhstudio.diary.feature.settings.ReminderSettingsScreen
import org.sjhstudio.diary.feature.settings.ReminderUiState
import org.sjhstudio.diary.feature.settings.SettingsScreen
import org.sjhstudio.diary.feature.settings.SettingsUiState
import org.sjhstudio.diary.feature.settings.TextSizeSettingsScreen
import org.sjhstudio.diary.feature.settings.ThemeSettingsScreen

// 설정 목록(S6), 글꼴(G1), 본문 글자 크기(G2), 화면 모드(G3), 매일 알림(R1) 프리뷰. 값은 Figma 화면과 같다.

private val sample =
    SettingsUiState(
        settings = AppSettings(font = DiaryFont.KKOMI, bodyTextSize = BodyTextSize.LARGE, reminderEnabled = true),
        lock = LockSettings(enabled = true, pinMac = "bWFj", pinSalt = "c2FsdA==", relockAfter = RelockAfter.ONE_MINUTE),
        showsMigrationReport = true
    )

@Composable
private fun SettingsList(dark: Boolean) = DiaryTheme(darkTheme = dark) {
    SettingsScreen(sample, onFont = {
    }, onTextSize = {}, onTheme = {}, onReminder = {}, onLock = {}, onMigrationReport = {}, onLicenses = {})
}

@Preview(name = "S6 설정 · 라이트", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun ListLight() = SettingsList(dark = false)

@Preview(name = "S6 설정 · 다크", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun ListDark() = SettingsList(dark = true)

/** 개발 빌드에만 있는 "개발" 묶음이 맨 아래 붙는다(FR-029). */
@Preview(name = "S6 설정 · 개발 빌드", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun ListDeveloper() = DiaryTheme(darkTheme = false) {
    SettingsScreen(sample, onFont = {
    }, onTextSize = {}, onTheme = {}, onReminder = {}, onLock = {}, onMigrationReport = {}, onLicenses = {}, onDesignCatalog = {})
}

@Composable
private fun TextSize(dark: Boolean) = DiaryTheme(darkTheme = dark) {
    TextSizeSettingsScreen(BodyTextSize.LARGE, sampleDate = LocalDate(2026, 10, 31), font = DiaryFont.KKOMI, onSelect = {}, onBack = {})
}

@Preview(name = "G2 본문 글자 크기 · 라이트", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun TextSizeLight() = TextSize(dark = false)

@Preview(name = "G2 본문 글자 크기 · 다크", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun TextSizeDark() = TextSize(dark = true)

@Composable
private fun Theme(dark: Boolean) = DiaryTheme(darkTheme = dark) {
    ThemeSettingsScreen(ThemeMode.SYSTEM, onSelect = {}, onBack = {})
}

@Preview(name = "G3 화면 모드 · 라이트", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun ThemeLight() = Theme(dark = false)

@Preview(name = "G3 화면 모드 · 다크", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun ThemeDark() = Theme(dark = true)

@Composable
private fun Reminder(dark: Boolean, state: ReminderUiState) = DiaryTheme(darkTheme = dark) {
    ReminderSettingsScreen(state, timeLabel = "오후 10:00", onEnabledChange = {}, onTime = {}, onAllow = {}, onBack = {})
}

@Preview(name = "R1 매일 알림 · 라이트", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun ReminderLight() = Reminder(dark = false, ReminderUiState(enabled = true, notificationsAllowed = true))

@Preview(name = "R1 매일 알림 · 다크", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun ReminderDark() = Reminder(dark = true, ReminderUiState(enabled = true, notificationsAllowed = true))

@Preview(name = "R1 매일 알림 · 허용 필요", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun ReminderNeedsPermission() = Reminder(dark = false, ReminderUiState(enabled = true, notificationsAllowed = false))

@Preview(name = "R1 매일 알림 · 끔", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun ReminderOff() = Reminder(dark = false, ReminderUiState(enabled = false))

private val fontSample = FontUiState(settings = AppSettings(font = DiaryFont.KKOMI))

@Composable
private fun Font(dark: Boolean, state: FontUiState) = DiaryTheme(darkTheme = dark) { FontSettingsScreen(state, onSelect = {}, onBack = {}) }

@Preview(name = "G1 글꼴 · 라이트", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun FontLight() = Font(dark = false, fontSample)

@Preview(name = "G1 글꼴 · 다크", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun FontDark() = Font(dark = true, fontSample)

@Preview(name = "G1 글꼴 · 받는 중", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun FontDownloading() = Font(dark = false, fontSample.copy(downloading = mapOf(DiaryFont.GAMJA_FLOWER to 0.42f)))
