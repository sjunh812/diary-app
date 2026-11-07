package org.sjhstudio.diary.core.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.ui.SkeletonEntryCards
import org.sjhstudio.diary.core.ui.SkeletonEntryRows
import org.sjhstudio.diary.core.ui.SkeletonPhotoGrid

@Composable
private fun Sample(dark: Boolean, content: @Composable () -> Unit) = DiaryTheme(darkTheme = dark) {
    Box(Modifier.fillMaxSize().background(DiaryTheme.colors.canvas)) { content() }
}

@Preview(name = "스켈레톤 · 피드 · 라이트", locale = "ko", widthDp = 390, heightDp = 480)
@Composable
private fun FeedLight() = Sample(dark = false) { SkeletonEntryRows() }

@Preview(name = "스켈레톤 · 피드 · 다크", locale = "ko", widthDp = 390, heightDp = 480)
@Composable
private fun FeedDark() = Sample(dark = true) { SkeletonEntryRows() }

@Preview(name = "스켈레톤 · 카드 · 라이트", locale = "ko", widthDp = 390, heightDp = 480)
@Composable
private fun CardLight() = Sample(dark = false) { SkeletonEntryCards() }

@Preview(name = "스켈레톤 · 카드 · 다크", locale = "ko", widthDp = 390, heightDp = 480)
@Composable
private fun CardDark() = Sample(dark = true) { SkeletonEntryCards() }

@Preview(name = "스켈레톤 · 사진 격자 · 라이트", locale = "ko", widthDp = 390, heightDp = 480)
@Composable
private fun GridLight() = Sample(dark = false) { SkeletonPhotoGrid() }

@Preview(name = "스켈레톤 · 사진 격자 · 다크", locale = "ko", widthDp = 390, heightDp = 480)
@Composable
private fun GridDark() = Sample(dark = true) { SkeletonPhotoGrid() }
