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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
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
import org.sjhstudio.diary.core.ui.SkeletonBlock
import org.sjhstudio.diary.core.ui.SkeletonLine
import org.sjhstudio.diary.core.ui.byteSize
import org.sjhstudio.diary.core.ui.grouped
import org.sjhstudio.diary.core.ui.rememberDiaryDateFormat
import org.sjhstudio.diary.core.ui.rememberNotificationAccess
import org.sjhstudio.diary.core.ui.workContinuesInBackground
import org.sjhstudio.diary.feature.backup.resources.Res
import org.sjhstudio.diary.feature.backup.resources.file_restore
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
import org.sjhstudio.diary.feature.backup.resources.restore_group_other
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
import org.sjhstudio.diary.feature.backup.resources.restore_loading
import org.sjhstudio.diary.feature.backup.resources.restore_lock_action
import org.sjhstudio.diary.feature.backup.resources.restore_lock_body
import org.sjhstudio.diary.feature.backup.resources.restore_lock_title
import org.sjhstudio.diary.feature.backup.resources.restore_merge
import org.sjhstudio.diary.feature.backup.resources.restore_merging_body
import org.sjhstudio.diary.feature.backup.resources.restore_needs_update
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
import org.sjhstudio.diary.feature.backup.resources.restore_preparing
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

/**
 * 백업에서 복원하기(Figma "백업에서 복원하기 (B3)").
 * 기기마다 묶어 보이고, 깨진 백업은 고를 수 없다.
 */
@Composable
fun RestoreListRoute(onBack: () -> Unit, onPick: (String) -> Unit, onFile: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel = koinViewModel<RestoreListViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    RestoreListScreen(state, onPick, viewModel::load, onBack, modifier, onFile)
}

@Composable
fun RestoreListScreen(
    state: RestoreListUi,
    onPick: (String) -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onFile: () -> Unit = {}
) {
    Page(stringResource(Res.string.restore_row), onBack, modifier) {
        when (state) {
            RestoreListUi.Loading -> LoadingList(stringResource(Res.string.restore_loading))

            RestoreListUi.Failed -> {
                Hero(
                    DiaryIcons.CircleAlert,
                    stringResource(Res.string.restore_list_failed),
                    stringResource(Res.string.restore_list_failed_body)
                )
                Bottom {
                    DiaryButton(stringResource(Res.string.restore_retry), onRetry, Modifier.fillMaxWidth(), size = DiaryButtonSize.XLarge)
                }
            }

            is RestoreListUi.Loaded ->
                if (state.entries.isEmpty()) {
                    Hero(DiaryIcons.History, stringResource(Res.string.restore_empty_title), stringResource(Res.string.restore_empty_body))
                    DiarySettingsRow(stringResource(Res.string.file_restore), onClick = onFile, leadingIcon = DiaryIcons.Download)
                    Spacer(Modifier.height(DiarySpacing.x8))
                } else {
                    Entries(state.entries, onPick, onFile)
                }
        }
    }
}

@Composable
private fun Entries(entries: List<BackupEntry>, onPick: (String) -> Unit, onFile: () -> Unit) {
    val colors = DiaryTheme.colors
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        BasicText(
            stringResource(Res.string.restore_keep_note),
            style = DiaryTheme.typography.caption.copy(color = colors.textSecondary),
            modifier = Modifier.padding(horizontal = DiarySpacing.x5, vertical = DiarySpacing.x2)
        )
        val ordered = entries.sortedBy { it.platform == PLATFORM_LEGACY }
        ordered.groupBy { it.deviceId to it.deviceName }.values.forEachIndexed { index, group ->
            DiarySettingsHeader(deviceLabel(group.first()), gapAbove = index > 0)
            group.forEach { entry ->
                val problem = problemText(entry.state)
                if (entry.platform == PLATFORM_LEGACY) {
                    DiarySettingsRow(
                        legacyDate(entry),
                        onClick = { onPick(entry.snapshotName) },
                        description = stringResource(Res.string.restore_legacy_row)
                    )
                } else if (problem == null) {
                    DiarySettingsRow(whenText(entry), onClick = { onPick(entry.snapshotName) }, description = countsText(entry))
                } else {
                    Unavailable(whenText(entry), problem)
                }
            }
        }
        DiarySettingsHeader(stringResource(Res.string.restore_group_other), gapAbove = true)
        DiarySettingsRow(stringResource(Res.string.file_restore), onClick = onFile, leadingIcon = DiaryIcons.Download)
        Spacer(Modifier.height(DiarySpacing.x8))
    }
}

