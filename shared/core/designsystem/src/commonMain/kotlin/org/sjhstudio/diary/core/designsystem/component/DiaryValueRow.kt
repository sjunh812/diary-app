package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme

/**
 * 이름과 값을 양끝에 두는 읽기 전용 줄(`docs/design.md` 값 줄, Figma 기록 이전 결과 M5의 count 줄).
 * 최소 48 · 좌우 20 · 이름 `body`(textSecondary) · 값 `bodyStrong`(textPrimary).
 */
@Composable
fun DiaryValueRow(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = DiaryTheme.colors
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = DiarySpacing.minTouchTarget).padding(horizontal = DiarySpacing.screenCompact),
        horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x4),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(label, style = DiaryTheme.typography.body.copy(color = colors.textSecondary), modifier = Modifier.weight(1f))
        BasicText(value, style = DiaryTheme.typography.bodyStrong.copy(color = colors.textPrimary))
    }
}
