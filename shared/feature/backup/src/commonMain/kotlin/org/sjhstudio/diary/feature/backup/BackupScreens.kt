package org.sjhstudio.diary.feature.backup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.sjhstudio.diary.core.designsystem.component.DiaryBottomSheet
import org.sjhstudio.diary.core.designsystem.component.DiaryButton
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonKind
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonSize
import org.sjhstudio.diary.core.designsystem.component.DiaryDialog
import org.sjhstudio.diary.core.designsystem.component.DiaryIcon
import org.sjhstudio.diary.core.designsystem.component.DiaryNotice
import org.sjhstudio.diary.core.designsystem.component.DiaryProgressBar
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsHeader
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsRow
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsSwitchRow
import org.sjhstudio.diary.core.designsystem.component.DiaryStatusBox
import org.sjhstudio.diary.core.designsystem.component.DiaryTopBar
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryIconSize
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.model.BackupConnection
import org.sjhstudio.diary.core.model.BackupEntry
import org.sjhstudio.diary.core.model.BackupFailure
import org.sjhstudio.diary.core.model.BackupStatus
import org.sjhstudio.diary.core.ui.rememberDiaryDateFormat
import org.sjhstudio.diary.feature.backup.resources.Res
import org.sjhstudio.diary.feature.backup.resources.backup_account
import org.sjhstudio.diary.feature.backup.resources.backup_all_done
import org.sjhstudio.diary.feature.backup.resources.backup_auto
import org.sjhstudio.diary.feature.backup.resources.backup_auto_body
import org.sjhstudio.diary.feature.backup.resources.backup_connect
import org.sjhstudio.diary.feature.backup.resources.backup_disconnect
import org.sjhstudio.diary.feature.backup.resources.backup_disconnect_body
import org.sjhstudio.diary.feature.backup.resources.backup_disconnect_message
import org.sjhstudio.diary.feature.backup.resources.backup_disconnect_title
import org.sjhstudio.diary.feature.backup.resources.backup_first_body
import org.sjhstudio.diary.feature.backup.resources.backup_first_title
import org.sjhstudio.diary.feature.backup.resources.backup_group_account
import org.sjhstudio.diary.feature.backup.resources.backup_group_connection
import org.sjhstudio.diary.feature.backup.resources.backup_group_upload
import org.sjhstudio.diary.feature.backup.resources.backup_intro_body
import org.sjhstudio.diary.feature.backup.resources.backup_intro_note
import org.sjhstudio.diary.feature.backup.resources.backup_intro_title
import org.sjhstudio.diary.feature.backup.resources.backup_last
import org.sjhstudio.diary.feature.backup.resources.backup_no_space_body
import org.sjhstudio.diary.feature.backup.resources.backup_no_space_title
import org.sjhstudio.diary.feature.backup.resources.backup_now
import org.sjhstudio.diary.feature.backup.resources.backup_options
import org.sjhstudio.diary.feature.backup.resources.backup_photos_waiting
import org.sjhstudio.diary.feature.backup.resources.backup_reconnect
import org.sjhstudio.diary.feature.backup.resources.backup_reconnect_body
import org.sjhstudio.diary.feature.backup.resources.backup_reconnect_title
import org.sjhstudio.diary.feature.backup.resources.backup_running
import org.sjhstudio.diary.feature.backup.resources.backup_running_body
import org.sjhstudio.diary.feature.backup.resources.backup_running_photos
import org.sjhstudio.diary.feature.backup.resources.backup_title
import org.sjhstudio.diary.feature.backup.resources.backup_upload_cellular
import org.sjhstudio.diary.feature.backup.resources.backup_wifi_only
import org.sjhstudio.diary.feature.backup.resources.backup_wifi_only_body
import org.sjhstudio.diary.feature.backup.resources.restore_device_group
import org.sjhstudio.diary.feature.backup.resources.restore_found_action
import org.sjhstudio.diary.feature.backup.resources.restore_found_body
import org.sjhstudio.diary.feature.backup.resources.restore_found_later
import org.sjhstudio.diary.feature.backup.resources.restore_found_title
import org.sjhstudio.diary.feature.backup.resources.restore_row
import org.sjhstudio.diary.feature.backup.resources.restore_row_body

/** 설정 > 백업(Figma "백업 (B1)"). */
@Composable
fun BackupSettingsRoute(
    onBack: () -> Unit,
    onOptions: () -> Unit,
    onRestore: () -> Unit,
    onRestoreEntry: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = koinViewModel<BackupViewModel>()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val connecting by viewModel.connecting.collectAsStateWithLifecycle()
    val found by viewModel.found.collectAsStateWithLifecycle()
    BackupSettingsScreen(status, connecting, viewModel::connect, {
        viewModel.backUpNow()
    }, { viewModel.backUpNow(cellularOnce = true) }, viewModel::setAuto, onOptions, onRestore, onBack, modifier)
    found?.let { entry ->
        BackupFoundSheet(entry, viewModel::foundShown) {
            viewModel.foundShown()
            onRestoreEntry(entry.snapshotName)
        }
    }
}