/**
 * 값 줄 묶음.
 * 값 줄이 스스로 화면 여백을 두므로 이미 여백 안에 있을 때는 겹치지 않게 뺀다.
 */
@Composable
private fun ValueGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    CompositionLocalProvider(LocalScreenGutter provides Dp.Hairline) { Column(modifier, content = content) }
}

/**
 * 고를 수 없는 백업.
 * 자리는 남겨서 왜 없는지 보인다.
 */
@Composable
private fun Unavailable(title: String, reason: String) {
    val colors = DiaryTheme.colors
    Column(
        Modifier.fillMaxWidth().padding(horizontal = DiarySpacing.x5, vertical = DiarySpacing.x3),
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x1)
    ) {
        BasicText(title, style = DiaryTheme.typography.reading.copy(color = colors.textSecondary))
        BasicText(reason, style = DiaryTheme.typography.caption.copy(color = colors.textSecondary))
    }
}

@Composable
private fun problemText(state: BackupEntryState): String? = when (state) {
    BackupEntryState.Ready -> null
    BackupEntryState.NeedsNewerApp -> stringResource(Res.string.restore_needs_update)
    is BackupEntryState.Damaged -> stringResource(Res.string.restore_damaged)
}

/**
 * 복원하기 전에 확인(Figma "복원하기 전에 확인 (B4)").
 * 합치기는 지금 일기를 지우지 않는다.
 */
