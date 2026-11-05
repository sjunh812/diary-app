package org.sjhstudio.diary.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.offset
import kotlin.math.max
import org.sjhstudio.diary.core.designsystem.component.DiaryIcon
import org.sjhstudio.diary.core.designsystem.component.diaryPress
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryIconSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme

/**
 * 누르면 달을 고르는 큰 제목(홈, 달력). `display` 제목 + 펼침 화살표 20, 사이 4. 긴 제목은 말줄임하고 화살표는 남긴다.
 * 차지하는 자리는 글 높이 그대로다. 그래야 다른 탭 제목과 높이가 같고 제목 아래 간격도 그대로다.
 * 누르는 곳만 위아래로 48까지, 옆으로 8 넓혀서 누름 면이 글자에 붙지 않는다(`docs/design.md` 터치 영역, 누름).
 */
@Composable
fun DiaryTitleButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = DiaryTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier
            .semantics { heading() }
            .layout { measurable, constraints ->
                val side = DiarySpacing.x2.roundToPx()
                val content = measurable.minIntrinsicHeight(constraints.maxWidth + side * 2)
                val touch = max(content, DiarySpacing.minTouchTarget.roundToPx())
                val wider = constraints.offset(horizontal = side * 2)
                val placeable = measurable.measure(wider.copy(minHeight = touch, maxHeight = touch))
                layout(placeable.width - side * 2, content) { placeable.placeRelative(-side, (content - touch) / 2) }
            }
            .diaryPress(interaction, colors.textPrimary, DiaryShapes.r12)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = DiarySpacing.x2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x1)
    ) {
        BasicText(
            text,
            modifier = Modifier.weight(1f, fill = false),
            style = DiaryTheme.typography.display.copy(color = colors.textPrimary),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        DiaryIcon(DiaryIcons.ChevronDown, contentDescription = null, tint = colors.textSecondary, size = DiaryIconSize.i20)
    }
}
