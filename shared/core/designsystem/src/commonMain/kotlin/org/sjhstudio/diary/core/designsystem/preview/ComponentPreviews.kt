package org.sjhstudio.diary.core.designsystem.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import org.sjhstudio.diary.core.designsystem.component.DiaryColorPicker
import org.sjhstudio.diary.core.designsystem.component.DiaryDateSlot
import org.sjhstudio.diary.core.designsystem.component.DiaryDateSlots
import org.sjhstudio.diary.core.designsystem.component.DiaryEmptyLine
import org.sjhstudio.diary.core.designsystem.component.DiaryIconButton
import org.sjhstudio.diary.core.designsystem.component.DiaryInviteCard
import org.sjhstudio.diary.core.designsystem.component.DiaryLoadingDots
import org.sjhstudio.diary.core.designsystem.component.DiaryPlusChip
import org.sjhstudio.diary.core.designsystem.component.DiaryResultHeader
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsSwitchRow
import org.sjhstudio.diary.core.designsystem.component.DiarySheetChoiceRow
import org.sjhstudio.diary.core.designsystem.component.DiaryThemeModeOption
import org.sjhstudio.diary.core.designsystem.component.DiaryThemeSwatch
import org.sjhstudio.diary.core.designsystem.component.DiaryTopBarTextButton
import org.sjhstudio.diary.core.designsystem.component.MiniHomeLayout
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DarkDiaryColors
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.LightDiaryColors

// 디자인 시스템 토큰과 컴포넌트 프리뷰
// 묶음은 디자인 견본 화면(`catalog/DesignCatalogScreen.kt`)과 같음
// 견본 글은 Figma 화면과 같은 지어낸 글이고 화면 문구 아님(문구는 각 기능의 문자열 리소스)
// 한국어를 먼저 보고, 라이트와 다크를 나란히

/**
 * 프리뷰 바탕. 폰 폭 390에 화면 바탕색을 깖
 * 다른 모듈의 프리뷰도 씀
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

/** 글이 반 칸에 들어가면 나란히, 넘치면 위아래로 쌓임 */
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

@Preview(name = "빈칸 한 줄 · 라이트", locale = "ko", widthDp = 390)
@Composable
private fun EmptyLineLight() = PreviewCanvas(dark = false) { DiaryEmptyLine("이 기간에 쓴 일기가 없어요", "일기 쓰기", {}) }

@Preview(name = "빈칸 한 줄 · 다크", locale = "ko", widthDp = 390)
@Composable
private fun EmptyLineDark() = PreviewCanvas(dark = true) { DiaryEmptyLine("이 기간에 쓴 일기가 없어요", "일기 쓰기", {}) }

@Preview(name = "아이콘 버튼 · 라이트", locale = "ko")
@Composable
private fun IconButtonsLight() = PreviewCanvas(dark = false) { IconButtons() }

@Preview(name = "아이콘 버튼 · 다크", locale = "ko")
@Composable
private fun IconButtonsDark() = PreviewCanvas(dark = true) { IconButtons() }

/** 기본, 색을 넘긴 버튼, 누를 수 없는 버튼 */
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

/** 끝난 결과, 문제가 있는 결과, 설명이 없는 머리 */
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

@Preview(name = "위 줄 글자 버튼 · 기다리는 중 · 라이트", locale = "ko")
@Composable
private fun TopBarTextButtonLoadingLight() = PreviewCanvas(dark = false) { TopBarTextButtons() }

@Preview(name = "위 줄 글자 버튼 · 기다리는 중 · 다크", locale = "ko")
@Composable
private fun TopBarTextButtonLoadingDark() = PreviewCanvas(dark = true) { TopBarTextButtons() }

@Composable
private fun TopBarTextButtons() = Row {
    DiaryTopBarTextButton("저장", onClick = {})
    DiaryTopBarTextButton("저장", onClick = {}, enabled = false)
    DiaryTopBarTextButton("저장", onClick = {}, enabled = false, loading = true)
}

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

@Composable
private fun DateSlotStates() {
    Column(verticalArrangement = Arrangement.spacedBy(DiarySpacing.x4)) {
        DiaryDateSlots("시작일", null, "종료일", null, "날짜 선택", DiaryDateSlot.Start, {}, null)
        DiaryDateSlots("시작일", "11월 3일", "종료일", null, "날짜 선택", DiaryDateSlot.End, {}, {})
        DiaryDateSlots("시작일", "11월 3일", "종료일", "11월 9일", "날짜 선택", null, {}, {})
    }
}

@Preview(name = "기간 두 칸 · 라이트", locale = "ko", widthDp = 360)
@Composable
private fun DateSlotsLight() = PreviewCanvas(dark = false) { DateSlotStates() }

