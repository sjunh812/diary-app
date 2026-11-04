package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import org.sjhstudio.diary.core.designsystem.theme.DiaryIconSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme

/** 안내 상자 한 줄: 아이콘 16 · 굵은 제목 · 설명. */
class DiaryNote(val icon: ImageVector, val title: String, val body: String?)

/**
 * 결과 안내 상자(Figma result-notes, `docs/design.md` 안내 상자, SEED Callout).
 * 바탕 sunken · 모서리 16 · 안쪽 20 · 줄 사이 16 · 아이콘 16과 글 사이 12(`docs/design.md` 상자 여백).
 * 아이콘은 제목 첫 줄 가운데에 둔다.
 * 정보는 문장보다 상자로 모아 훑어보기 쉽게 한다(토스 UX 라이팅).
 */
@Composable
fun DiaryNoteBox(notes: List<DiaryNote>, modifier: Modifier = Modifier) {
    if (notes.isEmpty()) return
    val colors = DiaryTheme.colors
    Column(
        modifier = modifier.fillMaxWidth().clip(DiaryShapes.r16).background(colors.sunken).padding(DiarySpacing.x5),
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x4)
    ) {
        for (note in notes) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x3),
                verticalAlignment = Alignment.Top
            ) {
                FirstLineIcon(note.icon, DiaryTheme.typography.bodyStrong, colors.textSecondary)
                Column(verticalArrangement = Arrangement.spacedBy(DiarySpacing.x0_5)) {
                    BasicText(note.title, style = DiaryTheme.typography.bodyStrong.copy(color = colors.textPrimary))
                    if (note.body != null) BasicText(note.body, style = DiaryTheme.typography.caption.copy(color = colors.textSecondary))
                }
            }
        }
    }
}

/**
 * 옆 글의 첫 줄 높이 칸 가운데에 놓는 16 아이콘(`docs/design.md` 아이콘과 그림).
 * 줄 높이가 sp라서 기기 글자 크기가 커져도 첫 줄 가운데에 맞는다.
 */
@Composable
fun FirstLineIcon(icon: ImageVector, line: TextStyle, tint: Color) {
    val height = with(LocalDensity.current) { line.lineHeight.toDp() }
    Box(Modifier.height(height), contentAlignment = Alignment.Center) {
        DiaryIcon(icon, contentDescription = null, tint = tint, size = DiaryIconSize.i16)
    }
}
