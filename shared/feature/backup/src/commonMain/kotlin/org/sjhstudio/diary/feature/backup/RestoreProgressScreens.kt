package org.sjhstudio.diary.feature.backup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.sjhstudio.diary.core.designsystem.component.DiaryBadgeTone
import org.sjhstudio.diary.core.designsystem.component.DiaryButton
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonKind
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonSize
import org.sjhstudio.diary.core.designsystem.component.DiaryDialog
import org.sjhstudio.diary.core.designsystem.component.DiaryHeroBadge
import org.sjhstudio.diary.core.designsystem.component.DiaryNote
import org.sjhstudio.diary.core.designsystem.component.DiaryNoteBox
import org.sjhstudio.diary.core.designsystem.component.DiaryNotice
import org.sjhstudio.diary.core.designsystem.component.DiaryProgressBar
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsHeader
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsRow
import org.sjhstudio.diary.core.designsystem.component.DiaryStatusBox
import org.sjhstudio.diary.core.designsystem.component.DiaryTopBar
import org.sjhstudio.diary.core.designsystem.component.DiaryValueRow
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.CenteredLineBreak
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.designsystem.theme.LocalScreenGutter
import org.sjhstudio.diary.core.model.BackupEntry
import org.sjhstudio.diary.core.model.BackupEntryState
import org.sjhstudio.diary.core.model.PLATFORM_LEGACY
import org.sjhstudio.diary.core.model.PhotoStop
import org.sjhstudio.diary.core.model.RestoreFailure
import org.sjhstudio.diary.core.model.RestorePreparation
import org.sjhstudio.diary.core.model.RestorePreview
import org.sjhstudio.diary.core.model.RestoreResult
import org.sjhstudio.diary.core.model.RestoreState
import org.sjhstudio.diary.core.ui.ByteUnit
import org.sjhstudio.diary.core.ui.DiarySkeleton
import org.sjhstudio.diary.core.ui.KeepScreenOn
import org.sjhstudio.diary.core.ui.SkeletonBlock
import org.sjhstudio.diary.core.ui.SkeletonLine
import org.sjhstudio.diary.core.ui.byteSize
import org.sjhstudio.diary.core.ui.grouped
import org.sjhstudio.diary.core.ui.rememberDiaryDateFormat
import org.sjhstudio.diary.core.ui.rememberNotificationAccess
import org.sjhstudio.diary.core.ui.rememberOpenLink
import org.sjhstudio.diary.core.ui.workContinuesInBackground
import org.sjhstudio.diary.feature.backup.resources.Res
import org.sjhstudio.diary.feature.backup.resources.restore_contact
import org.sjhstudio.diary.feature.backup.resources.restore_count_entries
import org.sjhstudio.diary.feature.backup.resources.restore_count_photos
import org.sjhstudio.diary.feature.backup.resources.restore_damaged
import org.sjhstudio.diary.feature.backup.resources.restore_device_group
import org.sjhstudio.diary.feature.backup.resources.restore_device_unknown
import org.sjhstudio.diary.feature.backup.resources.restore_done_added
import org.sjhstudio.diary.feature.backup.resources.restore_done_body
import org.sjhstudio.diary.feature.backup.resources.restore_done_nothing
import org.sjhstudio.diary.feature.backup.resources.restore_done_photos
import org.sjhstudio.diary.feature.backup.resources.restore_done_updated
import org.sjhstudio.diary.feature.backup.resources.restore_empty_body
import org.sjhstudio.diary.feature.backup.resources.restore_empty_title
import org.sjhstudio.diary.feature.backup.resources.restore_failed_busy_body
import org.sjhstudio.diary.feature.backup.resources.restore_failed_busy_title
import org.sjhstudio.diary.feature.backup.resources.restore_failed_mismatch_body
import org.sjhstudio.diary.feature.backup.resources.restore_failed_mismatch_title
import org.sjhstudio.diary.feature.backup.resources.restore_failed_other_body
import org.sjhstudio.diary.feature.backup.resources.restore_failed_other_title
import org.sjhstudio.diary.feature.backup.resources.restore_failed_space_body
import org.sjhstudio.diary.feature.backup.resources.restore_failed_space_title
import org.sjhstudio.diary.feature.backup.resources.restore_fetching_body
import org.sjhstudio.diary.feature.backup.resources.restore_group_legacy
import org.sjhstudio.diary.feature.backup.resources.restore_join
import org.sjhstudio.diary.feature.backup.resources.restore_keep_note
import org.sjhstudio.diary.feature.backup.resources.restore_legacy_action
import org.sjhstudio.diary.feature.backup.resources.restore_legacy_body
import org.sjhstudio.diary.feature.backup.resources.restore_legacy_gaps
import org.sjhstudio.diary.feature.backup.resources.restore_legacy_gaps_body
import org.sjhstudio.diary.feature.backup.resources.restore_legacy_no_photos
import org.sjhstudio.diary.feature.backup.resources.restore_legacy_no_photos_body
import org.sjhstudio.diary.feature.backup.resources.restore_legacy_recent
import org.sjhstudio.diary.feature.backup.resources.restore_legacy_row
import org.sjhstudio.diary.feature.backup.resources.restore_legacy_same_time
import org.sjhstudio.diary.feature.backup.resources.restore_legacy_same_time_body
import org.sjhstudio.diary.feature.backup.resources.restore_legacy_title
import org.sjhstudio.diary.feature.backup.resources.restore_list_failed
import org.sjhstudio.diary.feature.backup.resources.restore_list_failed_body
import org.sjhstudio.diary.feature.backup.resources.restore_lock_action
import org.sjhstudio.diary.feature.backup.resources.restore_lock_body
import org.sjhstudio.diary.feature.backup.resources.restore_lock_title
import org.sjhstudio.diary.feature.backup.resources.restore_merge
import org.sjhstudio.diary.feature.backup.resources.restore_merging_body
import org.sjhstudio.diary.feature.backup.resources.restore_needs_update
import org.sjhstudio.diary.feature.backup.resources.restore_no_mail
import org.sjhstudio.diary.feature.backup.resources.restore_no_space
import org.sjhstudio.diary.feature.backup.resources.restore_not_in_backup
import org.sjhstudio.diary.feature.backup.resources.restore_note_both
import org.sjhstudio.diary.feature.backup.resources.restore_note_both_body
import org.sjhstudio.diary.feature.backup.resources.restore_note_missing
import org.sjhstudio.diary.feature.backup.resources.restore_note_missing_body
import org.sjhstudio.diary.feature.backup.resources.restore_note_original
import org.sjhstudio.diary.feature.backup.resources.restore_note_original_body
import org.sjhstudio.diary.feature.backup.resources.restore_note_pending
import org.sjhstudio.diary.feature.backup.resources.restore_note_pending_body
import org.sjhstudio.diary.feature.backup.resources.restore_note_pending_file_body
import org.sjhstudio.diary.feature.backup.resources.restore_note_pending_space_body
import org.sjhstudio.diary.feature.backup.resources.restore_photo_progress
import org.sjhstudio.diary.feature.backup.resources.restore_pick_other
import org.sjhstudio.diary.feature.backup.resources.restore_platform_android
import org.sjhstudio.diary.feature.backup.resources.restore_platform_ios
import org.sjhstudio.diary.feature.backup.resources.restore_preview_body
import org.sjhstudio.diary.feature.backup.resources.restore_preview_title
import org.sjhstudio.diary.feature.backup.resources.restore_preview_top
import org.sjhstudio.diary.feature.backup.resources.restore_replace_action
import org.sjhstudio.diary.feature.backup.resources.restore_replace_body
import org.sjhstudio.diary.feature.backup.resources.restore_replace_entry
import org.sjhstudio.diary.feature.backup.resources.restore_replace_title
import org.sjhstudio.diary.feature.backup.resources.restore_replaced_body
import org.sjhstudio.diary.feature.backup.resources.restore_replaced_title
import org.sjhstudio.diary.feature.backup.resources.restore_retry
import org.sjhstudio.diary.feature.backup.resources.restore_row
import org.sjhstudio.diary.feature.backup.resources.restore_running_note
import org.sjhstudio.diary.feature.backup.resources.restore_running_note_open
import org.sjhstudio.diary.feature.backup.resources.restore_running_title
import org.sjhstudio.diary.feature.backup.resources.restore_see_entries
import org.sjhstudio.diary.feature.backup.resources.restore_settings_note
import org.sjhstudio.diary.feature.backup.resources.restore_size_gb
import org.sjhstudio.diary.feature.backup.resources.restore_size_kb
import org.sjhstudio.diary.feature.backup.resources.restore_size_mb
import org.sjhstudio.diary.feature.backup.resources.restore_start
import org.sjhstudio.diary.feature.backup.resources.restore_unit_entries
import org.sjhstudio.diary.feature.backup.resources.restore_unit_photos
import org.sjhstudio.diary.feature.backup.resources.restore_value_added
import org.sjhstudio.diary.feature.backup.resources.restore_value_both
import org.sjhstudio.diary.feature.backup.resources.restore_value_entries
import org.sjhstudio.diary.feature.backup.resources.restore_value_existing
import org.sjhstudio.diary.feature.backup.resources.restore_value_merged
import org.sjhstudio.diary.feature.backup.resources.restore_value_photo
import org.sjhstudio.diary.feature.backup.resources.restore_value_photos
import org.sjhstudio.diary.feature.backup.resources.restore_value_updated
import org.sjhstudio.diary.feature.backup.resources.restore_waiting_body
import org.sjhstudio.diary.feature.backup.resources.restore_when

