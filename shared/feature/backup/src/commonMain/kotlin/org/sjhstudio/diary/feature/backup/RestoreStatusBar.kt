package org.sjhstudio.diary.feature.backup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.sjhstudio.diary.core.designsystem.component.DiaryStatusBox
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.model.PhotoStop
import org.sjhstudio.diary.core.model.RollbackRun
import org.sjhstudio.diary.core.model.WaitingPhotos
import org.sjhstudio.diary.core.ui.grouped
import org.sjhstudio.diary.core.ui.workContinuesInBackground
import org.sjhstudio.diary.feature.backup.resources.Res
import org.sjhstudio.diary.feature.backup.resources.restore_note_pending
import org.sjhstudio.diary.feature.backup.resources.restore_note_pending_body
import org.sjhstudio.diary.feature.backup.resources.restore_note_pending_space_body
import org.sjhstudio.diary.feature.backup.resources.restore_retry
import org.sjhstudio.diary.feature.backup.resources.restore_wait_account
import org.sjhstudio.diary.feature.backup.resources.restore_wait_file
import org.sjhstudio.diary.feature.backup.resources.restore_wait_network
import org.sjhstudio.diary.feature.backup.resources.restore_wait_now
import org.sjhstudio.diary.feature.backup.resources.restore_wait_pick
import org.sjhstudio.diary.feature.backup.resources.restore_wait_running
import org.sjhstudio.diary.feature.backup.resources.restore_wait_wifi
import org.sjhstudio.diary.feature.backup.resources.rollback_failed_body
import org.sjhstudio.diary.feature.backup.resources.rollback_failed_title
import org.sjhstudio.diary.feature.backup.resources.rollback_running_body
import org.sjhstudio.diary.feature.backup.resources.rollback_running_title
import org.sjhstudio.diary.feature.backup.resources.rollback_waiting_body
import org.sjhstudio.diary.feature.backup.resources.rollback_waiting_title

/**
 * 백업 화면 아래에 두는 복원 소식.
 * 멈춘 사진이 몇 장인지, 왜 멈췄는지, 무엇을 하면 되는지 보인다.
 * 할 일이 없으면 자리를 차지하지 않는다.
 */
@Composable
fun RestoreStatusBar(onPickFile: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel = koinViewModel<RestoreStatusViewModel>()
    val waiting by viewModel.waiting.collectAsStateWithLifecycle()
    val rollback by viewModel.rollback.collectAsStateWithLifecycle()
    RestoreStatusBar(waiting, rollback, viewModel::fetchNow, onPickFile, viewModel::rollBack, modifier)
}

@Composable
internal fun RestoreStatusBar(
    waiting: WaitingPhotos?,
    rollback: RollbackRun,
    onFetch: () -> Unit,
    onPickFile: () -> Unit,
    onRollBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (waiting == null && rollback == RollbackRun.IDLE) return
    val width = Modifier.widthIn(max = DiaryWriteSize.settingsMax)
    Column(
        modifier
            .fillMaxWidth()
            .background(DiaryTheme.colors.canvas)
            .navigationBarsPadding()
            .padding(horizontal = DiarySpacing.screenCompact, vertical = DiarySpacing.x3),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x3)
    ) {
        if (rollback != RollbackRun.IDLE) Rollback(rollback, onRollBack, width)
        if (waiting != null) Waiting(waiting, onFetch, onPickFile, width)
    }
}

@Composable
private fun Rollback(run: RollbackRun, onRollBack: () -> Unit, modifier: Modifier) {
    val failed = run == RollbackRun.FAILED
    val (title, body) =
        when (run) {
            RollbackRun.WAITING -> Res.string.rollback_waiting_title to Res.string.rollback_waiting_body
            RollbackRun.FAILED -> Res.string.rollback_failed_title to Res.string.rollback_failed_body
            else -> Res.string.rollback_running_title to Res.string.rollback_running_body
        }
    DiaryStatusBox(
        if (failed) DiaryIcons.CircleAlert else DiaryIcons.Restore,
        stringResource(title),
        stringResource(body),
        modifier,
        actionLabel = if (failed) stringResource(Res.string.restore_retry) else null,
        onAction = onRollBack.takeIf { failed }
    )
}

@Composable
private fun Waiting(waiting: WaitingPhotos, onFetch: () -> Unit, onPickFile: () -> Unit, modifier: Modifier) {
    val (label, action) =
        when (waiting.stop) {
            // 받는 중이거나 연결이 풀렸으면 눌러도 나아지지 않는다.
            null, PhotoStop.ACCOUNT -> null to null

            PhotoStop.FILE -> Res.string.restore_wait_pick to onPickFile

            else -> Res.string.restore_wait_now to onFetch
        }
    DiaryStatusBox(
        DiaryIcons.Download,
        pluralStringResource(Res.plurals.restore_note_pending, waiting.count, waiting.count.grouped()),
        stringResource(waitingBody(waiting.stop)),
        modifier,
        actionLabel = label?.let { stringResource(it) },
        onAction = action
    )
}

/** 시스템이 깨워 주지 않는 플랫폼에서는 앱을 다시 열어야 잇는다. */
private fun waitingBody(stop: PhotoStop?): StringResource = when (stop) {
    null -> Res.string.restore_wait_running
    PhotoStop.NO_SPACE -> Res.string.restore_note_pending_space_body
    PhotoStop.ACCOUNT -> Res.string.restore_wait_account
    PhotoStop.FILE -> Res.string.restore_wait_file
    PhotoStop.NETWORK -> if (workContinuesInBackground) Res.string.restore_wait_network else Res.string.restore_note_pending_body
    PhotoStop.WIFI -> if (workContinuesInBackground) Res.string.restore_wait_wifi else Res.string.restore_note_pending_body
    PhotoStop.INTERRUPTED -> Res.string.restore_note_pending_body
}
