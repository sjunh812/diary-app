package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.resources.Res
import org.sjhstudio.diary.core.designsystem.resources.ds_close
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme

/**
 * 대화상자(`docs/design.md` 대화상자, M3 Dialog, 토스 UX 가이드).
 * 폭 312(최대 560) · 모서리 28 · 안쪽 24. 왼쪽 버튼은 늘 "닫기", 오른쪽이 하려는 일이다.
 * 큰 글자나 낮은 화면에서 넘치면 스크롤해서 버튼에 닿을 수 있다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryDialog(
    title: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
    destructive: Boolean = false
) {
    val colors = DiaryTheme.colors
    BasicAlertDialog(onDismissRequest = onDismiss, modifier = modifier) {
        Column(
            modifier =
                Modifier
                    .widthIn(min = 312.dp, max = 560.dp)
                    .clip(DiaryShapes.r28)
                    .background(colors.surface)
                    .verticalScroll(rememberScrollState())
                    .padding(DiarySpacing.x6),
            verticalArrangement = Arrangement.spacedBy(DiarySpacing.x2)
        ) {
            BasicText(text = title, style = DiaryTheme.typography.title2.copy(color = colors.textPrimary))
            if (message != null) BasicText(text = message, style = DiaryTheme.typography.body.copy(color = colors.textSecondary))
            DiaryButtonPair(
                secondary = stringResource(Res.string.ds_close),
                onSecondary = onDismiss,
                primary = confirmLabel,
                onPrimary = onConfirm,
                modifier = Modifier.padding(top = DiarySpacing.x4),
                primaryKind = if (destructive) DiaryButtonKind.Danger else DiaryButtonKind.Primary
            )
        }
    }
}