// 복원 진행·결과·실패 화면

/**
 * 복원하는 중과 결과(Figma B5, B6).
 * 뒤로 가면 멈추지 않고 홈으로 감.
 * 나간 뒤에 나온 결과는 앱 틀의 토스트로 다시 엶([RestoreResultNotice]).
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RestoreProgressRoute(onHome: () -> Unit, onLock: () -> Unit, onPickOther: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel = koinViewModel<RestoreProgressViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    // 결과를 닫으면 상태가 비는데, 밀려 나가는 동안 빈 화면이 되지 않게 마지막 화면을 그대로 그림
    val held = remember { HeldState() }
    val shown = if (state == RestoreState.Idle) held.last else state.also { held.last = it }
    var leaving by remember { mutableStateOf(false) }
    val finish = {
        leaving = true
        onHome()
        viewModel.dismiss()
    }
    val access = rememberNotificationAccess()
    // 사진을 받는 동안 앱을 나가도 끝나면 알릴 수 있게 시작할 때 한 번 물음
    // 거절해도 복원은 그대로 돎
    LaunchedEffect(access) { access.request {} }
    val home by rememberUpdatedState(onHome)
    // 할 일 없이 열렸을 때만 바로 나감. 결과를 닫아 비었을 때는 이미 나가는 중
    LaunchedEffect(state) { if (state == RestoreState.Idle && !leaving) home() }
    BackHandler(onBack = if (state is RestoreState.Done) finish else onHome)
    RestoreProgressScreen(shown, onHome, finish, onLock, viewModel::retry, onPickOther, modifier)
}

/** 나가는 동안 그릴 마지막 상태. 그리기 상태가 아니라 다시 그리지 않음 */
private class HeldState {
    var last: RestoreState = RestoreState.Idle
}

