package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme

/**
 * 묶음 안쪽의 글자 탭(`docs/design.md` 글자 탭, Material 3 Secondary tabs)
 * 화면 전체를 거르는 [DiarySegmentedTabs]와 모양을 달리해 위계를 보임
 * 고른 칸은 `bodyStrong` `textPrimary`와 밑줄 2, 나머지는 `body` `textSecondary`
 * 칸 사이 20 · 누르는 곳은 줄 높이 48 · 아래 `line` 1.
 * 글자가 커져 한 줄에 다 들어가지 않으면 옆으로 스크롤
 */
@Composable
fun DiaryTextTabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = DiaryTheme.colors
    val typography = DiaryTheme.typography
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.BottomStart) {
        Box(Modifier.fillMaxWidth().height(DiarySpacing.hairline).background(colors.line))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x5)) {
            labels.forEachIndexed { index, label ->
                val isSelected = index == selected
                val interaction = remember { MutableInteractionSource() }
                var width by remember { mutableStateOf(Dp.Hairline) }
                val density = LocalDensity.current
                Column(
                    Modifier
                        .heightIn(min = DiarySpacing.minTouchTarget)
                        .selectable(isSelected, interactionSource = interaction, indication = null, role = Role.Tab) { onSelect(index) },
                    verticalArrangement = Arrangement.Bottom
                ) {
                    BasicText(
                        label,
                        Modifier.onSizeChanged { width = with(density) { it.width.toDp() } },
                        style = (if (isSelected) typography.bodyStrong else typography.body).copy(
                            color = if (isSelected) colors.textPrimary else colors.textSecondary
                        ),
                        maxLines = 1
                    )
                    Box(Modifier.height(DiarySpacing.x2))
                    Box(
                        Modifier
                            .width(width)
                            .height(DiarySpacing.x0_5)
                            .background(if (isSelected) colors.textPrimary else Color.Transparent)
                    )
                }
            }
        }
    }
}
