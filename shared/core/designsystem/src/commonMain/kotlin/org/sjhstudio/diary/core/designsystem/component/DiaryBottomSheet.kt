package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
        Box(Modifier.fillMaxWidth().navigationBarsPadding(), contentAlignment = Alignment.BottomCenter) {
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
