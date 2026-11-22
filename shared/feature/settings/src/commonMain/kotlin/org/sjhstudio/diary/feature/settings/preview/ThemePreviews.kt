package org.sjhstudio.diary.feature.settings.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.ListStyle
import org.sjhstudio.diary.core.model.ThemeChoice
import org.sjhstudio.diary.core.model.ThemeMode
import org.sjhstudio.diary.core.ui.rememberThemeBrand
import org.sjhstudio.diary.feature.settings.ThemeSettingsScreen

// 테마(스펙 010, Figma "프로토타입 · 테마 색") 프리뷰. 한국어 먼저, 라이트·다크와 상태마다

@Composable
private fun Theme(
    dark: Boolean,
    mode: ThemeMode = ThemeMode.SYSTEM,
    theme: ThemeChoice = ThemeChoice(),
    canCustom: Boolean? = false,
    listStyle: ListStyle = ListStyle.FEED
) = DiaryTheme(darkTheme = dark, brand = rememberThemeBrand(theme, customAllowed = canCustom == true)) {
    ThemeSettingsScreen(
        mode = mode,
        theme = theme,
        listStyle = listStyle,
        canCustom = canCustom,
        onMode = {},
        onColor = {},
        onCustom = {},
        onPlus = {},
        onBack = {}
    )
}

@Preview(name = "테마 · 처음(라벤더, 시스템 설정) · 라이트", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun FirstLight() = Theme(dark = false)

// 미리보기는 기기가 늘 라이트라 시스템 설정을 따르면 다크 견본이 그려지지 않아 다크를 고름
@Preview(name = "테마 · 라벤더, 다크 고름", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun FirstDark() = Theme(dark = true, mode = ThemeMode.DARK)

@Preview(name = "테마 · 바다, 라이트 고름", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun SeaLight() = Theme(dark = false, mode = ThemeMode.LIGHT, theme = ThemeChoice(colorId = "sea"))

@Preview(name = "테마 · 단풍, 다크 고름, 카드", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun MapleDarkCard() = Theme(dark = true, mode = ThemeMode.DARK, theme = ThemeChoice(colorId = "maple"), listStyle = ListStyle.CARD)

@Preview(name = "테마 · 흑백, 다크, 사진 격자", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun BlackDarkGrid() =
    Theme(dark = true, mode = ThemeMode.DARK, theme = ThemeChoice(colorId = "black"), listStyle = ListStyle.PHOTO_GRID)

@Preview(name = "테마 · Plus 직접 고른 색", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun CustomPlus() = Theme(dark = false, theme = ThemeChoice(colorId = "custom", customHex = "#C2185B"), canCustom = true)

@Preview(name = "테마 · 무료(직접 고르기 칸에 Plus 칩)", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun FreeCustomLocked() = Theme(dark = false, canCustom = false)

@Preview(name = "테마 · 요금제 모름", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun PlanUnknown() = Theme(dark = false, canCustom = null)

@Preview(name = "테마 · Plus 직접 고른 색 · 다크", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun CustomPlusDark() = Theme(dark = true, theme = ThemeChoice(colorId = "custom", customHex = "#C2185B"), canCustom = true)

@Preview(name = "테마 · Plus 직접 고른 밝은 색(#C8E9FF)", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun CustomPlusPale() = Theme(dark = false, theme = ThemeChoice(colorId = "custom", customHex = "#C8E9FF"), canCustom = true)
