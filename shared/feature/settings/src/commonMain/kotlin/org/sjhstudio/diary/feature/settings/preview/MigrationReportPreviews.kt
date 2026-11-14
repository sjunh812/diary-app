package org.sjhstudio.diary.feature.settings.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.MigrationOutcome
import org.sjhstudio.diary.core.model.MigrationReport
import org.sjhstudio.diary.feature.settings.MigrationReportScreen
import org.sjhstudio.diary.feature.settings.MigrationReportUiState

// 설정 > 기록 이전 결과(M5) 프리뷰
// 숫자는 Figma M5 · 일부만과 같음

private val sample =
    MigrationReportUiState(
        report =
            MigrationReport(
                entriesTotal = 1_296, entriesMigrated = 1_284, entriesUnreadable = 12, photosFound = 1_998, photosMissing = 33,
                dateDiffers = 2, dateUnparsed = 0, thumbnailsFailed = 0, settingsMigrated = emptyList(),
                finishedAt = Instant.parse("2026-11-01T03:00:00Z"), outcome = MigrationOutcome.PARTIAL
            ),
        migratedOn = LocalDate(2026, 11, 1)
    )

@Preview(name = "기록 이전 결과 · 라이트", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun MigrationReportLight() = DiaryTheme(darkTheme = false) {
    MigrationReportScreen(sample, onBack = {}, onExportOriginal = {}, onContact = {})
}

@Preview(name = "기록 이전 결과 · 다크", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun MigrationReportDark() = DiaryTheme(darkTheme = true) {
    MigrationReportScreen(sample, onBack = {}, onExportOriginal = {}, onContact = {})
}

@Preview(name = "기록 이전 결과 · 영어", locale = "en", widthDp = 390, heightDp = 844)
@Composable
private fun MigrationReportEnglish() = DiaryTheme(darkTheme = false) {
    MigrationReportScreen(sample, onBack = {}, onExportOriginal = {}, onContact = {})
}
