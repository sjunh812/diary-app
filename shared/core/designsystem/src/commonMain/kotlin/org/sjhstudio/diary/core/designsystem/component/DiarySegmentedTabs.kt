package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme

/** 기간 고르기 같은 탭 한 줄. 높이 44, 바탕 `sunken`, 고른 칸은 `canvas`다. */
@Composable
fun DiarySegmentedTabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = DiaryTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .height(TABS_HEIGHT)
            .clip(DiaryShapes.r12)
            .background(colors.sunken)
            .padding(DiarySpacing.x1)
    ) {
        labels.forEachIndexed { index, label ->
            val interaction = remember { MutableInteractionSource() }
            val isSelected = index == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(CELL_HEIGHT)
                    .clip(DiaryShapes.r8)
                    .background(if (isSelected) colors.canvas else colors.sunken)
                    .selectable(isSelected, interactionSource = interaction, indication = null, role = Role.Tab) { onSelect(index) },
                contentAlignment = Alignment.Center
            ) {
                val typography = DiaryTheme.typography
                BasicText(
                    label,
                    style = (if (isSelected) typography.bodyStrong else typography.body).copy(
                        color = if (isSelected) colors.textPrimary else colors.textSecondary
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

private val TABS_HEIGHT = 44.dp
private val CELL_HEIGHT = 36.dp