/** Figma "B2 · 드라이브에 백업이 있어요". 연결한 뒤 드라이브에서 찾은 백업을 권한다. */
@Composable
fun BackupFoundSheet(entry: BackupEntry, onLater: () -> Unit, onRestore: () -> Unit) {
    DiaryBottomSheet(onDismiss = onLater, title = stringResource(Res.string.restore_found_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(DiarySpacing.x3)) {
            DiaryStatusBox(
                DiaryIcons.ShieldCheck,
                stringResource(Res.string.restore_device_group, deviceName(entry), whenText(entry)),
                countsText(entry)
            )
            BasicText(
                stringResource(Res.string.restore_found_body),
                style = DiaryTheme.typography.body.copy(color = DiaryTheme.colors.textSecondary)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2)) {
                DiaryButton(
                    stringResource(Res.string.restore_found_later),
                    onLater,
                    Modifier.weight(1f),
                    size = DiaryButtonSize.XLarge,
                    kind = DiaryButtonKind.Secondary
                )
                DiaryButton(stringResource(Res.string.restore_found_action), onRestore, Modifier.weight(1f), size = DiaryButtonSize.XLarge)
            }
        }
    }
}

@Composable
internal fun BackupSettingsScreen(
    status: BackupStatus,
    connecting: Boolean,
    onConnect: () -> Unit,
    onBackUp: () -> Unit,
    onCellular: () -> Unit,
    onAuto: (Boolean) -> Unit,
    onOptions: () -> Unit,
    onRestore: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DiaryTheme.colors
    Column(modifier.fillMaxSize().background(colors.canvas).statusBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        DiaryTopBar(stringResource(Res.string.backup_title), onBack = onBack)
        Column(Modifier.widthIn(max = DiaryWriteSize.bodyMax).fillMaxWidth().verticalScroll(rememberScrollState())) {
            if (status.connection == BackupConnection.NotConnected) {
                NotConnected(connecting, onConnect)
            } else {
                Column(
                    Modifier.padding(horizontal = DiarySpacing.screenCompact, vertical = DiarySpacing.x2),
                    verticalArrangement = Arrangement.spacedBy(DiarySpacing.x3)
                ) {
                    Status(status, onCellular, onConnect)
                    if (status.running == null && status.connection != BackupConnection.NeedsReconnect) {
                        DiaryButton(stringResource(Res.string.backup_now), onBackUp, Modifier.fillMaxWidth(), size = DiaryButtonSize.XLarge)
                    }
                    if (status.connection == BackupConnection.NeedsReconnect) {
                        DiaryButton(
                            stringResource(Res.string.backup_reconnect),
                            onConnect,
                            Modifier.fillMaxWidth(),
                            size = DiaryButtonSize.XLarge
                        )
                    }
                }
                DiarySettingsSwitchRow(
                    stringResource(Res.string.backup_auto),
                    status.autoEnabled,
                    onAuto,
                    description = stringResource(Res.string.backup_auto_body)
                )
                DiarySettingsRow(
                    stringResource(Res.string.restore_row),
                    onClick = onRestore,
                    description = stringResource(Res.string.restore_row_body)
                )
                DiarySettingsRow(stringResource(Res.string.backup_options), onClick = onOptions)
            }
            Spacer(Modifier.height(DiarySpacing.x8))
        }
    }
}

@Composable
private fun NotConnected(connecting: Boolean, onConnect: () -> Unit) {
    val colors = DiaryTheme.colors
    Column(
        Modifier.fillMaxWidth().padding(horizontal = DiarySpacing.screenCompact, vertical = DiarySpacing.x6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x2)
    ) {
        DiaryIcon(DiaryIcons.ShieldCheck, contentDescription = null, tint = colors.accentText, size = DiaryIconSize.i32)
        Spacer(Modifier.height(DiarySpacing.x1))
        BasicText(
            stringResource(Res.string.backup_intro_title),
            style = DiaryTheme.typography.title2.copy(color = colors.textPrimary, textAlign = TextAlign.Center)
        )
        BasicText(
            stringResource(Res.string.backup_intro_body),
            style = DiaryTheme.typography.body.copy(color = colors.textSecondary, textAlign = TextAlign.Center)
        )
        Spacer(Modifier.height(DiarySpacing.x4))
        DiaryNotice(stringResource(Res.string.backup_intro_note))
        Spacer(Modifier.height(DiarySpacing.x4))
        DiaryButton(
            stringResource(Res.string.backup_connect),
            onConnect,
            Modifier.fillMaxWidth(),
            size = DiaryButtonSize.XLarge,
            enabled = !connecting
        )
    }
}