@Composable
fun RestoreProgressScreen(
    state: RestoreState,
    onSeeEntries: () -> Unit,
    onStart: () -> Unit,
    onLock: () -> Unit,
    onRetry: () -> Unit,
    onPickOther: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize().background(DiaryTheme.colors.canvas).statusBarsPadding(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = DiaryWriteSize.settingsMax).fillMaxSize()) {
            when (state) {
                RestoreState.Idle -> Unit
                RestoreState.WaitingForBackup -> Running(null, onSeeEntries, Res.string.restore_waiting_body)
                RestoreState.Merging -> Running(null, onSeeEntries, Res.string.restore_merging_body)
                is RestoreState.FetchingPhotos -> Running(state, onSeeEntries, Res.string.restore_fetching_body)
                is RestoreState.Done -> Result(state.result, onLock, onStart)
                is RestoreState.Failed -> Failure(state.reason, onRetry, onPickOther)
            }
        }
    }
}

@Composable
internal fun ColumnScope.Running(photos: RestoreState.FetchingPhotos?, onSeeEntries: () -> Unit, body: StringResource) {
    val colors = DiaryTheme.colors
    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(DiarySpacing.x10))
        Hero(
            DiaryIcons.Restore,
            stringResource(Res.string.restore_running_title),
            stringResource(body),
            fill = false
        )
        if (photos != null) {
            ValueGroup(Modifier.padding(horizontal = LocalScreenGutter.current)) {
                DiaryProgressBar(if (photos.total > 0) photos.done.toFloat() / photos.total else 1f)
                Spacer(Modifier.height(DiarySpacing.x3))
                DiaryValueRow(
                    stringResource(Res.string.restore_value_entries),
                    stringResource(
                        Res.string.restore_value_merged,
                        pluralStringResource(Res.plurals.restore_unit_entries, photos.entries, photos.entries.grouped())
                    )
                )
                DiaryValueRow(
                    stringResource(Res.string.restore_value_photo),
                    stringResource(
                        Res.string.restore_photo_progress,
                        photos.done.grouped(),
                        pluralStringResource(Res.plurals.restore_unit_photos, photos.total, photos.total.grouped())
                    )
                )
            }
        }
    }
    // 화면이 꺼지면 iOS에서는 받기가 멈춤
    KeepScreenOn()
    Bottom {
        BasicText(
            stringResource(if (workContinuesInBackground) Res.string.restore_running_note else Res.string.restore_running_note_open),
            style = DiaryTheme.typography.caption.copy(
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                lineBreak = CenteredLineBreak
            ),
            modifier = Modifier.fillMaxWidth()
        )
        if (photos != null) {
            DiaryButton(
                stringResource(Res.string.restore_see_entries),
                onSeeEntries,
                Modifier.fillMaxWidth(),
                size = DiaryButtonSize.XLarge,
                kind = DiaryButtonKind.Secondary
            )
        }
    }
}

