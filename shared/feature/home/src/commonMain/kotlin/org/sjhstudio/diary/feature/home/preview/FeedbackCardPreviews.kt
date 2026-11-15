package org.sjhstudio.diary.feature.home.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.feature.home.FeedbackCard

@Composable
private fun Card(dark: Boolean) = DiaryTheme(darkTheme = dark) {
    Box(Modifier.background(DiaryTheme.colors.canvas).padding(horizontal = DiarySpacing.x5)) {
        FeedbackCard(onSend = {}, onClose = {})
    }
}

@Preview(name = "의견 카드 · 라이트", locale = "ko", widthDp = 390)
@Composable
private fun FeedbackCardLight() = Card(dark = false)

@Preview(name = "의견 카드 · 다크", locale = "ko", widthDp = 390)
@Composable
private fun FeedbackCardDark() = Card(dark = true)

@Preview(name = "의견 카드 · 영어", locale = "en", widthDp = 390)
@Composable
private fun FeedbackCardEnglish() = Card(dark = false)

@Preview(name = "의견 카드 · 큰 글자", locale = "ko", widthDp = 390, fontScale = 2f)
@Composable
private fun FeedbackCardLargeText() = Card(dark = false)
