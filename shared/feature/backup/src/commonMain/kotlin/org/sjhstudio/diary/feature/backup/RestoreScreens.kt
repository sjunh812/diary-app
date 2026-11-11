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
import org.sjhstudio.diary.feature.backup.resources.restore_more_backups
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

/**
 * 백업에서 복원하기(Figma "백업에서 복원하기 (B3)")
 * 기기마다 묶어 보이고, 깨진 백업은 고를 수 없음
 */
@Composable
fun RestoreListRoute(onBack: () -> Unit, onPick: (String) -> Unit, modifier: Modifier = Modifier) {
    val viewModel = koinViewModel<RestoreListViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    RestoreListScreen(state, onPick, viewModel::load, onBack, modifier)
}

@Composable
fun RestoreListScreen(
    state: RestoreListUi,
    onPick: (String) -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Page(stringResource(Res.string.restore_row), onBack, modifier) {
        when (state) {
            RestoreListUi.Loading -> LoadingList()

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
                } else {
                    Entries(state.entries, onPick)
                }
        }
    }
}

@Composable
private fun Entries(entries: List<BackupEntry>, onPick: (String) -> Unit) {
    val colors = DiaryTheme.colors
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        BasicText(
            stringResource(Res.string.restore_keep_note),
            style = DiaryTheme.typography.caption.copy(color = colors.textSecondary),
            modifier = Modifier.padding(horizontal = LocalScreenGutter.current, vertical = DiarySpacing.x2)
        )
        val ordered = entries.sortedBy { it.platform == PLATFORM_LEGACY }
        // 보통 가장 최근 백업을 복원해 휴대폰마다 최근 것만 먼저 보임. 예전 앱 백업은 몇 개뿐이라 다 보임
        var opened by remember { mutableStateOf(emptySet<Pair<String, String>>()) }
        ordered.groupBy { it.platform to it.deviceName }.entries.forEachIndexed { index, (device, group) ->
            DiarySettingsHeader(deviceLabel(group.first()), gapAbove = index > 0)
            val all = device.first == PLATFORM_LEGACY || device in opened
            val shown = if (all) group else group.take(RECENT_PER_DEVICE)
            shown.forEach { entry ->
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
            val hidden = group.size - shown.size
            if (hidden > 0) {
                DiaryButton(
                    pluralStringResource(Res.plurals.restore_more_backups, hidden, hidden.grouped()),
                    { opened = opened + device },
                    // 버튼 안쪽 여백만큼 덜 띄워 글자를 위 줄과 맞춤
                    Modifier.padding(start = LocalScreenGutter.current - DiarySpacing.x4),
                    size = DiaryButtonSize.Medium,
                    kind = DiaryButtonKind.Text
                )
            }
        }
        Spacer(Modifier.height(DiarySpacing.x8))
    }
}

/**
 * 값 줄 묶음
 * 값 줄이 스스로 화면 여백을 둬 이미 여백 안이면 겹치지 않게 뺌
 */
@Composable
internal fun ValueGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    CompositionLocalProvider(LocalScreenGutter provides Dp.Hairline) { Column(modifier, content = content) }
}

/**
 * 고를 수 없는 백업
 * 자리는 남겨 왜 없는지 보임
 */
@Composable
private fun Unavailable(title: String, reason: String) {
    val colors = DiaryTheme.colors
    Column(
        Modifier.fillMaxWidth().padding(horizontal = LocalScreenGutter.current, vertical = DiarySpacing.x3),
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
 * 복원하기 전에 확인(Figma "복원하기 전에 확인 (B4)")
 * 합치기는 지금 일기를 지우지 않음
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
            null -> LoadingPreview()

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
        Column(Modifier.padding(horizontal = LocalScreenGutter.current), verticalArrangement = Arrangement.spacedBy(DiarySpacing.x4)) {
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
            // 지금 막는 것부터 보이고, 뜻이 다르면 아이콘도 다르게 둠
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
 * 예전 앱 백업(Figma B7)
 * 합치기로만 불러옴
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
        Column(Modifier.padding(horizontal = LocalScreenGutter.current), verticalArrangement = Arrangement.spacedBy(DiarySpacing.x4)) {
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
 * 백업 목록을 읽는 동안
 * 나올 목록처럼 기기 머리 아래에 두 줄짜리 백업 줄을 둠
 */
@Composable
private fun LoadingList() {
    DiarySkeleton {
        Column(Modifier.fillMaxWidth()) {
            repeat(LOADING_DEVICES) { device ->
                SkeletonLine(
                    Modifier
                        .padding(
                            start = LocalScreenGutter.current,
                            top = if (device == 0) DiarySpacing.x5 else DiarySpacing.x6,
                            bottom = DiarySpacing.x2
                        ).width(DiarySpacing.x14 + DiarySpacing.x10)
                )
                repeat(LOADING_DEVICES - device) {
                    Column(
                        Modifier.fillMaxWidth().padding(
                            horizontal = LocalScreenGutter.current,
                            vertical =
                                DiarySpacing.x3 + DiarySpacing.x0_5
                        ),
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
 * 고른 백업을 받아 보는 동안
 * 나올 확인 화면처럼 배지, 제목, 값 줄 넷, 안내 상자, 버튼 자리를 둠
 */
@Composable
private fun ColumnScope.LoadingPreview() {
    Column(
        Modifier.weight(1f).fillMaxWidth().padding(horizontal = LocalScreenGutter.current, vertical = DiarySpacing.x6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x2)
    ) {
        DiarySkeleton { SkeletonBlock(Modifier.size(DiarySpacing.x14), DiaryShapes.full) }
        Spacer(Modifier.height(DiarySpacing.x1))
        DiarySkeleton { SkeletonLine(Modifier.fillMaxWidth(LOADING_HERO_WIDTH), DiarySpacing.x5) }
        DiarySkeleton { SkeletonLine(Modifier.fillMaxWidth(LOADING_TITLE_WIDTH)) }
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
 * 큰 배지, 제목, 설명
 * [fill]이면 남은 자리 가운데에 둠
 */
@Composable
internal fun ColumnScope.Hero(
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
            .padding(horizontal = LocalScreenGutter.current, vertical = DiarySpacing.x6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x2, Alignment.CenterVertically)
    ) {
        DiaryHeroBadge(icon, tone = tone)
        Spacer(Modifier.height(DiarySpacing.x1))
        BasicText(
            title,
            style = DiaryTheme.typography.title2.copy(
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
                lineBreak = CenteredLineBreak
            )
        )
        body?.let {
            BasicText(
                it,
                style = DiaryTheme.typography.body.copy(
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    lineBreak = CenteredLineBreak
                )
            )
        }
    }
}

@Composable
internal fun Bottom(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = LocalScreenGutter.current, vertical = DiarySpacing.x3),
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x3),
        content = content
    )
}

/** 휴대폰마다 먼저 보이는 최근 백업 수 */
private const val RECENT_PER_DEVICE = 2
