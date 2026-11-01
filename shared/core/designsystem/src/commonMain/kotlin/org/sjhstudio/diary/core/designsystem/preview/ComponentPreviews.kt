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
import org.sjhstudio.diary.core.designsystem.component.DiaryButton
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonKind
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonSize
import org.sjhstudio.diary.core.designsystem.component.DiaryChip
import org.sjhstudio.diary.core.designsystem.component.DiaryEmptyState
import org.sjhstudio.diary.core.designsystem.component.DiaryEntryRow
import org.sjhstudio.diary.core.designsystem.component.DiaryHeroBadge
import org.sjhstudio.diary.core.designsystem.component.DiaryMonthHeader
import org.sjhstudio.diary.core.designsystem.component.DiaryNote
import org.sjhstudio.diary.core.designsystem.component.DiaryNoteBox
import org.sjhstudio.diary.core.designsystem.component.DiaryNotice
import org.sjhstudio.diary.core.designsystem.component.DiaryProgressBar
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsHeader
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsRow
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsSwitchRow
import org.sjhstudio.diary.core.designsystem.component.DiaryTopBar
import org.sjhstudio.diary.core.designsystem.component.DiaryValueRow
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme

// 디자인 시스템 컴포넌트 프리뷰. 견본 글은 Figma 화면과 같은 지어낸 글이고 화면 문구가 아니다(문구는 각 기능의 문자열 리소스).
// 한국어를 먼저 보고, 라이트와 다크를 나란히 둔다.

/** 프리뷰 바탕. 폰 폭 390에 화면 바탕색을 깐다. 다른 모듈의 프리뷰도 쓴다. */
@Composable
fun PreviewCanvas(dark: Boolean, content: @Composable () -> Unit) {
    DiaryTheme(darkTheme = dark) {
        Column(Modifier.width(390.dp).background(DiaryTheme.colors.canvas).padding(vertical = 16.dp)) { content() }
    }
}

@Composable
private fun Buttons() {
    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DiaryButton("시작하기", {}, size = DiaryButtonSize.XLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DiaryButton("첨부 없이 문의", {}, Modifier.weight(1f), kind = DiaryButtonKind.Secondary)
            DiaryButton("첨부해서 문의", {}, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DiaryButton("오늘 일기 쓰기", {}, size = DiaryButtonSize.Medium, kind = DiaryButtonKind.Weak)
            DiaryButton("백업", {}, size = DiaryButtonSize.Small, kind = DiaryButtonKind.Weak)
            DiaryButton("지우기", {}, size = DiaryButtonSize.Medium, kind = DiaryButtonKind.Danger)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DiaryChip("좋음", selected = true, onClick = {}, leadingIcon = DiaryIcons.Star)
            DiaryChip("기분", selected = false, onClick = {})
        }
    }
}

@Preview(name = "버튼과 칩 · 라이트", locale = "ko")
@Composable
private fun ButtonsLight() = PreviewCanvas(dark = false) { Buttons() }

@Preview(name = "버튼과 칩 · 다크", locale = "ko")
@Composable
private fun ButtonsDark() = PreviewCanvas(dark = true) { Buttons() }

@Composable
private fun SettingsRows() {
    DiaryTopBar(title = "설정", onBack = {})
    DiarySettingsHeader("백업")
    DiarySettingsRow("백업과 복원", {}, value = "오늘 오후 9:30")
    DiarySettingsRow("원본 파일 내보내기", {}, leadingIcon = DiaryIcons.Download)
    DiarySettingsSwitchRow("자동 백업", checked = true, onCheckedChange = {}, description = "하루 한 번, 바뀐 것만 올려요")
    DiaryValueRow("옮긴 일기", "1,284편")
    DiaryValueRow("찾을 수 없는 사진", "33장")
}

@Preview(name = "설정 줄 · 라이트", locale = "ko")
@Composable
private fun SettingsRowsLight() = PreviewCanvas(dark = false) { SettingsRows() }

@Preview(name = "설정 줄 · 다크", locale = "ko")
@Composable
private fun SettingsRowsDark() = PreviewCanvas(dark = true) { SettingsRows() }

@Composable
private fun Boxes() {
    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DiaryHeroBadge(DiaryIcons.CircleCheck)
        DiaryNoteBox(
            listOf(
                DiaryNote(DiaryIcons.ImageOff, "찾을 수 없는 사진 33장", "예전 앱이 임시 공간에 두었다가 지워진 사진이에요. 일기 안에 자리를 남겨 뒀어요."),
                DiaryNote(DiaryIcons.CalendarCheck, "날짜를 바로잡은 일기 2편", "쓸 때 고른 날짜로 고쳤어요.")
            )
        )
        DiaryNotice("예전 앱 파일 일부가 깨져 일기 12편을 읽지 못했어요. 원래 기록은 휴대폰 안에 그대로 있어요.", icon = DiaryIcons.ShieldCheck)
        DiaryProgressBar(0.62f)
    }
}

@Preview(name = "상자와 배지 · 라이트", locale = "ko")
@Composable
private fun BoxesLight() = PreviewCanvas(dark = false) { Boxes() }

@Preview(name = "상자와 배지 · 다크", locale = "ko")
@Composable
private fun BoxesDark() = PreviewCanvas(dark = true) { Boxes() }

@Composable
private fun Entries() {
    DiaryEntryRow(
        weekday = "토",
        day = "31",
        meta = listOf("오후 9:40", "망원동"),
        preview = "퇴근길에 한강까지 걸었다. 바람이 차가워졌는데 그래서 더 좋았다.",
        moodColor = DiaryTheme.colors.mood.good,
        favorite = true,
        dividerAbove = false
    )
    DiaryEntryRow(weekday = null, day = null, meta = listOf("오전 8:10"), preview = "아침 회의가 길었다.", moodColor = DiaryTheme.colors.mood.tired)
    DiaryMonthHeader("9월")
    DiaryEmptyState(DiaryIcons.BookOpen, "아직 쓴 일기가 없어요", description = "오늘 있었던 일 한 줄이면 충분해요", actionLabel = "오늘 일기 쓰기", onAction = {})
}

@Preview(name = "일기 줄과 빈 상태 · 라이트", locale = "ko")
@Composable
private fun EntriesLight() = PreviewCanvas(dark = false) { Entries() }

@Preview(name = "일기 줄과 빈 상태 · 다크", locale = "ko")
@Composable
private fun EntriesDark() = PreviewCanvas(dark = true) { Entries() }