@Composable
fun RestorePreviewRoute(snapshotName: String, onBack: () -> Unit, onMerged: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel = koinViewModel<RestorePreviewViewModel>(key = snapshotName) { parametersOf(snapshotName) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    RestorePreviewScreen(state, {
        viewModel.merge()
        onMerged()
    }, viewModel::load, onBack, modifier, onReplace = {
        viewModel.replace()
        onMerged()
    })
}

@Composable
fun RestorePreviewScreen(
    state: RestorePreparation?,
    onMerge: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onReplace: () -> Unit = {}
) {
    Page(stringResource(Res.string.restore_preview_top), onBack, modifier) {
        when (state) {
            null -> LoadingPreview(stringResource(Res.string.restore_preparing))

            is RestorePreparation.Failed -> Failure(state.reason, onRetry, onBack)

            is RestorePreparation.Ready ->
                if (state.entry.platform == PLATFORM_LEGACY) {
                    LegacyPreview(state.entry, state.preview, onMerge)
                } else {
                    Preview(state.entry, state.preview, onMerge, onReplace)
                }
        }
    }
}

@Composable
private fun ColumnScope.Preview(entry: BackupEntry, preview: RestorePreview, onMerge: () -> Unit, onReplace: () -> Unit) {
    val lacking = preview.bytesToDownload - preview.deviceBytesFree
    var asking by remember { mutableStateOf(false) }
    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
        Hero(
            DiaryIcons.Restore,
            stringResource(Res.string.restore_preview_title),
            stringResource(Res.string.restore_preview_body, deviceName(entry), whenText(entry)),
            fill = false
        )
        Column(Modifier.padding(horizontal = DiarySpacing.screenCompact), verticalArrangement = Arrangement.spacedBy(DiarySpacing.x4)) {
            ValueGroup {
                val entries = Res.plurals.restore_unit_entries
                DiaryValueRow(
                    stringResource(Res.string.restore_value_added),
                    pluralStringResource(entries, preview.entriesAdded, preview.entriesAdded.grouped())
                )
                DiaryValueRow(
                    stringResource(Res.string.restore_value_updated),
                    pluralStringResource(entries, preview.entriesUpdated, preview.entriesUpdated.grouped())
                )
                DiaryValueRow(
                    stringResource(Res.string.restore_value_both),
                    pluralStringResource(entries, preview.entriesKeptBoth, preview.entriesKeptBoth.grouped())
                )
                DiaryValueRow(
                    stringResource(Res.string.restore_value_photos),
                    photosToReceive(preview.photosToDownload, preview.bytesToDownload)
                )
            }
            // 지금 막는 것부터 보이고, 뜻이 다르면 아이콘도 다르게 둔다.
            if (lacking > 0) DiaryNotice(stringResource(Res.string.restore_no_space, sizeText(lacking)), icon = DiaryIcons.CircleAlert)
            if (preview.photosNotInBackup > 0) {
                DiaryNotice(
                    pluralStringResource(Res.plurals.restore_not_in_backup, preview.photosNotInBackup, preview.photosNotInBackup.grouped()),
                    icon = DiaryIcons.ImageOff
                )
            }
            DiaryNotice(stringResource(Res.string.restore_settings_note))
        }
    }
    Bottom {
        if (preview.entriesHere > 0) {
            DiaryButton(
                stringResource(Res.string.restore_replace_entry),
                { asking = true },
                Modifier.fillMaxWidth(),
                kind = DiaryButtonKind.Text,
                enabled = lacking <= 0
            )
        }
        DiaryButton(
            stringResource(Res.string.restore_merge),
            onMerge,
            Modifier.fillMaxWidth(),
            size = DiaryButtonSize.XLarge,
            enabled = lacking <= 0
        )
    }
    if (asking) {
        DiaryDialog(
            title = stringResource(Res.string.restore_replace_title),
            confirmLabel = stringResource(Res.string.restore_replace_action),
            onConfirm = {
                asking = false
                onReplace()
            },
            onDismiss = { asking = false },
            message = pluralStringResource(Res.plurals.restore_replace_body, preview.entriesHere, preview.entriesHere.grouped()),
            destructive = true
        )
    }
}

/**
 * 예전 앱 백업(Figma B7).
 * 합치기로만 불러온다.
 */
@Composable
private fun ColumnScope.LegacyPreview(entry: BackupEntry, preview: RestorePreview, onImport: () -> Unit) {
    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
        Hero(
            DiaryIcons.Restore,
            stringResource(Res.string.restore_legacy_title),
            stringResource(Res.string.restore_legacy_body, legacyDate(entry)),
            fill = false
        )
        Column(Modifier.padding(horizontal = DiarySpacing.screenCompact), verticalArrangement = Arrangement.spacedBy(DiarySpacing.x4)) {
            ValueGroup {
                val entries = Res.plurals.restore_unit_entries
                DiaryValueRow(
                    stringResource(Res.string.restore_value_added),
                    pluralStringResource(entries, preview.entriesAdded, preview.entriesAdded.grouped())
                )
                DiaryValueRow(
                    stringResource(Res.string.restore_value_existing),
                    pluralStringResource(entries, preview.entriesUnchanged, preview.entriesUnchanged.grouped())
                )
                if (preview.entriesKeptBoth > 0) {
                    DiaryValueRow(
                        stringResource(Res.string.restore_value_both),
                        pluralStringResource(entries, preview.entriesKeptBoth, preview.entriesKeptBoth.grouped())
                    )
                }
            }
            DiaryNoteBox(
                listOf(
                    DiaryNote(
                        DiaryIcons.ImageOff,
                        stringResource(Res.string.restore_legacy_no_photos),
                        stringResource(Res.string.restore_legacy_no_photos_body)
                    ),
                    DiaryNote(DiaryIcons.Info, stringResource(Res.string.restore_legacy_recent), null)
                )
            )
        }
    }
    Bottom {
        DiaryButton(stringResource(Res.string.restore_legacy_action), onImport, Modifier.fillMaxWidth(), size = DiaryButtonSize.XLarge)
    }
}

