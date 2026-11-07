package org.sjhstudio.diary.core.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.datetime.YearMonth
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.ui.DiaryMonthDays
import org.sjhstudio.diary.core.ui.DiaryMonthPicker

@Composable
private fun Sample(dark: Boolean) = DiaryTheme(darkTheme = dark) {
    Box(Modifier.background(DiaryTheme.colors.surface).padding(DiarySpacing.x6)) {
        val month = YearMonth(2026, 7)
        DiaryMonthPicker(month, {}) { DiaryMonthDays(month, isPicked = { false }, onPick = {}) }
    }
}

@Preview(name = "달력 시트 · 라이트", locale = "ko")
@Composable
private fun MonthPickerLight() = Sample(dark = false)

@Preview(name = "달력 시트 · 다크", locale = "ko")
@Composable
private fun MonthPickerDark() = Sample(dark = true)
