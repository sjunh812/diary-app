package org.sjhstudio.diary.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.datetime.LocalTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.sjhstudio.diary.core.designsystem.component.DiaryBottomSheet
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonPair
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonSize
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsRow
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsSwitchRow
import org.sjhstudio.diary.core.designsystem.component.DiaryStatusBox
import org.sjhstudio.diary.core.designsystem.component.DiaryTimeWheel
import org.sjhstudio.diary.core.designsystem.component.DiaryTopBar
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.resources.Res as DsRes
import org.sjhstudio.diary.core.designsystem.resources.ds_close
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.designsystem.theme.LocalScreenGutter
import org.sjhstudio.diary.core.ui.rememberDiaryDateFormat
import org.sjhstudio.diary.core.ui.rememberNotificationAccess
import org.sjhstudio.diary.feature.settings.resources.Res
import org.sjhstudio.diary.feature.settings.resources.reminder_body
import org.sjhstudio.diary.feature.settings.resources.reminder_permission_allow
import org.sjhstudio.diary.feature.settings.resources.reminder_permission_body
import org.sjhstudio.diary.feature.settings.resources.reminder_permission_title
import org.sjhstudio.diary.feature.settings.resources.reminder_save
import org.sjhstudio.diary.feature.settings.resources.reminder_time
import org.sjhstudio.diary.feature.settings.resources.settings_reminder

/**
 * 매일 알림(Figma "매일 알림 (R1)", "매일 알림 상태").
 * 설정 > 매일 알림.
 * 켜면 알림 시각 줄이 생기고, 그 줄을 누르면 바텀 시트의 휠로 고름.
 * 알림을 끄면 시각 줄을 숨김.
 * 켜 두었는데 휴대폰에서 알림이 허용되지 않았으면 허용 안내를 보임.
 */
@Composable
fun ReminderSettingsScreen(
    state: ReminderUiState,
    timeLabel: String,
    onEnabledChange: (Boolean) -> Unit,
    onTime: () -> Unit,
    onAllow: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DiaryTheme.colors
    Column(modifier.fillMaxSize().background(colors.canvas).systemBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        DiaryTopBar(title = stringResource(Res.string.settings_reminder), onBack = onBack)
        Column(Modifier.widthIn(max = DiaryWriteSize.settingsMax).fillMaxWidth().verticalScroll(rememberScrollState())) {
            DiarySettingsSwitchRow(
                stringResource(Res.string.settings_reminder),
                checked = state.enabled,
                onCheckedChange = onEnabledChange,
                description = stringResource(Res.string.reminder_body)
            )
            if (state.enabled) DiarySettingsRow(stringResource(Res.string.reminder_time), onClick = onTime, value = timeLabel)
            if (state.showsPermissionNotice) {
                DiaryStatusBox(
                    DiaryIcons.Bell,
                    stringResource(Res.string.reminder_permission_title),
                    stringResource(Res.string.reminder_permission_body),
                    modifier = Modifier.padding(horizontal = LocalScreenGutter.current, vertical = DiarySpacing.x4),
                    actionLabel = stringResource(Res.string.reminder_permission_allow),
                    onAction = onAllow
                )
            }
        }
    }
}

/**
 * 알림 시각 시트.
 * 휠을 돌려도 "저장"을 눌러야 바뀌고, "닫기"는 그대로 둠.
 */
@Composable
fun ReminderTimeSheet(minuteOfDay: Int, is24Hour: Boolean, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    var picked by rememberSaveable { mutableIntStateOf(minuteOfDay) }
    DiaryBottomSheet(onDismiss = onDismiss, title = stringResource(Res.string.reminder_time)) {
        DiaryTimeWheel(minuteOfDay = minuteOfDay, onChange = { picked = it }, is24Hour = is24Hour)
        DiaryButtonPair(
            stringResource(DsRes.string.ds_close),
            onDismiss,
            stringResource(Res.string.reminder_save),
            { onSave(picked) },
            size = DiaryButtonSize.XLarge
        )
    }
}

/**
 * 매일 알림 화면과 시트, 알림 허용 묻기를 이음.
 * 앱 틀은 이것만 부름.
 */
@Composable
fun ReminderSettingsRoute(onBack: () -> Unit) {
    val viewModel = koinViewModel<ReminderSettingsViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val access = rememberNotificationAccess()
    val format = rememberDiaryDateFormat()
    var picking by rememberSaveable { mutableStateOf(false) }
    // 휴대폰 설정에서 허용하고 돌아오면 안내가 사라짐
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshPermission()
        onPauseOrDispose {}
    }
    ReminderSettingsScreen(
        state = state,
        timeLabel = format.time(LocalTime(state.minuteOfDay / 60, state.minuteOfDay % 60)),
        onEnabledChange = { enabled ->
            viewModel.setEnabled(enabled)
            // 켤 때 아직 허용되지 않았으면 시스템 창으로 물음
            // 거절해도 켜 둔 채 안내를 보임
            if (enabled && state.notificationsAllowed != true) access.request { viewModel.refreshPermission() }
        },
        onTime = { picking = true },
        onAllow = { access.allow { viewModel.refreshPermission() } },
        onBack = onBack
    )
    if (picking) {
        ReminderTimeSheet(
            minuteOfDay = state.minuteOfDay,
            is24Hour = format.is24Hour,
            onDismiss = { picking = false },
            onSave = { minute ->
                picking = false
                viewModel.setTime(minute)
            }
        )
    }
}