@Preview(name = "기간 두 칸 · 다크", locale = "ko", widthDp = 360)
@Composable
private fun DateSlotsDark() = PreviewCanvas(dark = true) { DateSlotStates() }

@Composable
private fun SheetChoiceRows() = Column {
    DiarySheetChoiceRow("3줄", selected = false, onClick = {})
    DiarySheetChoiceRow("5줄", selected = true, onClick = {})
    DiarySheetChoiceRow("직접 선택", selected = false, onClick = {}, description = "11월 3일 ~ 11월 9일")
}

@Preview(name = "시트 고르기 줄 · 라이트", locale = "ko", widthDp = 360)
@Composable
private fun SheetChoiceRowsLight() = PreviewCanvas(dark = false) { SheetChoiceRows() }

@Preview(name = "시트 고르기 줄 · 다크", locale = "ko", widthDp = 360)
@Composable
private fun SheetChoiceRowsDark() = PreviewCanvas(dark = true) { SheetChoiceRows() }

@Composable
private fun PlusChipRows() = Column {
    DiarySettingsSwitchRow("서식 그대로 보기", checked = false, onCheckedChange = {
    }, description = "굵은 글씨, 밑줄, 글자 색, 형광펜이 목록에도 보여요.", badge = "Plus")
    DiarySettingsSwitchRow("아주 긴 설정 이름이 두 줄까지 넘어가도 칩은 밀려나지 않아요", checked = false, onCheckedChange = {}, badge = "Plus")
    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DiaryPlusChip("Plus")
        DiaryPlusChip("Plus", separator = DiaryTheme.colors.accent)
    }
}

@Preview(name = "Plus 칩 · 라이트", locale = "ko", widthDp = 360)
@Composable
private fun PlusChipLight() = PreviewCanvas(dark = false) { PlusChipRows() }

@Preview(name = "Plus 칩 · 다크", locale = "ko", widthDp = 360)
@Composable
private fun PlusChipDark() = PreviewCanvas(dark = true) { PlusChipRows() }

@Composable
private fun ColorPickerSample() = Column(Modifier.padding(16.dp)) {
    DiaryColorPicker(initial = 0x46A6FF, onChange = {}, padLabel = "채도와 밝기", hueLabel = "색상", hexLabel = "색 코드")
}

@Preview(name = "색 고르기 · 라이트", locale = "ko", widthDp = 360)
@Composable
private fun ColorPickerLight() = PreviewCanvas(dark = false) { ColorPickerSample() }

@Preview(name = "색 고르기 · 다크", locale = "ko", widthDp = 360)
@Composable
private fun ColorPickerDark() = PreviewCanvas(dark = true) { ColorPickerSample() }

@Composable
private fun ThemeSwatchRow() = Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    val colors = DiaryTheme.colors
    DiaryThemeSwatch("라벤더", "라벤더", colors.accent, colors.onAccent, selected = true, onClick = {})
    DiaryThemeSwatch("숲", "숲", Color(0xFF3F7A52), Color.White, selected = false, onClick = {})
    DiaryThemeSwatch("직접 고르기", "직접 고르기, Plus", null, colors.onAccent, selected = false, onClick = {}, plusLabel = "Plus")
    DiaryThemeSwatch("바다", "바다", Color(0xFF3E6E96), Color.White, selected = false, onClick = {}, enabled = false)
}

@Preview(name = "테마 견본 · 고름·안 고름·직접 고르기·꺼짐 · 라이트", locale = "ko", widthDp = 360)
@Composable
private fun ThemeSwatchLight() = PreviewCanvas(dark = false) { ThemeSwatchRow() }

@Preview(name = "테마 견본 · 고름·안 고름·직접 고르기·꺼짐 · 다크", locale = "ko", widthDp = 360)
@Composable
private fun ThemeSwatchDark() = PreviewCanvas(dark = true) { ThemeSwatchRow() }

@Composable
private fun ThemeModeRow() = Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
    DiaryThemeModeOption("라이트", LightDiaryColors, MiniHomeLayout.Feed, selected = true, onClick = {})
    DiaryThemeModeOption("다크", DarkDiaryColors, MiniHomeLayout.Card, selected = false, onClick = {})
    DiaryThemeModeOption("격자", LightDiaryColors, MiniHomeLayout.Grid, selected = false, onClick = {})
}

@Preview(name = "화면 모드 그림 · 피드·카드·격자 · 라이트", locale = "ko", widthDp = 400)
@Composable
private fun ThemeModeLight() = PreviewCanvas(dark = false) { ThemeModeRow() }

@Preview(name = "화면 모드 그림 · 피드·카드·격자 · 다크", locale = "ko", widthDp = 400)
@Composable
private fun ThemeModeDark() = PreviewCanvas(dark = true) { ThemeModeRow() }
