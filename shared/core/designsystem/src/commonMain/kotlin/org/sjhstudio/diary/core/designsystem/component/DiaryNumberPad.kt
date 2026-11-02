package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.resources.Res
import org.sjhstudio.diary.core.designsystem.resources.ds_delete
import org.sjhstudio.diary.core.designsystem.resources.ds_pin_progress
import org.sjhstudio.diary.core.designsystem.theme.DiaryIconSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiaryStateAlpha
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme

/** 숫자판 왼쪽 아래 칸에 두는 키(잠금 화면의 생체 인증). */
data class DiaryPadKey(val icon: ImageVector, val label: String, val onClick: () -> Unit)

/**
 * 비밀번호 숫자판(`docs/design.md` 숫자판, TDS NumberKeypad).
 * 3열 · 키 높이 64 · 폭 최대 350 · 숫자 `keypad` · 지우기 24 · 왼쪽 아래 키 32 · 누르면 모서리 20 면과 살짝 축소(누름 효과).
 * [enabled]가 거짓이면 38%로 흐리게 두고 누르지 못한다(5번 틀린 뒤 기다림).
 */
@Composable
fun DiaryNumberPad(
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    bottomStart: DiaryPadKey? = null
) {
    val colors = DiaryTheme.colors
    val digit = DiaryTheme.typography.keypad.copy(color = colors.textPrimary)
    Column(
        modifier = modifier.widthIn(max = 350.dp).fillMaxWidth().alpha(if (enabled) 1f else DiaryStateAlpha.DISABLED_CONTENT)
    ) {
        for (row in listOf("123", "456", "789")) {
            Row(Modifier.fillMaxWidth()) {
                for (c in row) PadKey(enabled, label = null, onClick = { onDigit(c) }) { BasicText(c.toString(), style = digit) }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            if (bottomStart != null) {
                PadKey(enabled, bottomStart.label, bottomStart.onClick) {
                    DiaryIcon(bottomStart.icon, contentDescription = null, tint = colors.textPrimary, size = DiaryIconSize.i32)
                }
            } else {
                Box(Modifier.weight(1f))
            }
            PadKey(enabled, label = null, onClick = { onDigit('0') }) { BasicText("0", style = digit) }
            PadKey(enabled, stringResource(Res.string.ds_delete), onDelete) {
                DiaryIcon(DiaryIcons.Delete, contentDescription = null, tint = colors.textPrimary)
            }
        }
    }
}

@Composable
private fun RowScope.PadKey(enabled: Boolean, label: String?, onClick: () -> Unit, content: @Composable () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier =
            Modifier
                .weight(1f)
                .height(64.dp)
                .diaryPress(interaction, DiaryTheme.colors.textPrimary, DiaryShapes.r20)
                .clickable(interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
                .then(if (label != null) Modifier.semantics { contentDescription = label } else Modifier),
        contentAlignment = Alignment.Center
    ) { content() }
}

/**
 * 비밀번호 칸 점(Figma pin-dots). 지름 14 · 사이 20.
 * 빈 칸은 `lineStrong` 선 1.5, 찬 칸은 `textPrimary`, 틀렸을 때는 모두 `danger`로 채운다.
 */
@Composable
fun DiaryPinDots(filled: Int, modifier: Modifier = Modifier, length: Int = 4, error: Boolean = false) {
    val colors = DiaryTheme.colors
    val progress = stringResource(Res.string.ds_pin_progress, filled, length)
    Row(
        modifier = modifier.semantics { contentDescription = progress },
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        repeat(length) { index ->
            val dot = Modifier.size(14.dp).clip(DiaryShapes.full)
            when {
                error -> Box(dot.background(colors.danger))
                index < filled -> Box(dot.background(colors.textPrimary))
                else -> Box(dot.border(1.5.dp, colors.lineStrong, DiaryShapes.full))
            }
        }
    }
}
