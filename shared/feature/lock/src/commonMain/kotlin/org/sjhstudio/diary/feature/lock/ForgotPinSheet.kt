package org.sjhstudio.diary.feature.lock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.component.DiaryBottomSheet
import org.sjhstudio.diary.core.designsystem.component.DiaryButton
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonKind
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonSize
import org.sjhstudio.diary.core.designsystem.resources.Res as DsRes
import org.sjhstudio.diary.core.designsystem.resources.ds_close
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.feature.lock.resources.Res
import org.sjhstudio.diary.feature.lock.resources.forgot_body
import org.sjhstudio.diary.feature.lock.resources.forgot_confirm
import org.sjhstudio.diary.feature.lock.resources.forgot_no_device_lock_body
import org.sjhstudio.diary.feature.lock.resources.forgot_no_device_lock_title
import org.sjhstudio.diary.feature.lock.resources.forgot_title

/**
 * 비밀번호를 잊어버렸을 때(Figma "비밀번호를 잊었을 때 (L3)", FR-019).
 * 서버·계정·이메일 없이 휴대폰 화면 잠금으로만 본인을 확인한다.
 * 화면 잠금이 없으면 다시 정할 수 없다고 알린다. 백업 안내 버튼은 백업(스펙 003)이 생기면 붙인다.
 */
@Composable
fun ForgotPinSheet(deviceSecure: Boolean, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val colors = DiaryTheme.colors
    DiaryBottomSheet(
        onDismiss = onDismiss,
        title = stringResource(if (deviceSecure) Res.string.forgot_title else Res.string.forgot_no_device_lock_title)
    ) {
        BasicText(
            stringResource(if (deviceSecure) Res.string.forgot_body else Res.string.forgot_no_device_lock_body),
            style = DiaryTheme.typography.body.copy(color = colors.textSecondary)
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2)) {
            DiaryButton(
                stringResource(DsRes.string.ds_close),
                onClick = onDismiss,
                kind = DiaryButtonKind.Secondary,
                size = DiaryButtonSize.Large,
                modifier = Modifier.weight(1f)
            )
            if (deviceSecure) {
                DiaryButton(
                    stringResource(Res.string.forgot_confirm),
                    onClick = onConfirm,
                    size = DiaryButtonSize.Large,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
