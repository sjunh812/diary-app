package org.sjhstudio.diary.feature.calendar.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.DayMood
import org.sjhstudio.diary.core.model.EntryRow
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.ui.rememberDiaryDateFormat
import org.sjhstudio.diary.feature.calendar.CalendarPanel
import org.sjhstudio.diary.feature.calendar.DayPanel
import org.sjhstudio.diary.feature.calendar.pageIndex

private val Today = LocalDate(2026, 10, 31)
private val Shown = YearMonth(2026, 10)
private val Days =
    mapOf(
        LocalDate(2026, 10, 1) to DayMood(LocalDate(2026, 10, 1), Mood.COOL, 1),
        LocalDate(2026, 10, 12) to DayMood(LocalDate(2026, 10, 12), null, 1),
        LocalDate(2026, 10, 13) to DayMood(LocalDate(2026, 10, 13), null, 2),
        Today to DayMood(Today, Mood.GOOD, 2, Mood.COOL)
    )

@Composable
private fun Calendar(dark: Boolean, rows: List<EntryRow>? = emptyList()) = DiaryTheme(darkTheme = dark) {
    val dates = rememberDiaryDateFormat()
    Column(Modifier.background(DiaryTheme.colors.canvas)) {
        CalendarPanel(
            shown = Shown,
            pager = rememberPagerState(initialPage = Shown.pageIndex()) { Shown.pageIndex() + 1 },
            twoPane = false,
            today = Today,
            selected = LocalDate(2026, 10, 26),
            lastMonth = Shown,
            showToday = true,
            dates = dates,
            moods = { MutableStateFlow(Days) },
            onSelect = {},
            onToday = {},
            onPick = {},
            onMove = {}
        )
        DayPanel(LocalDate(2026, 10, 26), rows, dates, DiaryTheme.typography.body, false, {}, {}, {})
    }
}

@Preview(name = "달력 · 읽는 중 · 라이트", locale = "ko", widthDp = 390, heightDp = 700)
@Composable
private fun CalendarLoading() = Calendar(dark = false, rows = null)

@Preview(name = "달력 · 빈 날 · 라이트", locale = "ko", widthDp = 390, heightDp = 700)
@Composable
private fun CalendarLight() = Calendar(dark = false)

@Preview(name = "달력 · 빈 날 · 다크", locale = "ko", widthDp = 390, heightDp = 700)
@Composable
private fun CalendarDark() = Calendar(dark = true)
