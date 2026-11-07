package org.sjhstudio.diary.feature.backup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.source
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.sjhstudio.diary.core.designsystem.component.DiaryBadgeTone
import org.sjhstudio.diary.core.designsystem.component.DiaryBottomSheet
import org.sjhstudio.diary.core.designsystem.component.DiaryButton
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonPair
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonSize
import org.sjhstudio.diary.core.designsystem.component.DiaryDialog
import org.sjhstudio.diary.core.designsystem.component.DiaryHeroBadge
import org.sjhstudio.diary.core.designsystem.component.DiaryIcon
import org.sjhstudio.diary.core.designsystem.component.DiaryNotice
import org.sjhstudio.diary.core.designsystem.component.DiaryProgressBar
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsHeader
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsRow
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsSwitchRow
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsValueRow
import org.sjhstudio.diary.core.designsystem.component.DiaryStatusBox
import org.sjhstudio.diary.core.designsystem.component.DiaryToastHost
import org.sjhstudio.diary.core.designsystem.component.DiaryToastState
import org.sjhstudio.diary.core.designsystem.component.DiaryTopBar
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryIconSize
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.designsystem.theme.LocalScreenGutter
import org.sjhstudio.diary.core.model.BackupConnection
import org.sjhstudio.diary.core.model.BackupEntry
import org.sjhstudio.diary.core.model.BackupFailure
import org.sjhstudio.diary.core.model.BackupSkip
import org.sjhstudio.diary.core.model.BackupStatus
import org.sjhstudio.diary.core.model.PLATFORM_LEGACY
import org.sjhstudio.diary.core.model.RollbackInfo
import org.sjhstudio.diary.core.ui.KeepScreenOn
import org.sjhstudio.diary.core.ui.grouped
import org.sjhstudio.diary.core.ui.rememberDiaryDateFormat
import org.sjhstudio.diary.core.ui.rememberNotificationAccess
import org.sjhstudio.diary.core.ui.workContinuesInBackground
import org.sjhstudio.diary.feature.backup.resources.Res
import org.sjhstudio.diary.feature.backup.resources.backup_account
import org.sjhstudio.diary.feature.backup.resources.backup_all_done
import org.sjhstudio.diary.feature.backup.resources.backup_auto
import org.sjhstudio.diary.feature.backup.resources.backup_auto_body
import org.sjhstudio.diary.feature.backup.resources.backup_connect
import org.sjhstudio.diary.feature.backup.resources.backup_connect_failed_auth
import org.sjhstudio.diary.feature.backup.resources.backup_connect_failed_close
import org.sjhstudio.diary.feature.backup.resources.backup_connect_failed_offline
import org.sjhstudio.diary.feature.backup.resources.backup_connect_failed_other
import org.sjhstudio.diary.feature.backup.resources.backup_connect_failed_title
import org.sjhstudio.diary.feature.backup.resources.backup_disconnect
import org.sjhstudio.diary.feature.backup.resources.backup_disconnect_body
import org.sjhstudio.diary.feature.backup.resources.backup_disconnect_message
import org.sjhstudio.diary.feature.backup.resources.backup_disconnect_title
import org.sjhstudio.diary.feature.backup.resources.backup_failed_drive
import org.sjhstudio.diary.feature.backup.resources.backup_failed_local
import org.sjhstudio.diary.feature.backup.resources.backup_failed_offline
import org.sjhstudio.diary.feature.backup.resources.backup_finding_body
import org.sjhstudio.diary.feature.backup.resources.backup_finding_title
import org.sjhstudio.diary.feature.backup.resources.backup_first_body
import org.sjhstudio.diary.feature.backup.resources.backup_first_title
import org.sjhstudio.diary.feature.backup.resources.backup_group_account
import org.sjhstudio.diary.feature.backup.resources.backup_group_backup
import org.sjhstudio.diary.feature.backup.resources.backup_group_connection
import org.sjhstudio.diary.feature.backup.resources.backup_group_restore
import org.sjhstudio.diary.feature.backup.resources.backup_group_upload
import org.sjhstudio.diary.feature.backup.resources.backup_interrupted_body
import org.sjhstudio.diary.feature.backup.resources.backup_intro_body
import org.sjhstudio.diary.feature.backup.resources.backup_intro_note
import org.sjhstudio.diary.feature.backup.resources.backup_intro_title
import org.sjhstudio.diary.feature.backup.resources.backup_last
import org.sjhstudio.diary.feature.backup.resources.backup_no_space_title
import org.sjhstudio.diary.feature.backup.resources.backup_now
import org.sjhstudio.diary.feature.backup.resources.backup_options
import org.sjhstudio.diary.feature.backup.resources.backup_photos_unusable
import org.sjhstudio.diary.feature.backup.resources.backup_photos_waiting
import org.sjhstudio.diary.feature.backup.resources.backup_reconnect
import org.sjhstudio.diary.feature.backup.resources.backup_reconnect_body
import org.sjhstudio.diary.feature.backup.resources.backup_reconnect_title
import org.sjhstudio.diary.feature.backup.resources.backup_restored_body
import org.sjhstudio.diary.feature.backup.resources.backup_restored_title
import org.sjhstudio.diary.feature.backup.resources.backup_running
import org.sjhstudio.diary.feature.backup.resources.backup_running_body
import org.sjhstudio.diary.feature.backup.resources.backup_running_body_open
import org.sjhstudio.diary.feature.backup.resources.backup_running_photos
import org.sjhstudio.diary.feature.backup.resources.backup_skip_busy
import org.sjhstudio.diary.feature.backup.resources.backup_skip_empty
import org.sjhstudio.diary.feature.backup.resources.backup_space_manage
import org.sjhstudio.diary.feature.backup.resources.backup_space_note
import org.sjhstudio.diary.feature.backup.resources.backup_space_photos
import org.sjhstudio.diary.feature.backup.resources.backup_space_retry
import org.sjhstudio.diary.feature.backup.resources.backup_space_text
import org.sjhstudio.diary.feature.backup.resources.backup_space_trash
import org.sjhstudio.diary.feature.backup.resources.backup_stopped_title
import org.sjhstudio.diary.feature.backup.resources.backup_title
import org.sjhstudio.diary.feature.backup.resources.backup_upload_cellular
import org.sjhstudio.diary.feature.backup.resources.backup_wifi_only
import org.sjhstudio.diary.feature.backup.resources.backup_wifi_only_body
import org.sjhstudio.diary.feature.backup.resources.file_reading
import org.sjhstudio.diary.feature.backup.resources.file_restore
import org.sjhstudio.diary.feature.backup.resources.file_restore_body
import org.sjhstudio.diary.feature.backup.resources.file_row_body
import org.sjhstudio.diary.feature.backup.resources.file_top
import org.sjhstudio.diary.feature.backup.resources.restore_device_group
import org.sjhstudio.diary.feature.backup.resources.restore_found_action
import org.sjhstudio.diary.feature.backup.resources.restore_found_body
import org.sjhstudio.diary.feature.backup.resources.restore_found_later
import org.sjhstudio.diary.feature.backup.resources.restore_found_title
import org.sjhstudio.diary.feature.backup.resources.restore_legacy_row
import org.sjhstudio.diary.feature.backup.resources.restore_row
import org.sjhstudio.diary.feature.backup.resources.restore_row_body
import org.sjhstudio.diary.feature.backup.resources.rollback_action
import org.sjhstudio.diary.feature.backup.resources.rollback_ask_after
import org.sjhstudio.diary.feature.backup.resources.rollback_ask_body
import org.sjhstudio.diary.feature.backup.resources.rollback_ask_title
import org.sjhstudio.diary.feature.backup.resources.rollback_title
import org.sjhstudio.diary.feature.backup.resources.rollback_tomorrow
import org.sjhstudio.diary.feature.backup.resources.rollback_until

