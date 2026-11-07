package org.sjhstudio.diary.core.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.ui.DiaryTitleButton

@Composable
private fun Sample(dark: Boolean) = DiaryTheme(darkTheme = dark) {
    Box(Modifier.fillMaxWidth().background(DiaryTheme.colors.canvas).padding(DiarySpacing.screenCompact)) {
        DiaryTitleButton("2026년 10월", onClick = {})
    }
}

@Preview(name = "큰 제목 버튼 · 라이트", locale = "ko", widthDp = 390)
@Composable
private fun TitleLight() = Sample(dark = false)

@Preview(name = "큰 제목 버튼 · 다크", locale = "ko", widthDp = 390)
@Composable
private fun TitleDark() = Sample(dark = true)
