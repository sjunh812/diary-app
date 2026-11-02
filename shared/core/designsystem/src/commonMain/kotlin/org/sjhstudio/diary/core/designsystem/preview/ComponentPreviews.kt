package org.sjhstudio.diary.core.designsystem.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.sjhstudio.diary.core.designsystem.catalog.CatalogBoxes
import org.sjhstudio.diary.core.designsystem.catalog.CatalogButtons
import org.sjhstudio.diary.core.designsystem.catalog.CatalogColors
import org.sjhstudio.diary.core.designsystem.catalog.CatalogEntries
import org.sjhstudio.diary.core.designsystem.catalog.CatalogKeypad
import org.sjhstudio.diary.core.designsystem.catalog.CatalogSettingsRows
import org.sjhstudio.diary.core.designsystem.catalog.CatalogSpacing
import org.sjhstudio.diary.core.designsystem.catalog.CatalogTypography
import org.sjhstudio.diary.core.designsystem.catalog.DesignCatalogScreen
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme

// 디자인 시스템 토큰과 컴포넌트 프리뷰. 묶음은 디자인 견본 화면(`catalog/DesignCatalogScreen.kt`)과 같다.
// 견본 글은 Figma 화면과 같은 지어낸 글이고 화면 문구가 아니다(문구는 각 기능의 문자열 리소스).
// 한국어를 먼저 보고, 라이트와 다크를 나란히 둔다.

/** 프리뷰 바탕. 폰 폭 390에 화면 바탕색을 깐다. 다른 모듈의 프리뷰도 쓴다. */
@Composable
fun PreviewCanvas(dark: Boolean, content: @Composable () -> Unit) {
    DiaryTheme(darkTheme = dark) {
        Column(Modifier.width(390.dp).background(DiaryTheme.colors.canvas).padding(vertical = 16.dp)) { content() }
    }
}

@Preview(name = "색 · 라이트", locale = "ko")
@Composable
private fun ColorsLight() = PreviewCanvas(dark = false) { CatalogColors() }

@Preview(name = "색 · 다크", locale = "ko")
@Composable
private fun ColorsDark() = PreviewCanvas(dark = true) { CatalogColors() }

@Preview(name = "글자 · 라이트", locale = "ko")
@Composable
private fun TypographyLight() = PreviewCanvas(dark = false) { CatalogTypography() }

@Preview(name = "글자 · 다크", locale = "ko")
@Composable
private fun TypographyDark() = PreviewCanvas(dark = true) { CatalogTypography() }

@Preview(name = "간격과 모서리 · 라이트", locale = "ko")
@Composable
private fun SpacingLight() = PreviewCanvas(dark = false) { CatalogSpacing() }

@Preview(name = "간격과 모서리 · 다크", locale = "ko")
@Composable
private fun SpacingDark() = PreviewCanvas(dark = true) { CatalogSpacing() }

@Preview(name = "버튼과 칩 · 라이트", locale = "ko")
@Composable
private fun ButtonsLight() = PreviewCanvas(dark = false) { CatalogButtons() }

@Preview(name = "버튼과 칩 · 다크", locale = "ko")
@Composable
private fun ButtonsDark() = PreviewCanvas(dark = true) { CatalogButtons() }

@Preview(name = "설정 줄 · 라이트", locale = "ko")
@Composable
private fun SettingsRowsLight() = PreviewCanvas(dark = false) { CatalogSettingsRows() }

@Preview(name = "설정 줄 · 다크", locale = "ko")
@Composable
private fun SettingsRowsDark() = PreviewCanvas(dark = true) { CatalogSettingsRows() }

@Preview(name = "상자와 배지 · 라이트", locale = "ko")
@Composable
private fun BoxesLight() = PreviewCanvas(dark = false) { CatalogBoxes() }

@Preview(name = "상자와 배지 · 다크", locale = "ko")
@Composable
private fun BoxesDark() = PreviewCanvas(dark = true) { CatalogBoxes() }

@Preview(name = "일기 줄과 빈 상태 · 라이트", locale = "ko")
@Composable
private fun EntriesLight() = PreviewCanvas(dark = false) { CatalogEntries() }

@Preview(name = "일기 줄과 빈 상태 · 다크", locale = "ko")
@Composable
private fun EntriesDark() = PreviewCanvas(dark = true) { CatalogEntries() }

@Preview(name = "숫자판과 점 · 라이트", locale = "ko")
@Composable
private fun KeypadLight() = PreviewCanvas(dark = false) { CatalogKeypad() }

@Preview(name = "숫자판과 점 · 다크", locale = "ko")
@Composable
private fun KeypadDark() = PreviewCanvas(dark = true) { CatalogKeypad() }

@Preview(name = "디자인 견본 화면 · 라이트", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun CatalogScreenLight() = DiaryTheme(darkTheme = false) { DesignCatalogScreen(onBack = {}) }

@Preview(name = "디자인 견본 화면 · 다크", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun CatalogScreenDark() = DiaryTheme(darkTheme = true) { DesignCatalogScreen(onBack = {}) }