/** 설정 > 백업(Figma "백업 (B1)"). */
@Composable
fun BackupSettingsRoute(
    onBack: () -> Unit,
    onOptions: () -> Unit,
    onRestore: () -> Unit,
    onRestoreEntry: (String) -> Unit,
    onFileBackup: () -> Unit,
    onFilePreview: () -> Unit,
    modifier: Modifier = Modifier,
    statusBar: @Composable (pickFile: () -> Unit) -> Unit = {}
) {
    val viewModel = koinViewModel<BackupViewModel>()
    val files = koinViewModel<FileBackupViewModel>()
    val fileState by files.state.collectAsStateWithLifecycle()
    val picker =
        rememberFilePickerLauncher(type = FileKitType.File(), onError = { files.pickFailed() }) { file: PlatformFile? ->
            file?.let { picked -> files.read({ OkioSource(picked.source()) }, { picked.sizeOrNull() }, onFilePreview) }
        }
    val status by viewModel.status.collectAsStateWithLifecycle()
    val connecting by viewModel.connecting.collectAsStateWithLifecycle()
    val found by viewModel.found.collectAsStateWithLifecycle()
    val rollback by viewModel.rollback.collectAsStateWithLifecycle()
    val connectFailure by viewModel.connectFailure.collectAsStateWithLifecycle()
    val access = rememberNotificationAccess()
    val running = status.running != null
    val toast = remember { DiaryToastState() }
    val nothingYet = stringResource(Res.string.backup_skip_empty)
    val busy = stringResource(Res.string.backup_skip_busy)
    LaunchedEffect(viewModel) {
        viewModel.skipped.collect { skip -> toast.show(if (skip == BackupSkip.NO_ENTRIES) nothingYet else busy) }
    }
    // 백업이 시작되면 끝날 때 알릴 수 있게 한 번 묻는다.
    // 거절해도 백업은 그대로 돈다.
    LaunchedEffect(running) { if (running) access.request {} }
    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            BackupSettingsScreen(
                status,
                connecting,
                { viewModel.connect() },
                { viewModel.backUpNow() },
                { viewModel.backUpNow(cellularOnce = true) },
                viewModel::setAuto,
                onOptions,
                onRestore,
                onBack,
                Modifier.weight(1f),
                rollback,
                viewModel::rollBack,
                onFileBackup,
                onFileRestore = { picker.launch() },
                fileState = fileState
            )
            statusBar { picker.launch() }
        }
        DiaryToastHost(toast, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = DiarySpacing.x4))
    }
    found?.let { entry ->
        BackupFoundSheet(entry, viewModel::skipFound) {
            viewModel.takeFound()
            onRestoreEntry(entry.snapshotName)
        }
    }
    connectFailure?.let { BackupConnectFailedSheet(it, viewModel::connectFailureShown) }
}

