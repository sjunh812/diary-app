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

// 복원 화면에 쓰는 기기 이름·날짜·개수·크기 글

@Composable
internal fun deviceName(entry: BackupEntry): String = entry.deviceName.ifEmpty { stringResource(Res.string.restore_device_unknown) }

@Composable
internal fun deviceLabel(entry: BackupEntry): String = when (entry.platform) {
    PLATFORM_LEGACY -> stringResource(Res.string.restore_group_legacy)
    "android" -> stringResource(Res.string.restore_device_group, deviceName(entry), stringResource(Res.string.restore_platform_android))
    "ios" -> stringResource(Res.string.restore_device_group, deviceName(entry), stringResource(Res.string.restore_platform_ios))
    else -> deviceName(entry)
}

@Composable
internal fun whenText(entry: BackupEntry): String {
    val dates = rememberDiaryDateFormat()
    val local = entry.createdAt.toLocalDateTime(TimeZone.currentSystemDefault())
    return stringResource(Res.string.restore_when, dates.monthDay(local.date), dates.time(local.time))
}

@Composable
internal fun legacyDate(entry: BackupEntry): String =
    rememberDiaryDateFormat().fullDate(entry.createdAt.toLocalDateTime(TimeZone.currentSystemDefault()).date)

@Composable
internal fun countsText(entry: BackupEntry): String = stringResource(
    Res.string.restore_join,
    pluralStringResource(Res.plurals.restore_count_entries, entry.entryCount, entry.entryCount.grouped()),
    pluralStringResource(Res.plurals.restore_count_photos, entry.photoCount, entry.photoCount.grouped())
)

/** 받을 사진이 없으면 크기는 뜻이 없어 장수만 보인다. */
@Composable
internal fun photosToReceive(photos: Int, bytes: Long): String {
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

internal const val LOADING_DEVICES = 2
internal const val LOADING_VALUES = 4
internal const val LOADING_TITLE_WIDTH = 0.42f
internal const val LOADING_BODY_WIDTH = 0.3f
internal const val LOADING_HERO_WIDTH = 0.5f
internal const val SUPPORT_ADDRESS = "sjunh812@naver.com"
internal const val SUPPORT_MAIL = "mailto:$SUPPORT_ADDRESS"
