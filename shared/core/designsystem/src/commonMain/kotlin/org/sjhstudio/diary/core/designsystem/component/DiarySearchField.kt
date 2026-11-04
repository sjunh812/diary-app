package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryIconSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme

/** 찾기 입력 칸(Figma "찾기 (S1-a)"). 높이 44, 모서리 12, 글이 있으면 지우기 버튼이 나온다. */
@Composable
fun DiarySearchField(value: String, onValueChange: (String) -> Unit, hint: String, clearLabel: String, modifier: Modifier = Modifier) {
    val colors = DiaryTheme.colors
    val typography = DiaryTheme.typography
    val focus = LocalFocusManager.current
    Row(
        modifier
            .height(FIELD_HEIGHT)
            .clip(DiaryShapes.r12)
            .background(colors.sunken)
            .padding(horizontal = DiarySpacing.x3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2)
    ) {
        DiaryIcon(DiaryIcons.Search, contentDescription = null, tint = colors.textSecondary, size = DiaryIconSize.i20)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = typography.body.copy(color = colors.textPrimary),
            cursorBrush = SolidColor(colors.accent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) BasicText(hint, style = typography.body.copy(color = colors.placeholder), maxLines = 1)
                    inner()
                }
            }
        )
        if (value.isNotEmpty()) {
            val interaction = remember { MutableInteractionSource() }
            Box(
                Modifier
                    .size(CLEAR_SIZE)
                    .clip(DiaryShapes.full)
                    .background(colors.textTertiary)
                    .clickable(interactionSource = interaction, indication = null, role = Role.Button) { onValueChange("") },
                contentAlignment = Alignment.Center
            ) {
                DiaryIcon(DiaryIcons.Close, contentDescription = clearLabel, tint = colors.canvas, size = DiaryIconSize.i16)
            }
        }
    }
}

private val FIELD_HEIGHT = 44.dp
private val CLEAR_SIZE = 18.dp
