package org.sjhstudio.diary.feature.write.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.Weather
import org.sjhstudio.diary.feature.write.MoodGrid
import org.sjhstudio.diary.feature.write.WeatherGrid

@Composable
private fun Grids(dark: Boolean) = DiaryTheme(darkTheme = dark) {
    Column(
        Modifier.background(DiaryTheme.colors.surface).padding(DiarySpacing.x6),
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x5)
    ) {
        MoodGrid(Mood.GOOD, onSelect = {})
        WeatherGrid(Weather.SUNNY, onSelect = {})
    }
}

@Preview(name = "기분·날씨 칸 · 라이트", locale = "ko", widthDp = 370)
@Composable
private fun GridsLight() = Grids(dark = false)

@Preview(name = "기분·날씨 칸 · 다크", locale = "ko", widthDp = 370)
@Composable
private fun GridsDark() = Grids(dark = true)