@Composable
internal fun ColumnScope.Result(result: RestoreResult, onLock: () -> Unit, onStart: () -> Unit) {
    val notes = resultNotes(result)
    // 덧붙일 안내가 없으면 다른 완료 화면처럼 가운데에
    if (notes.isEmpty() && !result.lockWasOn) {
        Hero(DiaryIcons.CircleCheck, resultTitle(result), resultBody(result))
    } else {
        ResultWithNotes(result, notes, onLock)
    }
    Bottom { DiaryButton(stringResource(Res.string.restore_start), onStart, Modifier.fillMaxWidth(), size = DiaryButtonSize.XLarge) }
}

@Composable
private fun ColumnScope.ResultWithNotes(result: RestoreResult, notes: List<DiaryNote>, onLock: () -> Unit) {
    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(DiarySpacing.x10))
        Hero(DiaryIcons.CircleCheck, resultTitle(result), resultBody(result), fill = false)
        Column(Modifier.padding(horizontal = LocalScreenGutter.current), verticalArrangement = Arrangement.spacedBy(DiarySpacing.x4)) {
            DiaryNoteBox(notes)
            if (result.lockWasOn) {
                DiaryStatusBox(
                    DiaryIcons.Lock,
                    stringResource(Res.string.restore_lock_title),
                    stringResource(Res.string.restore_lock_body),
                    actionLabel = stringResource(Res.string.restore_lock_action),
                    onAction = onLock
                )
            }
        }
    }
}

@Composable
internal fun resultTitle(result: RestoreResult): String = when {
    result.replaced -> stringResource(Res.string.restore_replaced_title)

    result.entriesAdded > 0 -> pluralStringResource(Res.plurals.restore_done_added, result.entriesAdded, result.entriesAdded.grouped())

    result.entriesUpdated > 0 -> pluralStringResource(
        Res.plurals.restore_done_updated,
        result.entriesUpdated,
        result.entriesUpdated.grouped()
    )

    else -> stringResource(Res.string.restore_done_nothing)
}

@Composable
internal fun resultBody(result: RestoreResult): String {
    val kept = stringResource(if (result.replaced) Res.string.restore_replaced_body else Res.string.restore_done_body)
    val allPhotos = result.photosRestored > 0 && result.photosPending == 0 && result.photosMissing == 0 && result.photosOnOriginal == 0
    if (!allPhotos) return kept
    return "${pluralStringResource(Res.plurals.restore_done_photos, result.photosRestored, result.photosRestored.grouped())} $kept"
}

@Composable
internal fun resultNotes(result: RestoreResult): List<DiaryNote> = if (result.fromLegacy) legacyNotes(result) else backupNotes(result)

