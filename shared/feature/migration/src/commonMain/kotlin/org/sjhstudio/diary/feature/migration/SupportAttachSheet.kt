package org.sjhstudio.diary.feature.migration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.component.DiaryBottomSheet
import org.sjhstudio.diary.core.designsystem.component.DiaryButton
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonKind
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonSize
import org.sjhstudio.diary.core.designsystem.component.FirstLineIcon
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.feature.migration.resources.Res
import org.sjhstudio.diary.feature.migration.resources.support_sheet_body
import org.sjhstudio.diary.feature.migration.resources.support_sheet_contains
import org.sjhstudio.diary.feature.migration.resources.support_sheet_deleted
import org.sjhstudio.diary.feature.migration.resources.support_sheet_restore
import org.sjhstudio.diary.feature.migration.resources.support_sheet_title
import org.sjhstudio.diary.feature.migration.resources.support_with
import org.sjhstudio.diary.feature.migration.resources.support_without

/**
 * 고쳐 받기 문의 시트(Figma "M5 · 고쳐 받기 문의", FR-010a). 일부만 옮겨진 기기의 문의하기에서 먼저 뜬다.
 * 첨부하기 전에 무엇이 들어 있는지와 지우는 때를 보인다. 첨부는 사용자가 고를 때만 한다(헌법 II 예외).
 */
@Composable
fun SupportAttachSheet(onDismiss: () -> Unit, onContactWithout: () -> Unit, onContactWith: () -> Unit) {
    val colors = DiaryTheme.colors
    DiaryBottomSheet(onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(DiarySpacing.x3)) {
            BasicText(stringResource(Res.string.support_sheet_title), style = DiaryTheme.typography.title2.copy(color = colors.textPrimary))
            BasicText(stringResource(Res.string.support_sheet_body), style = DiaryTheme.typography.body.copy(color = colors.textSecondary))
            // 아이콘 줄 목록(`docs/design.md` 상태 상자): 아이콘 16, 사이 12, 줄 사이 16
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(DiaryShapes.r16)
                        .background(colors.sunken)
                        .padding(horizontal = DiarySpacing.x4, vertical = DiarySpacing.x3 + DiarySpacing.x0_5),
                verticalArrangement = Arrangement.spacedBy(DiarySpacing.x4)
            ) {
                Line(DiaryIcons.Copy, stringResource(Res.string.support_sheet_contains))
                Line(DiaryIcons.Trash, stringResource(Res.string.support_sheet_deleted))
                Line(DiaryIcons.History, stringResource(Res.string.support_sheet_restore))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2)) {
            DiaryButton(
                text = stringResource(Res.string.support_without),
                onClick = onContactWithout,
                size = DiaryButtonSize.Large,
                kind = DiaryButtonKind.Secondary,
                modifier = Modifier.weight(1f)
            )
            DiaryButton(
                text = stringResource(Res.string.support_with),
                onClick = onContactWith,
                size = DiaryButtonSize.Large,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun Line(icon: ImageVector, text: String) {
    val colors = DiaryTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x3), verticalAlignment = Alignment.Top) {
        FirstLineIcon(icon, DiaryTheme.typography.body, colors.textSecondary)
        BasicText(text, style = DiaryTheme.typography.body.copy(color = colors.textPrimary))
    }
}