/**
 * 복원하는 중과 결과(Figma B5, B6).
 * 뒤로 가면 멈추지 않고 홈으로 간다.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RestoreProgressRoute(onHome: () -> Unit, onLock: () -> Unit, onPickOther: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel = koinViewModel<RestoreProgressViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val finish = {
        viewModel.dismiss()
        onHome()
    }
    val access = rememberNotificationAccess()
    // 사진을 받는 동안 앱을 나가도 끝나면 알릴 수 있게 시작할 때 한 번 묻는다.
    // 거절해도 복원은 그대로 돈다.
    LaunchedEffect(access) { access.request {} }
    LaunchedEffect(state) { if (state == RestoreState.Idle) onHome() }
    BackHandler(onBack = if (state is RestoreState.Done) finish else onHome)
    RestoreProgressScreen(state, onHome, finish, onLock, viewModel::retry, onPickOther, modifier)
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
        Column(Modifier.widthIn(max = DiaryWriteSize.bodyMax).fillMaxSize()) {
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
private fun ColumnScope.Running(photos: RestoreState.FetchingPhotos?, onSeeEntries: () -> Unit, body: StringResource) {
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
            ValueGroup(Modifier.padding(horizontal = DiarySpacing.screenCompact)) {
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
    Bottom {
        BasicText(
            stringResource(if (workContinuesInBackground) Res.string.restore_running_note else Res.string.restore_running_note_open),
            style = DiaryTheme.typography.caption.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
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
private fun ColumnScope.Result(result: RestoreResult, onLock: () -> Unit, onStart: () -> Unit) {
    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(DiarySpacing.x10))
        Hero(DiaryIcons.CircleCheck, resultTitle(result), resultBody(result), fill = false)
        Column(Modifier.padding(horizontal = DiarySpacing.screenCompact), verticalArrangement = Arrangement.spacedBy(DiarySpacing.x3)) {
            DiaryNoteBox(resultNotes(result))
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
    Bottom { DiaryButton(stringResource(Res.string.restore_start), onStart, Modifier.fillMaxWidth(), size = DiaryButtonSize.XLarge) }
}

@Composable
private fun resultTitle(result: RestoreResult): String = when {
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
private fun resultBody(result: RestoreResult): String {
    val kept = stringResource(if (result.replaced) Res.string.restore_replaced_body else Res.string.restore_done_body)
    val allPhotos = result.photosRestored > 0 && result.photosPending == 0 && result.photosMissing == 0 && result.photosOnOriginal == 0
    if (!allPhotos) return kept
    return "${pluralStringResource(Res.plurals.restore_done_photos, result.photosRestored, result.photosRestored.grouped())} $kept"
}

@Composable
private fun resultNotes(result: RestoreResult): List<DiaryNote> = if (result.fromLegacy) legacyNotes(result) else backupNotes(result)

@Composable
private fun legacyNotes(result: RestoreResult): List<DiaryNote> = buildList {
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
private fun backupNotes(result: RestoreResult): List<DiaryNote> = buildList {
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

/** 남은 사진마다 까닭과 할 일이 달라서 나눠 말한다. */
private fun pendingBody(result: RestoreResult): StringResource = when {
    result.photosStop == PhotoStop.NO_SPACE -> Res.string.restore_note_pending_space_body
    result.fromFile && result.photosStop == null -> Res.string.restore_note_pending_file_body
    else -> Res.string.restore_note_pending_body
}

/**
 * Figma "B6 · 백업을 확인하지 못했어요".
 * 어떤 실패든 기기의 기록은 그대로다.
 */
