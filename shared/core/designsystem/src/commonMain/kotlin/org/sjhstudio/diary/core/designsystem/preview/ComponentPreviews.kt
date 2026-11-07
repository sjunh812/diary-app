package org.sjhstudio.diary.core.designsystem.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import org.sjhstudio.diary.core.designsystem.catalog.CatalogTimeWheel
import org.sjhstudio.diary.core.designsystem.catalog.CatalogTypography
import org.sjhstudio.diary.core.designsystem.catalog.DesignCatalogScreen
import org.sjhstudio.diary.core.designsystem.component.DiaryBadgeTone
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonPair
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonSize
import org.sjhstudio.diary.core.designsystem.component.DiaryEmptyLine
import org.sjhstudio.diary.core.designsystem.component.DiaryIconButton
import org.sjhstudio.diary.core.designsystem.component.DiaryInviteCard
import org.sjhstudio.diary.core.designsystem.component.DiaryLoadingDots
import org.sjhstudio.diary.core.designsystem.component.DiaryResultHeader
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme

// 디자인 시스템 토큰과 컴포넌트 프리뷰.
// 묶음은 디자인 견본 화면(`catalog/DesignCatalogScreen.kt`)과 같다.
// 견본 글은 Figma 화면과 같은 지어낸 글이고 화면 문구가 아니다(문구는 각 기능의 문자열 리소스).
// 한국어를 먼저 보고, 라이트와 다크를 나란히 둔다.

/**
 * 프리뷰 바탕.
 * 폰 폭 390에 화면 바탕색을 깐다.
 * 다른 모듈의 프리뷰도 쓴다.
 */
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

@Preview(name = "두 버튼 · 라이트", locale = "ko", widthDp = 360)
@Composable
private fun ButtonPairLight() = PreviewCanvas(dark = false) { ButtonPairs() }

@Preview(name = "두 버튼 · 다크", locale = "ko", widthDp = 360)
@Composable
private fun ButtonPairDark() = PreviewCanvas(dark = true) { ButtonPairs() }

/** 글이 반 칸에 들어가면 나란히, 넘치면 위아래로 쌓인다. */
@Composable
private fun ButtonPairs() = Column(verticalArrangement = Arrangement.spacedBy(DiarySpacing.x6)) {
    DiaryButtonPair("괜찮아요", {}, "광고 없애기", {}, size = DiaryButtonSize.XLarge)
    DiaryButtonPair("괜찮아요", {}, "한 번에 다 담기지 않는 아주 긴 버튼 글이에요", {}, size = DiaryButtonSize.XLarge)
}

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

@Preview(name = "상태 상자와 시각 휠 · 라이트", locale = "ko")
@Composable
private fun TimeWheelLight() = PreviewCanvas(dark = false) { CatalogTimeWheel() }

@Preview(name = "상태 상자와 시각 휠 · 다크", locale = "ko")
@Composable
private fun TimeWheelDark() = PreviewCanvas(dark = true) { CatalogTimeWheel() }

@Preview(name = "일기 줄과 빈 상태 · 라이트", locale = "ko")
@Composable
private fun EntriesLight() = PreviewCanvas(dark = false) { CatalogEntries() }

@Preview(name = "일기 줄과 빈 상태 · 다크", locale = "ko")
@Composable
private fun EntriesDark() = PreviewCanvas(dark = true) { CatalogEntries() }

@Preview(name = "빈 칸 한 줄 · 라이트", locale = "ko", widthDp = 390)
@Composable
private fun EmptyLineLight() = PreviewCanvas(dark = false) { DiaryEmptyLine("이 기간에 쓴 일기가 없어요", "일기 쓰기", {}) }

@Preview(name = "빈 칸 한 줄 · 다크", locale = "ko", widthDp = 390)
@Composable
private fun EmptyLineDark() = PreviewCanvas(dark = true) { DiaryEmptyLine("이 기간에 쓴 일기가 없어요", "일기 쓰기", {}) }

@Preview(name = "아이콘 버튼 · 라이트", locale = "ko")
@Composable
private fun IconButtonsLight() = PreviewCanvas(dark = false) { IconButtons() }

@Preview(name = "아이콘 버튼 · 다크", locale = "ko")
@Composable
private fun IconButtonsDark() = PreviewCanvas(dark = true) { IconButtons() }

/** 기본, 색을 넘긴 버튼, 누를 수 없는 버튼. */
@Composable
private fun IconButtons() = Row(
    Modifier.padding(horizontal = DiarySpacing.screenCompact),
    horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2)
) {
    DiaryIconButton(DiaryIcons.Search, "찾기", onClick = {})
    DiaryIconButton(DiaryIcons.Trash, "삭제", onClick = {}, tint = DiaryTheme.colors.danger)
    DiaryIconButton(DiaryIcons.Share, "공유", onClick = {}, enabled = false)
}

@Preview(name = "결과 머리 · 라이트", locale = "ko")
@Composable
private fun ResultHeadersLight() = PreviewCanvas(dark = false) { ResultHeaders() }

@Preview(name = "결과 머리 · 다크", locale = "ko")
@Composable
private fun ResultHeadersDark() = PreviewCanvas(dark = true) { ResultHeaders() }

/** 끝난 결과, 문제가 있는 결과, 설명이 없는 머리. */
@Composable
private fun ResultHeaders() = Column {
    DiaryResultHeader(DiaryIcons.CircleCheck, "백업을 마쳤어요", description = "일기 1,284편과 사진 320장을 담았어요.")
    DiaryResultHeader(
        DiaryIcons.CircleAlert,
        "드라이브 공간이 부족해요",
        description = "공간을 비우고 다시 시도해 주세요.",
        tone = DiaryBadgeTone.Danger
    )
    DiaryResultHeader(DiaryIcons.Lock, "비밀번호를 입력해 주세요")
}

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

@Preview(name = "기다림 점 · 라이트", locale = "ko")
@Composable
private fun LoadingDotsLight() = PreviewCanvas(dark = false) { DiaryLoadingDots(DiaryTheme.colors.textSecondary, animated = false) }

@Preview(name = "기다림 점 · 다크", locale = "ko")
@Composable
private fun LoadingDotsDark() = PreviewCanvas(dark = true) { DiaryLoadingDots(DiaryTheme.colors.textSecondary, animated = false) }

@Preview(name = "초대 카드 · 라이트", locale = "ko")
@Composable
private fun InviteCardLight() = PreviewCanvas(dark = false) {
    DiaryInviteCard("광고 없이, 더 편하게 써요", "일기마다 사진 30장, 체크리스트, 고급 통계까지 쓸 수 있어요.", "Plus 알아보기", {})
}

@Preview(name = "초대 카드 · 다크", locale = "ko")
@Composable
private fun InviteCardDark() = PreviewCanvas(dark = true) {
    DiaryInviteCard("광고 없이, 더 편하게 써요", "일기마다 사진 30장, 체크리스트, 고급 통계까지 쓸 수 있어요.", "Plus 알아보기", {})
}
