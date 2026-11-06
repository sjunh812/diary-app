package org.sjhstudio.diary.feature.settings

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
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.component.DiaryNotice
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsRow
import org.sjhstudio.diary.core.designsystem.component.DiaryStatusBox
import org.sjhstudio.diary.core.designsystem.component.DiaryTopBar
import org.sjhstudio.diary.core.designsystem.component.DiaryValueRow
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.designsystem.theme.LocalScreenGutter
import org.sjhstudio.diary.core.ui.grouped
import org.sjhstudio.diary.core.ui.rememberDiaryDateFormat
import org.sjhstudio.diary.feature.settings.resources.Res
import org.sjhstudio.diary.feature.settings.resources.migration_deferred_body
import org.sjhstudio.diary.feature.settings.resources.migration_deferred_failed
import org.sjhstudio.diary.feature.settings.resources.migration_deferred_retry
import org.sjhstudio.diary.feature.settings.resources.migration_deferred_running
import org.sjhstudio.diary.feature.settings.resources.migration_deferred_title
import org.sjhstudio.diary.feature.settings.resources.migration_report_contact
import org.sjhstudio.diary.feature.settings.resources.migration_report_date_fixed
import org.sjhstudio.diary.feature.settings.resources.migration_report_entries
import org.sjhstudio.diary.feature.settings.resources.migration_report_entry_count
import org.sjhstudio.diary.feature.settings.resources.migration_report_export
import org.sjhstudio.diary.feature.settings.resources.migration_report_missing_photos
import org.sjhstudio.diary.feature.settings.resources.migration_report_notice
import org.sjhstudio.diary.feature.settings.resources.migration_report_photo_count
import org.sjhstudio.diary.feature.settings.resources.migration_report_photos
import org.sjhstudio.diary.feature.settings.resources.migration_report_title
import org.sjhstudio.diary.feature.settings.resources.migration_report_unreadable
import org.sjhstudio.diary.feature.settings.resources.migration_report_when

/**
 * 설정 > 기록 이전 결과(Figma M5 · 일부만).
 * 옮긴 날, 개수 다섯 줄, 일부만 옮겨진 까닭, 원본 내보내기와 문의하기.
 * 내보내기와 문의하기는 기록 이전 기능이 만들어 앱 틀이 넘겨준다.
 * 없는 플랫폼(iOS)에서는 줄을 숨긴다.
 */
@Composable
fun MigrationReportScreen(
    state: MigrationReportUiState,
    onBack: () -> Unit,
    onExportOriginal: (() -> Unit)?,
    onContact: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {}
) {
    val colors = DiaryTheme.colors
    val dates = rememberDiaryDateFormat()
    Column(modifier.fillMaxSize().background(colors.canvas).systemBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        DiaryTopBar(title = stringResource(Res.string.migration_report_title), onBack = onBack)
        Column(Modifier.widthIn(max = DiaryWriteSize.settingsMax).fillMaxWidth().verticalScroll(rememberScrollState())) {
            if (state.deferred) {
                DiaryStatusBox(
                    icon = DiaryIcons.Info,
                    title = stringResource(Res.string.migration_deferred_title),
                    body =
                        stringResource(
                            if (state.retry == MigrationRetry.Failed) {
                                Res.string.migration_deferred_failed
                            } else {
                                Res.string.migration_deferred_body
                            }
                        ),
                    modifier = Modifier.padding(horizontal = LocalScreenGutter.current, vertical = DiarySpacing.x4),
                    actionLabel =
                        stringResource(
                            if (state.retry == MigrationRetry.Running) {
                                Res.string.migration_deferred_running
                            } else {
                                Res.string.migration_deferred_retry
                            }
                        ),
                    onAction = onRetry.takeIf { state.retry != MigrationRetry.Running }
                )
                onExportOriginal?.let {
                    DiarySettingsRow(stringResource(Res.string.migration_report_export), it, leadingIcon = DiaryIcons.Download)
                }
                onContact?.let { DiarySettingsRow(stringResource(Res.string.migration_report_contact), it, leadingIcon = DiaryIcons.Mail) }
                return@Column
            }
            val report = state.report ?: return@Column
            state.migratedOn?.let { day ->
                BasicText(
                    stringResource(Res.string.migration_report_when, dates.fullDate(day)),
                    style = DiaryTheme.typography.caption.copy(color = colors.textSecondary),
                    modifier =
                        Modifier.padding(
                            start = LocalScreenGutter.current,
                            end = LocalScreenGutter.current,
                            top = DiarySpacing.x1,
                            bottom = DiarySpacing.x2
                        )
                )
            }
            DiaryValueRow(stringResource(Res.string.migration_report_entries), entries(report.entriesMigrated))
            DiaryValueRow(stringResource(Res.string.migration_report_photos), photos(report.photosFound))
            DiaryValueRow(stringResource(Res.string.migration_report_missing_photos), photos(report.photosMissing))
            DiaryValueRow(stringResource(Res.string.migration_report_date_fixed), entries(report.dateDiffers))
            DiaryValueRow(stringResource(Res.string.migration_report_unreadable), entries(report.entriesUnreadable))
            if (report.entriesUnreadable > 0) {
                DiaryNotice(
                    pluralStringResource(Res.plurals.migration_report_notice, report.entriesUnreadable, report.entriesUnreadable.grouped()),
                    Modifier.padding(horizontal = LocalScreenGutter.current, vertical = DiarySpacing.x4),
                    icon = DiaryIcons.ShieldCheck
                )
            }
            onExportOriginal?.let {
                DiarySettingsRow(stringResource(Res.string.migration_report_export), it, leadingIcon = DiaryIcons.Download)
            }
            onContact?.let { DiarySettingsRow(stringResource(Res.string.migration_report_contact), it, leadingIcon = DiaryIcons.Mail) }
        }
    }
}

@Composable
private fun entries(count: Int): String = pluralStringResource(Res.plurals.migration_report_entry_count, count, count.grouped())

@Composable
private fun photos(count: Int): String = pluralStringResource(Res.plurals.migration_report_photo_count, count, count.grouped())