@Composable
private fun ColumnScope.Failure(reason: RestoreFailure, onRetry: () -> Unit, onPickOther: () -> Unit) {
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
        val links = LocalUriHandler.current
        DiarySettingsRow(stringResource(Res.string.restore_contact), onClick = {
            links.openUri(SUPPORT_MAIL)
        }, leadingIcon = DiaryIcons.Mail)
    }
    Bottom { DiaryButton(stringResource(Res.string.restore_retry), onRetry, Modifier.fillMaxWidth(), size = DiaryButtonSize.XLarge) }
}

@Composable
private fun Page(title: String, onBack: () -> Unit, modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxSize().background(DiaryTheme.colors.canvas).statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DiaryTopBar(title, onBack = onBack)
        Column(Modifier.widthIn(max = DiaryWriteSize.settingsMax).fillMaxWidth().weight(1f), content = content)
    }
}

/**
 * 백업 목록을 읽는 동안.
 * 나올 목록처럼 기기 머리 아래에 두 줄짜리 백업 줄을 둔다.
 */
@Composable
private fun LoadingList(label: String) {
    BasicText(
        label,
        Modifier.padding(horizontal = DiarySpacing.x5, vertical = DiarySpacing.x2),
        style = DiaryTheme.typography.caption.copy(color = DiaryTheme.colors.textSecondary)
    )
    DiarySkeleton {
        Column(Modifier.fillMaxWidth()) {
            repeat(LOADING_DEVICES) { device ->
                SkeletonLine(
                    Modifier
                        .padding(
                            start = DiarySpacing.x5,
                            top = if (device == 0) DiarySpacing.x5 else DiarySpacing.x6,
                            bottom = DiarySpacing.x2
                        ).width(DiarySpacing.x14 + DiarySpacing.x10)
                )
                repeat(LOADING_DEVICES - device) {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = DiarySpacing.x5, vertical = DiarySpacing.x3 + DiarySpacing.x0_5),
                        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x2)
                    ) {
                        SkeletonLine(Modifier.fillMaxWidth(LOADING_TITLE_WIDTH), DiarySpacing.x4)
                        SkeletonLine(Modifier.fillMaxWidth(LOADING_BODY_WIDTH))
                    }
                }
            }
        }
    }
}

/**
 * 고른 백업을 받아 보는 동안.
 * 나올 확인 화면처럼 배지, 제목, 값 줄 넷, 안내 상자, 버튼 자리를 둔다.
 */
@Composable
private fun ColumnScope.LoadingPreview(label: String) {
    val colors = DiaryTheme.colors
    Column(
        Modifier.weight(1f).fillMaxWidth().padding(horizontal = DiarySpacing.screenCompact, vertical = DiarySpacing.x6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x2)
    ) {
        DiarySkeleton { SkeletonBlock(Modifier.size(DiarySpacing.x14), DiaryShapes.full) }
        Spacer(Modifier.height(DiarySpacing.x1))
        DiarySkeleton { SkeletonLine(Modifier.fillMaxWidth(LOADING_HERO_WIDTH), DiarySpacing.x5) }
        BasicText(label, style = DiaryTheme.typography.body.copy(color = colors.textSecondary, textAlign = TextAlign.Center))
        Spacer(Modifier.height(DiarySpacing.x6))
        DiarySkeleton {
            Column(verticalArrangement = Arrangement.spacedBy(DiarySpacing.x4)) {
                repeat(LOADING_VALUES) {
                    Row(Modifier.fillMaxWidth().padding(vertical = DiarySpacing.x2), verticalAlignment = Alignment.CenterVertically) {
                        SkeletonLine(Modifier.fillMaxWidth(LOADING_TITLE_WIDTH))
                        Spacer(Modifier.weight(1f))
                        SkeletonLine(Modifier.width(DiarySpacing.x10))
                    }
                }
                SkeletonBlock(Modifier.fillMaxWidth().height(DiarySpacing.x14 + DiarySpacing.x6), DiaryShapes.r16)
            }
        }
    }
    Bottom { DiarySkeleton { SkeletonBlock(Modifier.fillMaxWidth().height(DiarySpacing.x14), DiaryShapes.r16) } }
}

