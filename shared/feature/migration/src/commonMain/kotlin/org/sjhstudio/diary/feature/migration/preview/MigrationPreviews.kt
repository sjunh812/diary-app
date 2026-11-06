package org.sjhstudio.diary.feature.migration.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.migration.LegacyEntryView
import org.sjhstudio.diary.core.migration.LegacyNote
import org.sjhstudio.diary.core.model.MigrationFailureKind
import org.sjhstudio.diary.core.model.MigrationOutcome
import org.sjhstudio.diary.core.model.MigrationReport
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.feature.migration.LegacyReaderScreen
import org.sjhstudio.diary.feature.migration.LegacyReaderState
import org.sjhstudio.diary.feature.migration.MigrationScreen
import org.sjhstudio.diary.feature.migration.MigrationUiState

// 기록 이전 화면(M1~M4) 프리뷰.
// 숫자와 글은 Figma 화면과 같은 견본이다.
// 고쳐 받기 문의 시트는 바텀 시트라 프리뷰 창에 그려지지 않는다.
// 개발 빌드 화면 모음(support_sheet)으로 본다.

private fun report(outcome: MigrationOutcome) = MigrationReport(
    entriesTotal = 1_296, entriesMigrated = 1_284, entriesUnreadable = if (outcome == MigrationOutcome.PARTIAL) 12 else 0,
    photosFound = 1_998, photosMissing = 33, dateDiffers = 2, dateUnparsed = 0, thumbnailsFailed = 0, settingsMigrated = emptyList(),
    finishedAt = Instant.parse("2026-11-01T03:00:00Z"), outcome = outcome
)

@Composable
private fun Screen(state: MigrationUiState, dark: Boolean = false) = DiaryTheme(darkTheme = dark) {
    MigrationScreen(state, onStart = {}, onRetry = {}, onViewLegacy = {}, onExportOriginal = {}, onContact = {})
}

@Preview(name = "M1 옮기는 중", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun Copying() = Screen(MigrationUiState.Copying(entriesDone = 640, entriesTotal = 1_296, photosDone = 980, photosTotal = 2_031))

@Preview(name = "M2 다 옮겼어요 · 라이트", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun DoneLight() = Screen(MigrationUiState.Result(report(MigrationOutcome.DONE)))

@Preview(name = "M2 다 옮겼어요 · 다크", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun DoneDark() = Screen(MigrationUiState.Result(report(MigrationOutcome.DONE)), dark = true)

@Preview(name = "M2-b 일부만", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun Partial() = Screen(MigrationUiState.Result(report(MigrationOutcome.PARTIAL)))

@Preview(name = "M3 옮기지 못했어요", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun Failed() = Screen(MigrationUiState.Failed(MigrationFailureKind.READ_ERROR, shortfallBytes = null))

@Preview(name = "M3 저장 공간 부족", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun NoSpace() = Screen(MigrationUiState.Failed(MigrationFailureKind.NO_SPACE, shortfallBytes = 850L * 1024 * 1024))

@Preview(name = "M2 다 옮겼어요 · 영어", locale = "en", widthDp = 390, heightDp = 844)
@Composable
private fun DoneEnglish() = Screen(MigrationUiState.Result(report(MigrationOutcome.DONE)))

private fun entry(id: Long, day: Int, hour: Int, text: String, mood: Mood?, favorite: Boolean = false) = LegacyEntryView(
    id = id,
    source = LegacyNote(id, null, null, null, null, null, null, null, null, null, null),
    date = LocalDate(2024, 10, day),
    time = LocalTime(hour, 10),
    text = text,
    mood = mood,
    weather = null,
    placeName = "망원동",
    favorite = favorite,
    photos = emptyList()
)

@Preview(name = "M4 예전 일기 보기", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun LegacyReader() = DiaryTheme(darkTheme = false) {
    LegacyReaderScreen(
        state =
            LegacyReaderState(
                entries =
                    listOf(
                        entry(3, 31, 21, "퇴근길에 한강까지 걸었다. 바람이 차가워졌는데 그래서 더 좋았다.", Mood.GOOD, favorite = true),
                        entry(2, 31, 8, "아침 회의가 길었다. 커피 두 잔째인데도 눈이 감긴다.", Mood.TIRED),
                        entry(1, 29, 23, "비 오는 소리 들으면서 책 한 챕터. 오늘은 그걸로 충분했다.", Mood.COOL)
                    ),
                loading = false,
                end = true
            ),
        currentYear = 2024,
        onBack = {},
        onLoadMore = {}
    )
}

@Preview(name = "M4 예전 일기 · 읽기 실패", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun LegacyReaderFailed() = DiaryTheme(darkTheme = false) {
    LegacyReaderScreen(
        state = LegacyReaderState(loading = false, failed = true),
        currentYear = 2024,
        onBack = {},
        onLoadMore = {}
    )
}
