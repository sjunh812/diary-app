package org.sjhstudio.diary.feature.settings.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.datetime.LocalDate
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.ListPreferences
import org.sjhstudio.diary.core.model.ListStyle
import org.sjhstudio.diary.core.model.PreviewLength
import org.sjhstudio.diary.feature.settings.ListStyleSettingsScreen

// 목록 보기(스펙 008) 프리뷰. 피드 3줄, 카드 전체, Plus 서식, 사진 격자

@Composable
private fun ListStyleSample(list: ListPreferences, canFormat: Boolean = false, dark: Boolean = false) = DiaryTheme(darkTheme = dark) {
    ListStyleSettingsScreen(
        list = list,
        canFormat = canFormat,
        previewStyle = DiaryTheme.typography.body,
        today = LocalDate(2026, 11, 17),
        onStyle = {},
        onLength = {},
        onFormatting = {},
        onPlus = {},
        onBack = {}
    )
}

@Preview(name = "목록 보기 · 피드 3줄 · 라이트", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun FeedThreeLight() = ListStyleSample(ListPreferences())

@Preview(name = "목록 보기 · 피드 3줄 · 다크", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun FeedThreeDark() = ListStyleSample(ListPreferences(), dark = true)

@Preview(name = "목록 보기 · 카드 전체 · 라이트", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun CardFullLight() = ListStyleSample(ListPreferences(listStyle = ListStyle.CARD, previewLength = PreviewLength.FULL))

@Preview(name = "목록 보기 · Plus 서식 켬 · 라이트", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun FormattingLight() = ListStyleSample(ListPreferences(showFormatting = true), canFormat = true)

@Preview(name = "목록 보기 · Plus 서식 켬 · 다크", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun FormattingDark() = ListStyleSample(ListPreferences(showFormatting = true), canFormat = true, dark = true)

@Preview(name = "목록 보기 · 사진 격자 · 라이트", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun GridLight() = ListStyleSample(ListPreferences(listStyle = ListStyle.PHOTO_GRID))
