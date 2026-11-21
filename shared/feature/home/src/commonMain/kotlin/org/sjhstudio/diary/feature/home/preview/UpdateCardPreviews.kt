package org.sjhstudio.diary.feature.home.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.ReleaseNote
import org.sjhstudio.diary.core.model.UpdateNotice
import org.sjhstudio.diary.feature.home.UpdateCard

private val note = ReleaseNote(mapOf("ko" to "결제 뒤에도 광고가 보이던 문제를 고쳤어요.", "en" to "Fixed ads showing after purchase."))

@Composable
private fun Card(notice: UpdateNotice, dark: Boolean = false) = DiaryTheme(darkTheme = dark) {
    Box(Modifier.background(DiaryTheme.colors.canvas).padding(horizontal = DiarySpacing.x5)) {
        UpdateCard(notice, onUpdate = {}, onLater = {}, onRestart = {})
    }
}

@Preview(name = "새 버전 카드 · 라이트", locale = "ko", widthDp = 390)
@Composable
private fun UpdateCardLight() = Card(UpdateNotice.Available(required = false, note = note))

@Preview(name = "새 버전 카드 · 다크", locale = "ko", widthDp = 390)
@Composable
private fun UpdateCardDark() = Card(UpdateNotice.Available(required = false, note = note), dark = true)

@Preview(name = "새 버전 카드 · 소식 없음", locale = "ko", widthDp = 390)
@Composable
private fun UpdateCardNoNote() = Card(UpdateNotice.Available(required = false, note = null))

@Preview(name = "새 버전 카드 · 꼭 필요", locale = "ko", widthDp = 390)
@Composable
private fun UpdateCardRequired() = Card(UpdateNotice.Available(required = true, note = note))

@Preview(name = "새 버전 카드 · 받는 중", locale = "ko", widthDp = 390)
@Composable
private fun UpdateCardDownloading() = Card(UpdateNotice.Downloading(0.42f))

@Preview(name = "새 버전 카드 · 받는 중 · 다크", locale = "ko", widthDp = 390)
@Composable
private fun UpdateCardDownloadingDark() = Card(UpdateNotice.Downloading(0.42f), dark = true)

@Preview(name = "새 버전 카드 · 받았어요", locale = "ko", widthDp = 390)
@Composable
private fun UpdateCardDownloaded() = Card(UpdateNotice.Downloaded())

@Preview(name = "새 버전 카드 · 받았어요 · 꼭 필요", locale = "ko", widthDp = 390)
@Composable
private fun UpdateCardDownloadedRequired() = Card(UpdateNotice.Downloaded(required = true))

@Preview(name = "새 버전 카드 · 받기 실패", locale = "ko", widthDp = 390)
@Composable
private fun UpdateCardFailed() = Card(UpdateNotice.Available(required = false, note = note, failed = true))

@Preview(name = "새 버전 카드 · 영어", locale = "en", widthDp = 390)
@Composable
private fun UpdateCardEnglish() = Card(UpdateNotice.Available(required = false, note = note))

@Preview(name = "새 버전 카드 · 큰 글자", locale = "ko", widthDp = 390, fontScale = 2f)
@Composable
private fun UpdateCardLargeText() = Card(UpdateNotice.Available(required = false, note = note))
