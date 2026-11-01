package org.sjhstudio.diary.feature.lock

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.component.DiaryNumberPad
import org.sjhstudio.diary.core.designsystem.component.DiaryPadKey
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.feature.lock.resources.Res
import org.sjhstudio.diary.feature.lock.resources.lock_biometric
import org.sjhstudio.diary.feature.lock.resources.lock_forgot
import org.sjhstudio.diary.feature.lock.resources.lock_title
import org.sjhstudio.diary.feature.lock.resources.lock_wait_body
import org.sjhstudio.diary.feature.lock.resources.lock_wait_title
import org.sjhstudio.diary.feature.lock.resources.lock_wrong

/**
 * 잠금 화면(Figma "잠금 (L1)", 스펙 001 FR-015~FR-020).
 * 틀리면 점이 빨갛게 바뀌고 남은 횟수를 보인다. 5번 틀리면 30초 동안 숫자판을 흐리게 막는다.
 * 뒤로 가기는 따로 받지 않아 앱을 닫는다.
 */
@Composable
fun LockScreen(
    state: LockUiState,
    showBiometric: Boolean,
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    onBiometric: () -> Unit,
    onForgot: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DiaryTheme.colors
    val message = state.message
    val waiting = message is LockMessage.Waiting
    val line =
        when (message) {
            LockMessage.None -> null

            is LockMessage.Wrong -> PinLine(
                pluralStringResource(Res.plurals.lock_wrong, message.remaining, message.remaining),
                colors.danger
            )

            is LockMessage.Waiting -> PinLine(
                pluralStringResource(Res.plurals.lock_wait_body, message.seconds, message.seconds),
                colors.textSecondary
            )
        }
    PinEntryLayout(
        title = stringResource(if (waiting) Res.string.lock_wait_title else Res.string.lock_title),
        line = line,
        entered = if (waiting) 0 else state.entered,
        error = message is LockMessage.Wrong,
        modifier = modifier.pinKeys(enabled = !waiting && !state.checking, onDigit = onDigit, onDelete = onDelete)
    ) {
        DiaryNumberPad(
            onDigit = onDigit,
            onDelete = onDelete,
            enabled = !waiting && !state.checking,
            bottomStart = if (showBiometric) {
                DiaryPadKey(
                    DiaryIcons.Fingerprint,
                    stringResource(Res.string.lock_biometric),
                    onBiometric
                )
            } else {
                null
            }
        )
        Spacer(Modifier.height(DiarySpacing.x1_5))
        Box(
            modifier =
                Modifier
                    .heightIn(min = DiarySpacing.minTouchTarget)
                    .clickable(role = Role.Button, onClick = onForgot)
                    .padding(horizontal = DiarySpacing.x3),
            contentAlignment = Alignment.Center
        ) {
            BasicText(stringResource(Res.string.lock_forgot), style = DiaryTheme.typography.label.copy(color = colors.textSecondary))
        }
        Spacer(Modifier.height(DiarySpacing.x2 + DiarySpacing.x0_5))
    }
}
