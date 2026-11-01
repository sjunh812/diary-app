package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import org.sjhstudio.diary.core.designsystem.theme.DiaryIconSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme

/** 안내 상자 한 줄: 아이콘 16 · 굵은 제목 · 설명. */
class DiaryNote(val icon: ImageVector, val title: String, val body: String?)

/**
 * 결과 안내 상자(Figma result-notes). 바탕 sunken · 모서리 16 · 줄 안쪽 16.
 * 정보는 문장보다 상자로 모아 훑어보기 쉽게 한다(토스 UX 라이팅).
 */
@Composable
fun DiaryNoteBox(notes: List<DiaryNote>, modifier: Modifier = Modifier) {
    if (notes.isEmpty()) return
    val colors = DiaryTheme.colors
    Column(modifier = modifier.fillMaxWidth().clip(DiaryShapes.r16).background(colors.sunken)) {
        for (note in notes) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(DiarySpacing.x4),
                horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x4),
                verticalAlignment = Alignment.Top
            ) {
                DiaryIcon(
                    note.icon,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    size = DiaryIconSize.i16,
                    modifier = Modifier.padding(top = DiarySpacing.x0_5)
                )
                Column(verticalArrangement = Arrangement.spacedBy(DiarySpacing.x0_5)) {
                    BasicText(note.title, style = DiaryTheme.typography.bodyStrong.copy(color = colors.textPrimary))
                    if (note.body != null) BasicText(note.body, style = DiaryTheme.typography.caption.copy(color = colors.textSecondary))
                }
            }
        }
    }
}
