package org.sjhstudio.diary.feature.lock

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.component.DiaryButton
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonKind
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonSize
import org.sjhstudio.diary.core.designsystem.component.DiaryNumberPad
import org.sjhstudio.diary.core.designsystem.component.DiaryPadKey
import org.sjhstudio.diary.core.designsystem.component.DiaryTopBar
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.feature.lock.resources.Res
import org.sjhstudio.diary.feature.lock.resources.lock_biometric
import org.sjhstudio.diary.feature.lock.resources.lock_forgot
import org.sjhstudio.diary.feature.lock.resources.lock_key_unavailable
import org.sjhstudio.diary.feature.lock.resources.lock_off_failed
import org.sjhstudio.diary.feature.lock.resources.lock_title
import org.sjhstudio.diary.feature.lock.resources.lock_wait_body
import org.sjhstudio.diary.feature.lock.resources.lock_wait_title
import org.sjhstudio.diary.feature.lock.resources.lock_wrong

/**
 * 잠금 화면(Figma "잠금 (L1)", 스펙 001 FR-015~FR-020)
 * 틀리면 점이 빨갛게 바뀌고 남은 횟수 표시
 * 5번 틀리면 30초 동안 숫자판을 흐리게 막음
 * 뒤로 가기는 따로 받지 않아 앱을 닫음
 */
@Composable
fun LockScreen(
    state: LockUiState,
    showBiometric: Boolean,
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    onBiometric: () -> Unit,
    onForgot: (() -> Unit)?,
    modifier: Modifier = Modifier,
    title: String? = null,
    onBack: (() -> Unit)? = null
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

            LockMessage.KeyUnavailable -> PinLine(stringResource(Res.string.lock_key_unavailable), colors.danger)

            LockMessage.NotSaved -> PinLine(stringResource(Res.string.lock_off_failed), colors.danger)
        }
    PinEntryLayout(
        title = if (waiting) stringResource(Res.string.lock_wait_title) else title ?: stringResource(Res.string.lock_title),
        line = line,
        entered = if (waiting) 0 else state.entered,
        wrongCount = state.wrongCount,
        modifier = modifier.pinKeys(enabled = !waiting && !state.checking, onDigit = onDigit, onDelete = onDelete),
        top = {
            if (onBack != null) {
                DiaryTopBar(title = "", onBack = onBack)
                Spacer(Modifier.height(BadgeTop - DiarySpacing.x14))
            } else {
                Spacer(Modifier.height(BadgeTop))
            }
        }
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
        if (onForgot != null) {
            // 버튼 안쪽 6을 더해 숫자판 → 글자 20, 글자 → 아래 24(Figma L1). 합이 아래 빈칸 64와 같음
            Spacer(Modifier.height(DiarySpacing.x1_5 + DiarySpacing.x2))
            DiaryButton(stringResource(Res.string.lock_forgot), onForgot, kind = DiaryButtonKind.QuietText, size = DiaryButtonSize.Header)
            Spacer(Modifier.height(DiarySpacing.x4 + DiarySpacing.x0_5))
        } else {
            Spacer(Modifier.height(DiarySpacing.x14 + DiarySpacing.x2))
        }
    }
}
