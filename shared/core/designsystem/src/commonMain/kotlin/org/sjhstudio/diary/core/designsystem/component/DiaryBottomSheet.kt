package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme

/**
 * 떠 있는 바텀 시트(`docs/design.md` 바텀 시트, TDS BottomSheet, SEED 최대 폭 480).
 * 좌우·아래 10 띄움 · 모서리 28 · 손잡이 48×4 · 안쪽 24 · 제목 title2 · 폭 최대 480.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = DiaryTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.Transparent,
        scrimColor = colors.scrim,
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        modifier = modifier
    ) {
        // 입력칸이 있는 시트는 키보드 위로 올라온다.
        Box(Modifier.fillMaxWidth().navigationBarsPadding().imePadding(), contentAlignment = Alignment.BottomCenter) {
            Column(
                modifier =
                    Modifier
                        .padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
                        .widthIn(max = 480.dp)
                        .fillMaxWidth()
                        .clip(DiaryShapes.r28)
                        .background(colors.surface)
                        .padding(start = DiarySpacing.x6, end = DiarySpacing.x6, top = DiarySpacing.x3, bottom = DiarySpacing.x6),
                verticalArrangement = Arrangement.spacedBy(DiarySpacing.x5)
            ) {
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(width = 48.dp, height = 4.dp)
                        .clip(DiaryShapes.full)
                        .background(colors.lineStrong)
                )
                if (title != null) BasicText(text = title, style = DiaryTheme.typography.title2.copy(color = colors.textPrimary))
                content()
            }
        }
    }
}

/**
 * 시트 안의 동작 줄(`docs/design.md` 누름). 글자는 시트 안쪽 선에 맞추고, 누름 면은 양옆으로 12 나와서 글자에 여백이 보인다.
 * 면이 줄 폭과 같으면 아이콘이 면 끝에 붙어 보인다(2026-11-05 사용자 지적).
 */
@Composable
fun DiarySheetAction(icon: ImageVector, label: String, tint: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier
            .fillMaxWidth()
            .layout { measurable, constraints ->
                val outset = DiarySpacing.x3.roundToPx()
                val width = constraints.maxWidth + outset * 2
                val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
                layout(constraints.maxWidth, placeable.height) { placeable.place(-outset, 0) }
            }
            .diaryPress(interaction, DiaryTheme.colors.textPrimary, DiaryShapes.r12)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = DiarySpacing.x3, vertical = DiarySpacing.x3),
        horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x3),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DiaryIcon(icon, contentDescription = null, tint = tint)
        BasicText(label, style = DiaryTheme.typography.body.copy(color = tint))
    }
}
