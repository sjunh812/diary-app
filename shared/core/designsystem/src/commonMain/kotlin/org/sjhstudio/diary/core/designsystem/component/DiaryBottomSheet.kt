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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme

/**
 * 시트 높이의 끝.
 * 창 높이에 대한 비율이다(`docs/design.md` 바텀 시트).
 */
private const val MAX_HEIGHT_RATIO = 0.9f

/**
 * 시트 안에서 두 버튼이 스스로 더 띄우는 위 여백.
 * 시트의 기본 간격 12에 이 값을 더해 글과 버튼 사이가 24가 된다.
 * 시트마다 버튼을 슬롯으로 넘기지 않아도 되게 버튼 쪽이 안다.
 */
internal val LocalSheetButtonGap = staticCompositionLocalOf { 0.dp }

/**
 * 떠 있는 바텀 시트(`docs/design.md` 바텀 시트, TDS BottomSheet, SEED 최대 폭 480).
 * 좌우·아래 10 띄움 · 모서리 28 · 손잡이 48×4 · 안쪽 24 · 제목 title2 · 폭 최대 480 · 높이 최대 90%.
 * 내용이 90%를 넘으면 손잡이는 두고 내용만 스크롤한다.
 * 낮은 폰과 가로 화면에서 아래 버튼이 잘리지 않는다.
 * 내용 사이는 12, 글과 두 버튼 사이는 24다.
 * 내용에는 지연 목록이나 세로 `weight`를 넣지 않는다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    description: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = DiaryTheme.colors
    val maxHeight = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() } * MAX_HEIGHT_RATIO
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
                        .heightIn(max = maxHeight)
                        .clip(DiaryShapes.r28)
                        .background(colors.surface)
                        .padding(top = DiarySpacing.x3)
            ) {
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(width = 48.dp, height = 4.dp)
                        .clip(DiaryShapes.full)
                        .background(colors.lineStrong)
                )
                CompositionLocalProvider(LocalSheetButtonGap provides DiarySpacing.x3) {
                    Column(
                        modifier =
                            Modifier
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState())
                                .padding(start = DiarySpacing.x6, end = DiarySpacing.x6, top = DiarySpacing.x5, bottom = DiarySpacing.x6),
                        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x3)
                    ) {
                        // 제목과 설명은 한 묶음이라 다른 내용보다 가깝게 둔다.
                        if (title != null) {
                            Column(verticalArrangement = Arrangement.spacedBy(DiarySpacing.x2)) {
                                BasicText(text = title, style = DiaryTheme.typography.title2.copy(color = colors.textPrimary))
                                if (description != null) {
                                    BasicText(text = description, style = DiaryTheme.typography.body.copy(color = colors.textSecondary))
                                }
                            }
                        }
                        content()
                    }
                }
            }
        }
    }
}

/**
 * 시트 안의 동작 줄(`docs/design.md` 누름).
 * 글자는 시트 안쪽 선에 맞추고, 누름 면은 양옆으로 12 나와서 글자에 여백이 보인다.
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
