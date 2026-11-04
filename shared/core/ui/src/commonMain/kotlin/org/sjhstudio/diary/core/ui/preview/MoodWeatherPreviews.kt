package org.sjhstudio.diary.core.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.Weather
import org.sjhstudio.diary.core.ui.MoodFace
import org.sjhstudio.diary.core.ui.WeatherIcon

@Composable
private fun Sample(dark: Boolean) = DiaryTheme(darkTheme = dark) {
    Column(
        Modifier.background(DiaryTheme.colors.canvas).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        for (size in listOf(44, 30, 18)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Mood.entries.chunked(5).first().forEach { MoodFace(it, size.dp) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Mood.entries.drop(5).forEach { MoodFace(it, size.dp) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Weather.entries.forEach { WeatherIcon(it, 24.dp, DiaryTheme.colors.textPrimary) }
        }
    }
}

@Preview(name = "기분·날씨 · 라이트", locale = "ko")
@Composable
private fun Light() = Sample(dark = false)

@Preview(name = "기분·날씨 · 다크", locale = "ko")
@Composable
private fun Dark() = Sample(dark = true)
