package org.sjhstudio.diary.feature.lock

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.component.DiaryNotice
import org.sjhstudio.diary.core.designsystem.component.DiaryNumberPad
import org.sjhstudio.diary.core.designsystem.component.DiaryTopBar
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.designsystem.theme.LocalScreenGutter
import org.sjhstudio.diary.feature.lock.resources.Res
import org.sjhstudio.diary.feature.lock.resources.pin_setup_again
import org.sjhstudio.diary.feature.lock.resources.pin_setup_mismatch
import org.sjhstudio.diary.feature.lock.resources.pin_setup_note
import org.sjhstudio.diary.feature.lock.resources.pin_setup_save_failed
import org.sjhstudio.diary.feature.lock.resources.pin_setup_title

/**
 * 비밀번호 정하기(Figma "비밀번호 정하기 (L2)", FR-019).
 * 두 번 넣어 같으면 정함.
 * 휴대폰 잠금을 아는 사람도 일기를 열 수 있다는 점을 미리 알림.
 */
@Composable
fun PinSetupScreen(
    state: PinSetupUiState,
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    PinEntryLayout(
        title = stringResource(if (state.again) Res.string.pin_setup_again else Res.string.pin_setup_title),
        line =
            when {
                state.saveFailed -> PinLine(stringResource(Res.string.pin_setup_save_failed), DiaryTheme.colors.danger)
                state.mismatch -> PinLine(stringResource(Res.string.pin_setup_mismatch), DiaryTheme.colors.danger)
                else -> null
            },
        entered = state.entered,
        wrongCount = state.wrongCount,
        modifier = modifier.pinKeys(enabled = !state.saving, onDigit = onDigit, onDelete = onDelete),
        top = {
            DiaryTopBar(title = "", onBack = onBack)
            // 상단 바가 있어도 배지는 잠금 화면과 같은 자리
            Spacer(Modifier.height(BadgeTop - DiarySpacing.x14))
        },
        middle = {
            if (!state.again) {
                Spacer(Modifier.height(DiarySpacing.x2))
                DiaryNotice(
                    stringResource(Res.string.pin_setup_note),
                    Modifier.widthIn(max = DiaryWriteSize.settingsMax).padding(horizontal = LocalScreenGutter.current)
                )
            }
        }
    ) {
        DiaryNumberPad(onDigit = onDigit, onDelete = onDelete, enabled = !state.saving)
        // 잠금 화면의 "비밀번호를 잊어버렸어요" 줄만큼 비워 숫자판 자리를 맞춤
        Spacer(Modifier.height(DiarySpacing.x14 + DiarySpacing.x2))
    }
}
