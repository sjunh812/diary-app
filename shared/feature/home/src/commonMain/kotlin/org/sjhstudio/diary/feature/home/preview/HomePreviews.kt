package org.sjhstudio.diary.feature.home.preview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.YearMonth
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.EntryRow
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.Weather
import org.sjhstudio.diary.core.ui.DiaryMonthSheet
import org.sjhstudio.diary.feature.home.HomeScreen

private val Today = LocalDate(2026, 10, 31)

private val Rows =
    listOf(
        EntryRow("1", Today, LocalTime(21, 40), "퇴근길에 한강까지 걸었다. 바람이 차가웠는데 그래서 더 좋았다.", Mood.GOOD, Weather.SUNNY, "망원동", true, listOf(null)),
        EntryRow("2", Today, LocalTime(8, 10), "아침 회의가 길었다. 커피 두 잔째인데도 눈이 감긴다.", Mood.TIRED, Weather.CLOUDY, null, false, emptyList()),
        EntryRow("3", LocalDate(2026, 9, 30), LocalTime(20, 30), "제주 마지막 날.", Mood.COOL, Weather.SUNNY, "제주 애월", false, emptyList())
    )

@Composable
private fun Home(dark: Boolean, rows: List<EntryRow> = Rows, favoritesOnly: Boolean = false) = DiaryTheme(darkTheme = dark) {
    HomeScreen(
        title = "2026년 10월",
        rows = flowOf(PagingData.from(rows)).collectAsLazyPagingItems(),
        list = rememberLazyListState(),
        favoritesOnly = favoritesOnly,
        selectedId = null,
        today = Today,
        previewStyle = DiaryTheme.typography.body,
        onToggleFavorites = {},
        onPickMonth = {},
        onOpen = {},
        onWrite = {}
    )
}

@Preview(name = "홈 · 라이트", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun HomeLight() = Home(dark = false)

@Preview(name = "홈 · 다크", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun HomeDark() = Home(dark = true)

@Preview(name = "홈 · 빈 상태", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun HomeEmpty() = Home(dark = false, rows = emptyList())

@Preview(name = "홈 · 즐겨찾기 없음", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun HomeNoFavorites() = Home(dark = true, rows = emptyList(), favoritesOnly = true)

@Preview(name = "연·월 고르기", locale = "ko", widthDp = 390, heightDp = 500)
@Composable
private fun MonthPicker() = DiaryTheme(darkTheme = false) {
    Box {
        DiaryMonthSheet(
            title = "날짜로 가기",
            initialYear = 2026,
            shown = YearMonth(2026, 10),
            hasYear = { it == 2026 },
            enabled = { it == YearMonth(2026, 9) || it == YearMonth(2026, 10) },
            onPick = {},
            onDismiss = {}
        )
    }
}