/**
 * 구글 드라이브 연결 실패 안내.
 * 까닭에 맞는 할 일을 말한다.
 * 사용자가 취소한 때는 띄우지 않는다.
 */
@Composable
fun BackupConnectFailedSheet(failure: BackupFailure, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val body =
        when (failure) {
            BackupFailure.OFFLINE -> Res.string.backup_connect_failed_offline
            BackupFailure.AUTH -> Res.string.backup_connect_failed_auth
            else -> Res.string.backup_connect_failed_other
        }
    DiaryBottomSheet(
        onDismiss,
        modifier,
        title = stringResource(Res.string.backup_connect_failed_title),
        description = stringResource(body)
    ) {
        DiaryButton(
            stringResource(Res.string.backup_connect_failed_close),
            onDismiss,
            Modifier.fillMaxWidth(),
            size = DiaryButtonSize.XLarge
        )
    }
}

/**
 * Figma "B2 · 드라이브에 백업이 있어요".
 * 연결한 뒤 드라이브에서 찾은 백업을 권한다.
 * 예전 앱이 올린 백업은 기기와 편수를 몰라서 날짜와 사진이 없다는 것만 말한다.
 */
@Composable
fun BackupFoundSheet(entry: BackupEntry, onLater: () -> Unit, onRestore: () -> Unit) {
    DiaryBottomSheet(onDismiss = onLater, title = stringResource(Res.string.restore_found_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(DiarySpacing.x3)) {
            if (entry.platform == PLATFORM_LEGACY) {
                DiaryStatusBox(
                    DiaryIcons.ShieldCheck,
                    rememberDiaryDateFormat().fullDate(entry.createdAt.toLocalDateTime(TimeZone.currentSystemDefault()).date),
                    stringResource(Res.string.restore_legacy_row)
                )
            } else {
                DiaryStatusBox(
                    DiaryIcons.ShieldCheck,
                    stringResource(Res.string.restore_device_group, deviceName(entry), whenText(entry)),
                    countsText(entry)
                )
            }
            BasicText(
                stringResource(Res.string.restore_found_body),
                style = DiaryTheme.typography.body.copy(color = DiaryTheme.colors.textSecondary)
            )
            DiaryButtonPair(
                stringResource(Res.string.restore_found_later),
                onLater,
                stringResource(Res.string.restore_found_action),
                onRestore,
                size = DiaryButtonSize.XLarge
            )
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
    modifier: Modifier = Modifier,
    rollback: RollbackInfo? = null,
    onRollBack: () -> Unit = {},
    onFile: () -> Unit = {},
    onFileRestore: () -> Unit = {},
    fileState: FileBackupUi = FileBackupUi.Idle
) {
    val colors = DiaryTheme.colors
    Column(modifier.fillMaxSize().background(colors.canvas).statusBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        DiaryTopBar(stringResource(Res.string.backup_title), onBack = onBack)
        Column(Modifier.widthIn(max = DiaryWriteSize.settingsMax).fillMaxWidth().verticalScroll(rememberScrollState())) {
            // 파일로 바꾼 사람은 연결이 없어도 되돌릴 수 있어야 한다.
            rollback?.let {
                Box(Modifier.padding(start = LocalScreenGutter.current, end = LocalScreenGutter.current, top = DiarySpacing.x4)) {
                    Rollback(it, onRollBack)
                }
            }
            if (status.connection == BackupConnection.NotConnected) {
                NotConnected(connecting, onConnect)
                FileRestoreNotice(fileState)
                DiarySettingsRow(
                    stringResource(Res.string.file_top),
                    onClick = onFile,
                    description = stringResource(Res.string.file_row_body)
                )
                FileRestoreRow(onFileRestore)
            } else {
                // 공간 안내는 상태 자리만 대신한다.
                // 공간이 없어도 복원, 자동 백업, 백업 설정 길은 그대로 있어야 한다.
                if (status.lastFailure == BackupFailure.NO_SPACE && status.running == null) {
                    NoSpace(status, onBackUp, onFile)
                } else {
                    Column(
                        Modifier.padding(horizontal = LocalScreenGutter.current, vertical = DiarySpacing.x4),
                        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x4)
                    ) {
                        Status(status, onCellular, onConnect)
                        if (status.running == null && status.connection != BackupConnection.NeedsReconnect) {
                            // 다른 기기의 백업을 고르기 전에 이 기기 것을 올리지 않게 찾는 동안 막는다.
                            DiaryButton(
                                stringResource(Res.string.backup_now),
                                onBackUp,
                                Modifier.fillMaxWidth(),
                                size = DiaryButtonSize.XLarge,
                                enabled = !status.findingBackup
                            )
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
                }
                // 백업과 복원을 두 묶음으로 나눠 복원을 찾으러 다른 화면을 뒤지지 않게 한다.
                DiarySettingsHeader(stringResource(Res.string.backup_group_backup))
                DiarySettingsSwitchRow(
                    stringResource(Res.string.backup_auto),
                    status.autoEnabled,
                    onAuto,
                    description = stringResource(Res.string.backup_auto_body)
                )
                DiarySettingsRow(
                    stringResource(Res.string.file_top),
                    onClick = onFile,
                    description = stringResource(Res.string.file_row_body)
                )
                DiarySettingsRow(stringResource(Res.string.backup_options), onClick = onOptions)
                DiarySettingsHeader(stringResource(Res.string.backup_group_restore), gapAbove = true)
                FileRestoreNotice(fileState)
                DiarySettingsRow(
                    stringResource(Res.string.restore_row),
                    onClick = onRestore,
                    description = stringResource(Res.string.restore_row_body)
                )
                FileRestoreRow(onFileRestore)
            }
            Spacer(Modifier.height(DiarySpacing.x8))
        }
    }
}

@Composable
private fun FileRestoreRow(onClick: () -> Unit) {
    DiarySettingsRow(stringResource(Res.string.file_restore), onClick = onClick, description = stringResource(Res.string.file_restore_body))
}

/** 고른 파일을 앱으로 옮기는 동안의 안내와 실패 까닭. */
@Composable
private fun FileRestoreNotice(state: FileBackupUi) {
    val text =
        when (state) {
            FileBackupUi.Reading -> stringResource(Res.string.file_reading)
            is FileBackupUi.ReadFailed -> readFailedText(state)
            else -> return
        }
    DiaryNotice(text, Modifier.padding(horizontal = LocalScreenGutter.current, vertical = DiarySpacing.x2))
}

/**
 * Figma "드라이브 공간이 부족해요 (B10)".
 * 우리가 공간을 팔지 않고 정리할 곳만 알려 준다(FR-019).
 */
@Composable
private fun NoSpace(status: BackupStatus, onRetry: () -> Unit, onFile: () -> Unit) {
    val colors = DiaryTheme.colors
    val links = LocalUriHandler.current
    val size = sizeText(status.bytesShort)
    Column(
        Modifier.fillMaxWidth().padding(horizontal = LocalScreenGutter.current, vertical = DiarySpacing.x6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x2)
    ) {
        DiaryHeroBadge(DiaryIcons.CircleAlert, tone = DiaryBadgeTone.Danger)
        Spacer(Modifier.height(DiarySpacing.x1))
        BasicText(
            stringResource(Res.string.backup_no_space_title),
            style = DiaryTheme.typography.title2.copy(color = colors.textPrimary, textAlign = TextAlign.Center)
        )
        BasicText(
            if (status.failingSince != null) {
                stringResource(Res.string.backup_space_text, size)
            } else {
                pluralStringResource(Res.plurals.backup_space_photos, status.photosNotBackedUp, status.photosNotBackedUp.grouped(), size)
            },
            style = DiaryTheme.typography.body.copy(color = colors.textSecondary, textAlign = TextAlign.Center)
        )
    }
    DiarySettingsRow(stringResource(Res.string.backup_space_manage), onClick = { links.openUri(STORAGE_URL) })
    DiarySettingsRow(stringResource(Res.string.backup_space_trash), onClick = { links.openUri(TRASH_URL) })
    DiarySettingsRow(stringResource(Res.string.file_top), onClick = onFile)
    Column(
        Modifier.padding(horizontal = LocalScreenGutter.current, vertical = DiarySpacing.x3),
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x3)
    ) {
        DiaryNotice(stringResource(Res.string.backup_space_note))
        DiaryButton(stringResource(Res.string.backup_space_retry), onRetry, Modifier.fillMaxWidth(), size = DiaryButtonSize.XLarge)
    }
}

private const val STORAGE_URL = "https://drive.google.com/settings/storage"
private const val TRASH_URL = "https://drive.google.com/drive/trash"

/** Figma "B9 백업 · 되돌릴 수 있어요". */
@Composable
private fun Rollback(info: RollbackInfo, onRollBack: () -> Unit) {
    var asking by remember { mutableStateOf(false) }
    val dates = rememberDiaryDateFormat()
    val zone = TimeZone.currentSystemDefault()
    val until = info.until.toLocalDateTime(zone).date
    val tomorrow = (Clock.System.now() + 1.days).toLocalDateTime(zone).date
    DiaryStatusBox(
        DiaryIcons.Restore,
        stringResource(Res.string.rollback_title),
        if (until <=
            tomorrow
        ) {
            stringResource(Res.string.rollback_tomorrow)
        } else {
            stringResource(Res.string.rollback_until, dates.monthDay(until))
        },
        actionLabel = stringResource(Res.string.rollback_action),
        onAction = { asking = true }
    )
    if (asking) {
        DiaryDialog(
            title = stringResource(Res.string.rollback_ask_title),
            confirmLabel = stringResource(Res.string.rollback_action),
            onConfirm = {
                asking = false
                onRollBack()
            },
            onDismiss = { asking = false },
            message =
                if (info.entriesAfter > 0) {
                    stringResource(Res.string.rollback_ask_after, info.entriesBefore.grouped(), info.entriesAfter.grouped())
                } else {
                    stringResource(Res.string.rollback_ask_body, info.entriesBefore.grouped())
                }
        )
    }
}

@Composable
private fun NotConnected(connecting: Boolean, onConnect: () -> Unit) {
    val colors = DiaryTheme.colors
    Column(
        Modifier.fillMaxWidth().padding(horizontal = LocalScreenGutter.current, vertical = DiarySpacing.x6),
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
        status.findingBackup ->
            DiaryStatusBox(
                DiaryIcons.ShieldCheck,
                stringResource(Res.string.backup_finding_title),
                stringResource(Res.string.backup_finding_body)
            )

        running != null -> {
            // 화면이 꺼지면 iOS에서는 올리기가 멈춘다.
            KeepScreenOn()
            Column(verticalArrangement = Arrangement.spacedBy(DiarySpacing.x2)) {
                DiaryStatusBox(
                    DiaryIcons.ShieldCheck,
                    stringResource(Res.string.backup_running),
                    if (running.photosTotal > 0) {
                        val done = stringResource(Res.string.backup_running_photos, running.photosDone, running.photosTotal)
                        if (workContinuesInBackground) done else done + "\n" + stringResource(Res.string.backup_running_body_open)
                    } else {
                        stringResource(
                            if (workContinuesInBackground) Res.string.backup_running_body else Res.string.backup_running_body_open
                        )
                    }
                )
                // 올릴 양을 세기 전에는 막대를 그리지 않는다.
                // 빈 막대는 멈춘 것처럼 보인다.
                if (running.bytesTotal > 0) DiaryProgressBar(running.bytesDone.toFloat() / running.bytesTotal, onSunken = true)
            }
        }

        status.connection == BackupConnection.NeedsReconnect ->
            DiaryStatusBox(
                DiaryIcons.ShieldCheck,
                stringResource(Res.string.backup_reconnect_title),
                stringResource(Res.string.backup_reconnect_body)
            )

        // 마지막 시도가 실패했으면 앞선 성공보다 그 까닭과 할 일을 먼저 말한다.
        // 첫 백업이 끊긴 때도 같다.
        // 올린 사진은 남아 있다.
        status.lastFailure != null -> FailedStatus(status.lastFailure, last)

        status.photosNotBackedUp > 0 && last != null ->
            DiaryStatusBox(
                DiaryIcons.ShieldCheck,
                last,
                pluralStringResource(Res.plurals.backup_photos_waiting, status.photosNotBackedUp, status.photosNotBackedUp),
                actionLabel = stringResource(Res.string.backup_upload_cellular),
                onAction = onCellular
            )

        status.photosUnusable > 0 && last != null ->
            DiaryStatusBox(
                DiaryIcons.ShieldCheck,
                last,
                pluralStringResource(Res.plurals.backup_photos_unusable, status.photosUnusable, status.photosUnusable)
            )

        last != null -> DiaryStatusBox(DiaryIcons.ShieldCheck, last, stringResource(Res.string.backup_all_done))

        // 이 기기에서 백업한 적은 없어도 복원한 백업이 드라이브에 있다.
        // 첫 백업이라고 말하면 틀린 말이 된다.
        else -> {
            val restored = status.restoredBackupAt
            DiaryStatusBox(
                DiaryIcons.ShieldCheck,
                if (restored != null) restoredBackupText(restored) else stringResource(Res.string.backup_first_title),
                stringResource(if (restored != null) Res.string.backup_restored_body else Res.string.backup_first_body)
            )
        }
    }
}

/**
 * 마지막 시도가 실패했다.
 * 까닭과 할 일을 말하고, 앞선 성공이 있으면 언제까지 백업됐는지 함께 보인다.
 */
@Composable
private fun FailedStatus(failure: BackupFailure?, last: String?) {
    val reason =
        when (failure) {
            BackupFailure.OFFLINE -> Res.string.backup_failed_offline
            BackupFailure.DRIVE_ERROR -> Res.string.backup_failed_drive
            BackupFailure.LOCAL_IO -> Res.string.backup_failed_local
            else -> Res.string.backup_interrupted_body
        }
    DiaryStatusBox(
        DiaryIcons.CircleAlert,
        stringResource(Res.string.backup_stopped_title),
        listOfNotNull(stringResource(reason), last).joinToString(separator = "\n")
    )
}

/**
 * 복원한 백업을 말한다.
 * 홈 상태 줄과 백업 화면이 같이 쓴다.
 */
@Composable
internal fun restoredBackupText(at: Instant): String {
    val dates = rememberDiaryDateFormat()
    return stringResource(Res.string.backup_restored_title, dates.monthDay(at.toLocalDateTime(TimeZone.currentSystemDefault()).date))
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
        Column(Modifier.widthIn(max = DiaryWriteSize.settingsMax).fillMaxWidth().verticalScroll(rememberScrollState())) {
            DiarySettingsHeader(stringResource(Res.string.backup_group_account))
            DiarySettingsValueRow(
                stringResource(Res.string.backup_account),
                (status.connection as? BackupConnection.Connected)?.accountLabel.orEmpty()
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