@Composable
private fun Status(status: BackupStatus, onCellular: () -> Unit, onReconnect: () -> Unit) {
    val running = status.running
    val last = status.lastSuccessAt?.let { lastBackupText(it) }
    when {
        running != null -> {
            Column(verticalArrangement = Arrangement.spacedBy(DiarySpacing.x2)) {
                DiaryStatusBox(
                    DiaryIcons.ShieldCheck,
                    stringResource(Res.string.backup_running),
                    if (running.photosTotal > 0) {
                        stringResource(Res.string.backup_running_photos, running.photosDone, running.photosTotal)
                    } else {
                        stringResource(Res.string.backup_running_body)
                    }
                )
                val fraction = if (running.bytesTotal > 0) running.bytesDone.toFloat() / running.bytesTotal else 0f
                DiaryProgressBar(fraction)
            }
        }

        status.connection == BackupConnection.NeedsReconnect ->
            DiaryStatusBox(
                DiaryIcons.ShieldCheck,
                stringResource(Res.string.backup_reconnect_title),
                stringResource(Res.string.backup_reconnect_body)
            )

        status.lastFailure == BackupFailure.NO_SPACE ->
            DiaryStatusBox(
                DiaryIcons.ShieldCheck,
                stringResource(Res.string.backup_no_space_title),
                stringResource(Res.string.backup_no_space_body)
            )

        status.photosNotBackedUp > 0 && last != null ->
            DiaryStatusBox(
                DiaryIcons.ShieldCheck,
                last,
                pluralStringResource(Res.plurals.backup_photos_waiting, status.photosNotBackedUp, status.photosNotBackedUp),
                actionLabel = stringResource(Res.string.backup_upload_cellular),
                onAction = onCellular
            )

        last != null -> DiaryStatusBox(DiaryIcons.ShieldCheck, last, stringResource(Res.string.backup_all_done))

        else -> DiaryStatusBox(
            DiaryIcons.ShieldCheck,
            stringResource(Res.string.backup_first_title),
            stringResource(Res.string.backup_first_body)
        )
    }
}

@Composable
private fun lastBackupText(at: Instant): String {
    val dates = rememberDiaryDateFormat()
    val local = at.toLocalDateTime(TimeZone.currentSystemDefault())
    return stringResource(Res.string.backup_last, dates.dayWithWeekday(local.date), dates.time(local.time))
}

/** 백업 설정(Figma "백업 설정 (B13)"). */
@Composable
fun BackupOptionsRoute(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel = koinViewModel<BackupViewModel>()
    val status by viewModel.status.collectAsStateWithLifecycle()
    var asking by remember { mutableStateOf(false) }
    val colors = DiaryTheme.colors
    Column(modifier.fillMaxSize().background(colors.canvas).statusBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        DiaryTopBar(stringResource(Res.string.backup_options), onBack = onBack)
        Column(Modifier.widthIn(max = DiaryWriteSize.bodyMax).fillMaxWidth().verticalScroll(rememberScrollState())) {
            DiarySettingsHeader(stringResource(Res.string.backup_group_account))
            DiarySettingsRow(
                stringResource(Res.string.backup_account),
                onClick = {},
                value = (status.connection as? BackupConnection.Connected)?.accountLabel
            )
            DiarySettingsHeader(stringResource(Res.string.backup_group_upload), gapAbove = true)
            DiarySettingsSwitchRow(
                stringResource(Res.string.backup_wifi_only),
                !status.photosOnCellular,
                { viewModel.setPhotosOnCellular(!it) },
                description = stringResource(Res.string.backup_wifi_only_body)
            )
            DiarySettingsHeader(stringResource(Res.string.backup_group_connection), gapAbove = true)
            DiarySettingsRow(
                stringResource(Res.string.backup_disconnect),
                onClick = { asking = true },
                description = stringResource(Res.string.backup_disconnect_body)
            )
        }
    }
    if (asking) {
        DiaryDialog(
            title = stringResource(Res.string.backup_disconnect_title),
            confirmLabel = stringResource(Res.string.backup_disconnect),
            onConfirm = {
                asking = false
                viewModel.disconnect()
                onBack()
            },
            onDismiss = { asking = false },
            message = stringResource(Res.string.backup_disconnect_message),
            destructive = true
        )
    }
}
