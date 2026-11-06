package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.sjhstudio.diary.core.designsystem.theme.DiaryIconSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme

/** 칩(`docs/design.md` 칩, SEED Chip). 32 · 좌우 12(아이콘이 있으면 왼쪽 10) · full · label · 아이콘 16 · 터치 영역 48. */
@Composable
fun DiaryChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    leading: (@Composable () -> Unit)? = null,
    trailingIcon: ImageVector? = null
) {
    val colors = DiaryTheme.colors
    val content = if (selected) colors.accentText else colors.textPrimary
    val interaction = remember { MutableInteractionSource() }
    // 누르는 곳은 높이 48이고 보이는 칩은 32다. 폭은 칩 그대로 둔다.
    // 최소 터치 폭까지 넓히면 글자가 짧은 칩이 칸 가운데로 밀려 줄 왼쪽이 어긋난다.
    Box(
        modifier
            .heightIn(min = DiarySpacing.minTouchTarget)
            .selectable(selected = selected, role = Role.Checkbox, interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            Modifier
                .heightIn(min = 32.dp)
                .diaryPress(interaction, colors.textPrimary, DiaryShapes.full)
                .clip(DiaryShapes.full)
                .background(if (selected) colors.selected else colors.sunken)
                // 아이콘 쪽은 아이콘 자체 여백이 있어 10으로 줄여 눈으로 좌우를 맞춘다(이 칩 안에서만 쓰는 값).
                .padding(start = if (leadingIcon != null || leading != null) 10.dp else DiarySpacing.x3, end = DiarySpacing.x3),
            horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x1),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leading?.invoke()
            if (leadingIcon != null) DiaryIcon(leadingIcon, contentDescription = null, tint = content, size = DiaryIconSize.i16)
            // 칩은 LazyRow처럼 폭이 끝없는 곳에도 놓여서 weight를 쓰지 않는다. 쓰면 글자 폭이 0이 된다.
            BasicText(
                text = text,
                style = DiaryTheme.typography.label.copy(color = content),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (trailingIcon != null) DiaryIcon(trailingIcon, contentDescription = null, tint = content, size = DiaryIconSize.i16)
        }
    }
}
