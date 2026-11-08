package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
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

/**
 * 찾기 입력 칸(`docs/design.md` 검색 칸, Figma "찾기 (S1-a)").
 * 높이 44, 모서리 12, 글이 있으면 지우기 버튼이 나온다.
 * 지우기는 원 16에 누르는 곳 48이다.
 * 누르는 곳이 칸 높이 44보다 커서 칸을 `clip`하지 않고 모양은 바탕으로만 그린다.
 */
@Composable
fun DiarySearchField(value: String, onValueChange: (String) -> Unit, hint: String, clearLabel: String, modifier: Modifier = Modifier) {
    val colors = DiaryTheme.colors
    val typography = DiaryTheme.typography
    val focus = LocalFocusManager.current
    Row(
        modifier
            .heightIn(min = FIELD_HEIGHT)
            .background(colors.sunken, DiaryShapes.r12)
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
            // 바깥 칸은 원 크기만 차지해서 입력 칸 높이를 밀어 올리지 않고, 누르는 곳만 넘쳐 48이 된다.
            Box(Modifier.size(DiaryIconSize.i16), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .requiredSize(DiarySpacing.minTouchTarget)
                        .clickable(interactionSource = interaction, indication = null, role = Role.Button) { onValueChange("") },
                    contentAlignment = Alignment.Center
                ) {
                    // 누름 면은 입력 칸 안에 들어가게 원 둘레 8까지만 그린다.
                    Box(
                        Modifier.size(DiaryIconSize.i16 + DiarySpacing.x4).diaryPress(interaction, colors.textPrimary, DiaryShapes.full),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(Modifier.size(DiaryIconSize.i16).clip(DiaryShapes.full).background(colors.textTertiary)) {
                            // X는 원보다 한 단계 작게 둔다.
                            // 원과 같은 크기면 X가 원을 꽉 채워 커 보였다(2026-11-08 사용자 지적, iOS 검색칸 지우기도 X가 원의 3분의 1 남짓이다).
                            DiaryIcon(
                                DiaryIcons.Close,
                                contentDescription = clearLabel,
                                tint = colors.canvas,
                                modifier = Modifier.align(Alignment.Center),
                                size = DiaryIconSize.i12
                            )
                        }
                    }
                }
            }
        }
    }
}

private val FIELD_HEIGHT = 44.dp
