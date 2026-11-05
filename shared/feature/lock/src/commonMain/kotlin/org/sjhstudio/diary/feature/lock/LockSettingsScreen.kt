package org.sjhstudio.diary.feature.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.component.DiaryNotice
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsChoiceRow
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsHeader
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsRow
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsSwitchRow
import org.sjhstudio.diary.core.designsystem.component.DiaryTopBar
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.model.LockSettings
import org.sjhstudio.diary.core.model.RelockAfter
import org.sjhstudio.diary.feature.lock.resources.Res
import org.sjhstudio.diary.feature.lock.resources.lock_biometric
import org.sjhstudio.diary.feature.lock.resources.lock_settings_biometric_unavailable
import org.sjhstudio.diary.feature.lock.resources.lock_settings_change
import org.sjhstudio.diary.feature.lock.resources.lock_settings_enable
import org.sjhstudio.diary.feature.lock.resources.lock_settings_method
import org.sjhstudio.diary.feature.lock.resources.lock_settings_note
import org.sjhstudio.diary.feature.lock.resources.lock_settings_relock
import org.sjhstudio.diary.feature.lock.resources.lock_settings_relock_body
import org.sjhstudio.diary.feature.lock.resources.lock_settings_title
import org.sjhstudio.diary.feature.lock.resources.relock_cold_start
import org.sjhstudio.diary.feature.lock.resources.relock_five_minutes
import org.sjhstudio.diary.feature.lock.resources.relock_immediately
import org.sjhstudio.diary.feature.lock.resources.relock_one_minute

/**
 * 잠금 설정(Figma "잠금 설정 (L4)", FR-017~FR-019).
 * 잠금을 켜면 비밀번호 줄과 생체 인증, 다시 잠그기가 나온다.
 * 생체 인증을 지금 기기에서 쓸 수 없으면 그 까닭을 보인다. 켜 둔 것은 끄지 않는다(contracts/lock.md).
 */
@Composable
fun LockSettingsScreen(
    settings: LockSettings,
    biometricAvailable: Boolean,
    onBack: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onChangePin: () -> Unit,
    onBiometricChange: (Boolean) -> Unit,
    onRelockChange: (RelockAfter) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DiaryTheme.colors
    Column(modifier.fillMaxSize().background(colors.canvas).systemBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        DiaryTopBar(title = stringResource(Res.string.lock_settings_title), onBack = onBack)
        // 넓은 화면에서는 설정 목록 폭을 폰과 같게 둔다.
        Column(Modifier.widthIn(max = DiaryWriteSize.settingsMax).fillMaxWidth().verticalScroll(rememberScrollState())) {
            DiarySettingsSwitchRow(
                stringResource(Res.string.lock_settings_enable),
                checked = settings.enabled,
                onCheckedChange = onEnabledChange
            )
            if (settings.enabled) {
                DiarySettingsHeader(stringResource(Res.string.lock_settings_method), gapAbove = true)
                DiarySettingsRow(stringResource(Res.string.lock_settings_change), onClick = onChangePin)
                DiarySettingsSwitchRow(
                    stringResource(Res.string.lock_biometric),
                    checked = settings.biometricEnabled,
                    onCheckedChange = onBiometricChange,
                    description = if (biometricAvailable) null else stringResource(Res.string.lock_settings_biometric_unavailable),
                    enabled = biometricAvailable || settings.biometricEnabled
                )
                DiarySettingsHeader(stringResource(Res.string.lock_settings_relock), gapAbove = true)
                BasicText(
                    stringResource(Res.string.lock_settings_relock_body),
                    style = DiaryTheme.typography.caption.copy(color = colors.textSecondary),
                    modifier = Modifier.padding(
                        start = DiarySpacing.screenCompact,
                        end = DiarySpacing.screenCompact,
                        bottom = DiarySpacing.x2
                    )
                )
                for (option in RelockAfter.entries) {
                    DiarySettingsChoiceRow(stringResource(option.label()), selected = settings.relockAfter == option, onClick = {
                        onRelockChange(option)
                    })
                }
            }
            DiaryNotice(
                stringResource(Res.string.lock_settings_note),
                Modifier.padding(horizontal = DiarySpacing.screenCompact, vertical = DiarySpacing.x6)
            )
        }
    }
}

private fun RelockAfter.label(): StringResource = when (this) {
    RelockAfter.IMMEDIATELY -> Res.string.relock_immediately
    RelockAfter.ONE_MINUTE -> Res.string.relock_one_minute
    RelockAfter.FIVE_MINUTES -> Res.string.relock_five_minutes
    RelockAfter.COLD_START -> Res.string.relock_cold_start
}
