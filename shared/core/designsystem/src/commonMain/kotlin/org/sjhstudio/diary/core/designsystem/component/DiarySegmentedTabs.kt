package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme

/**
 * 기간 고르기 같은 탭 한 줄(`docs/design.md` 탭 한 줄). 높이 48, 바탕 `sunken`, 고른 칸은 `canvas`다.
 * 칸은 폭을 고르게 나누고, 위아래 4를 띄운 고른 표시가 40이어도 누르는 곳은 줄 높이 전체다.
 */
@Composable
fun DiarySegmentedTabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = DiaryTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .heightIn(min = DiarySpacing.minTouchTarget)
            .clip(DiaryShapes.r12)
            .background(colors.sunken)
            .padding(horizontal = DiarySpacing.x1)
    ) {
        labels.forEachIndexed { index, label ->
            val interaction = remember { MutableInteractionSource() }
            val isSelected = index == selected
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .selectable(isSelected, interactionSource = interaction, indication = null, role = Role.Tab) { onSelect(index) }
                    .padding(vertical = DiarySpacing.x1)
                    .clip(DiaryShapes.r8)
                    .background(if (isSelected) colors.canvas else colors.sunken),
                contentAlignment = Alignment.Center
            ) {
                val typography = DiaryTheme.typography
                BasicText(
                    label,
                    style = (if (isSelected) typography.bodyStrong else typography.body).copy(
                        color = if (isSelected) colors.textPrimary else colors.textSecondary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