/**
 * 큰 배지, 제목, 설명.
 * [fill]이면 남은 자리 가운데에 둔다.
 */
@Composable
private fun ColumnScope.Hero(
    icon: ImageVector,
    title: String,
    body: String? = null,
    tone: DiaryBadgeTone = DiaryBadgeTone.Accent,
    fill: Boolean = true
) {
    val colors = DiaryTheme.colors
    Column(
        (if (fill) Modifier.weight(1f) else Modifier)
            .fillMaxWidth()
            .padding(horizontal = DiarySpacing.screenCompact, vertical = DiarySpacing.x6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x2, Alignment.CenterVertically)
    ) {
        DiaryHeroBadge(icon, tone = tone)
        Spacer(Modifier.height(DiarySpacing.x1))
        BasicText(title, style = DiaryTheme.typography.title2.copy(color = colors.textPrimary, textAlign = TextAlign.Center))
        body?.let { BasicText(it, style = DiaryTheme.typography.body.copy(color = colors.textSecondary, textAlign = TextAlign.Center)) }
    }
}

@Composable
private fun Bottom(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = DiarySpacing.screenCompact, vertical = DiarySpacing.x3),
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x3),
        content = content
    )
}

@Composable
internal fun deviceName(entry: BackupEntry): String = entry.deviceName.ifEmpty { stringResource(Res.string.restore_device_unknown) }

@Composable
private fun deviceLabel(entry: BackupEntry): String {
    val platform =
        when (entry.platform) {
            PLATFORM_LEGACY -> return stringResource(Res.string.restore_group_legacy)
            "android" -> stringResource(Res.string.restore_platform_android)
            "ios" -> stringResource(Res.string.restore_platform_ios)
            else -> return deviceName(entry)
        }
    return stringResource(Res.string.restore_device_group, deviceName(entry), platform)
}

@Composable
internal fun whenText(entry: BackupEntry): String {
    val dates = rememberDiaryDateFormat()
    val local = entry.createdAt.toLocalDateTime(TimeZone.currentSystemDefault())
    return stringResource(Res.string.restore_when, dates.monthDay(local.date), dates.time(local.time))
}

@Composable
private fun legacyDate(entry: BackupEntry): String =
    rememberDiaryDateFormat().fullDate(entry.createdAt.toLocalDateTime(TimeZone.currentSystemDefault()).date)

@Composable
internal fun countsText(entry: BackupEntry): String = stringResource(
    Res.string.restore_join,
    pluralStringResource(Res.plurals.restore_count_entries, entry.entryCount, entry.entryCount.grouped()),
    pluralStringResource(Res.plurals.restore_count_photos, entry.photoCount, entry.photoCount.grouped())
)

/** 받을 사진이 없으면 크기는 뜻이 없어 장수만 보인다. */
@Composable
private fun photosToReceive(photos: Int, bytes: Long): String {
    val count = pluralStringResource(Res.plurals.restore_unit_photos, photos, photos.grouped())
    return if (photos == 0) count else stringResource(Res.string.restore_join, count, sizeText(bytes))
}

@Composable
internal fun sizeText(bytes: Long): String {
    val size = byteSize(bytes)
    return when (size.unit) {
        ByteUnit.KB -> stringResource(Res.string.restore_size_kb, size.value)
        ByteUnit.MB -> stringResource(Res.string.restore_size_mb, size.value)
        ByteUnit.GB -> stringResource(Res.string.restore_size_gb, size.value)
    }
}

private const val LOADING_DEVICES = 2
private const val LOADING_VALUES = 4
private const val LOADING_TITLE_WIDTH = 0.42f
private const val LOADING_BODY_WIDTH = 0.3f
private const val LOADING_HERO_WIDTH = 0.5f
private const val SUPPORT_MAIL = "mailto:sjunh812@naver.com"