@Composable
internal fun legacyNotes(result: RestoreResult): List<DiaryNote> = buildList {
    if (result.entriesKeptBoth > 0) {
        add(
            DiaryNote(
                DiaryIcons.Copy,
                pluralStringResource(Res.plurals.restore_legacy_same_time, result.entriesKeptBoth, result.entriesKeptBoth.grouped()),
                stringResource(Res.string.restore_legacy_same_time_body)
            )
        )
    }
    if (result.photosMissing > 0) {
        add(
            DiaryNote(
                DiaryIcons.ImageOff,
                pluralStringResource(Res.plurals.restore_legacy_gaps, result.photosMissing, result.photosMissing.grouped()),
                stringResource(Res.string.restore_legacy_gaps_body)
            )
        )
    }
}

@Composable
internal fun backupNotes(result: RestoreResult): List<DiaryNote> = buildList {
    if (result.entriesKeptBoth > 0) {
        add(
            DiaryNote(
                DiaryIcons.Copy,
                pluralStringResource(Res.plurals.restore_note_both, result.entriesKeptBoth, result.entriesKeptBoth.grouped()),
                stringResource(Res.string.restore_note_both_body)
            )
        )
    }
    if (result.photosOnOriginal > 0) {
        add(
            DiaryNote(
                DiaryIcons.Image,
                pluralStringResource(Res.plurals.restore_note_original, result.photosOnOriginal, result.photosOnOriginal.grouped()),
                stringResource(Res.string.restore_note_original_body)
            )
        )
    }
    if (result.photosPending > 0) {
        add(
            DiaryNote(
                DiaryIcons.Download,
                pluralStringResource(Res.plurals.restore_note_pending, result.photosPending, result.photosPending.grouped()),
                stringResource(pendingBody(result))
            )
        )
    }
    if (result.photosMissing > 0) {
        add(
            DiaryNote(
                DiaryIcons.ImageOff,
                pluralStringResource(Res.plurals.restore_note_missing, result.photosMissing, result.photosMissing.grouped()),
                stringResource(Res.string.restore_note_missing_body)
            )
        )
    }
}

/** 남은 사진마다 까닭과 할 일이 달라 나눠 말함 */
internal fun pendingBody(result: RestoreResult): StringResource = when {
    result.photosStop == PhotoStop.NO_SPACE -> Res.string.restore_note_pending_space_body
    result.fromFile && result.photosStop == null -> Res.string.restore_note_pending_file_body
    else -> Res.string.restore_note_pending_body
}

/**
 * Figma "B6 · 백업을 확인하지 못했어요".
 * 어떤 실패든 기기의 기록은 그대로.
 */
@Composable
internal fun ColumnScope.Failure(reason: RestoreFailure, onRetry: () -> Unit, onPickOther: () -> Unit) {
    val (title, body) =
        when (reason) {
            RestoreFailure.MISMATCH -> Res.string.restore_failed_mismatch_title to Res.string.restore_failed_mismatch_body
            RestoreFailure.NO_SPACE -> Res.string.restore_failed_space_title to Res.string.restore_failed_space_body
            RestoreFailure.NEEDS_UPDATE -> Res.string.restore_needs_update to Res.string.restore_failed_other_body
            RestoreFailure.BUSY -> Res.string.restore_failed_busy_title to Res.string.restore_failed_busy_body
            RestoreFailure.OTHER -> Res.string.restore_failed_other_title to Res.string.restore_failed_other_body
        }
    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(DiarySpacing.x10))
        Hero(
            DiaryIcons.CircleAlert,
            stringResource(title),
            stringResource(body),
            tone = DiaryBadgeTone.Danger,
            fill = false
        )
        DiarySettingsRow(stringResource(Res.string.restore_pick_other), onClick = onPickOther, leadingIcon = DiaryIcons.History)
        val openLink = rememberOpenLink()
        var noMail by rememberSaveable { mutableStateOf(false) }
        DiarySettingsRow(stringResource(Res.string.restore_contact), onClick = {
            // 메일 앱이 없으면 보낼 곳을 글로 알림
            noMail = !openLink(SUPPORT_MAIL)
        }, leadingIcon = DiaryIcons.Mail)
        if (noMail) {
            BasicText(
                stringResource(Res.string.restore_no_mail, SUPPORT_ADDRESS),
                style = DiaryTheme.typography.caption.copy(color = DiaryTheme.colors.textSecondary),
                modifier = Modifier.padding(horizontal = LocalScreenGutter.current, vertical = DiarySpacing.x2)
            )
        }
    }
    Bottom { DiaryButton(stringResource(Res.string.restore_retry), onRetry, Modifier.fillMaxWidth(), size = DiaryButtonSize.XLarge) }
}
