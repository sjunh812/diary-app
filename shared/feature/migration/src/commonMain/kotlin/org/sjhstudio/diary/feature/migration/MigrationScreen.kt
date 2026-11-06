package org.sjhstudio.diary.feature.migration

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.component.DiaryBadgeTone
import org.sjhstudio.diary.core.designsystem.component.DiaryButton
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonKind
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonSize
import org.sjhstudio.diary.core.designsystem.component.DiaryIndeterminateProgressBar
import org.sjhstudio.diary.core.designsystem.component.DiaryNote
import org.sjhstudio.diary.core.designsystem.component.DiaryNoteBox
import org.sjhstudio.diary.core.designsystem.component.DiaryProgressBar
import org.sjhstudio.diary.core.designsystem.component.DiaryResultHeader
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsRow
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.model.MigrationFailureKind
import org.sjhstudio.diary.core.model.MigrationOutcome
import org.sjhstudio.diary.core.ui.KeepScreenOn
import org.sjhstudio.diary.core.ui.formatBytes
import org.sjhstudio.diary.core.ui.grouped
import org.sjhstudio.diary.feature.migration.resources.Res
import org.sjhstudio.diary.feature.migration.resources.migration_check_again
import org.sjhstudio.diary.feature.migration.resources.migration_contact
import org.sjhstudio.diary.feature.migration.resources.migration_date_fixed_body
import org.sjhstudio.diary.feature.migration.resources.migration_date_fixed_title
import org.sjhstudio.diary.feature.migration.resources.migration_done_photos
import org.sjhstudio.diary.feature.migration.resources.migration_done_title
import org.sjhstudio.diary.feature.migration.resources.migration_export_original
import org.sjhstudio.diary.feature.migration.resources.migration_failed_body_mismatch
import org.sjhstudio.diary.feature.migration.resources.migration_failed_body_read
import org.sjhstudio.diary.feature.migration.resources.migration_failed_body_snapshot
import org.sjhstudio.diary.feature.migration.resources.migration_failed_body_unknown
import org.sjhstudio.diary.feature.migration.resources.migration_failed_body_write
import org.sjhstudio.diary.feature.migration.resources.migration_failed_title
import org.sjhstudio.diary.feature.migration.resources.migration_missing_photos_body
import org.sjhstudio.diary.feature.migration.resources.migration_missing_photos_body_short
import org.sjhstudio.diary.feature.migration.resources.migration_missing_photos_title
import org.sjhstudio.diary.feature.migration.resources.migration_no_space_body
import org.sjhstudio.diary.feature.migration.resources.migration_no_space_title
import org.sjhstudio.diary.feature.migration.resources.migration_partial_body
import org.sjhstudio.diary.feature.migration.resources.migration_partial_footnote
import org.sjhstudio.diary.feature.migration.resources.migration_partial_title
import org.sjhstudio.diary.feature.migration.resources.migration_preparing_body
import org.sjhstudio.diary.feature.migration.resources.migration_preparing_title
import org.sjhstudio.diary.feature.migration.resources.migration_progress_body
import org.sjhstudio.diary.feature.migration.resources.migration_progress_entries
import org.sjhstudio.diary.feature.migration.resources.migration_progress_entries_value
import org.sjhstudio.diary.feature.migration.resources.migration_progress_note
import org.sjhstudio.diary.feature.migration.resources.migration_progress_photos
import org.sjhstudio.diary.feature.migration.resources.migration_progress_photos_value
import org.sjhstudio.diary.feature.migration.resources.migration_progress_title
import org.sjhstudio.diary.feature.migration.resources.migration_retry
import org.sjhstudio.diary.feature.migration.resources.migration_skip
import org.sjhstudio.diary.feature.migration.resources.migration_start
import org.sjhstudio.diary.feature.migration.resources.migration_view_legacy

/**
 * 기록 이전 화면들(Figma "화면" M0·M1·M2·M2-b·M3).
 * 상태만 받아 그리는 함수다.
 * 아직 만들지 않은 동작(예전 일기 보기, 원본 내보내기, 문의하기)은 null이면 그 줄을 숨긴다.
 */
@Composable
fun MigrationScreen(
    state: MigrationUiState,
    onStart: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onSkip: () -> Unit = {},
    onViewLegacy: (() -> Unit)? = null,
    onExportOriginal: (() -> Unit)? = null,
    onContact: (() -> Unit)? = null
) {
    Box(modifier.fillMaxSize().background(DiaryTheme.colors.canvas)) {
        when (state) {
            MigrationUiState.Checking, MigrationUiState.Passed -> Unit

            MigrationUiState.Preparing ->
                Frame(
                    DiaryIcons.BookOpen,
                    stringResource(Res.string.migration_preparing_title),
                    stringResource(Res.string.migration_preparing_body)
                ) {
                    KeepScreenOn()
                    DiaryIndeterminateProgressBar()
                }

            is MigrationUiState.Copying -> CopyingContent(state)

            is MigrationUiState.Result -> ResultContent(state, onStart, onExportOriginal, onContact)

            is MigrationUiState.Failed -> FailedContent(state, onRetry, onSkip, onViewLegacy, onExportOriginal, onContact)
        }
    }
}

@Composable
private fun CopyingContent(state: MigrationUiState.Copying) {
    KeepScreenOn()
    Frame(
        icon = DiaryIcons.BookOpen,
        title = stringResource(Res.string.migration_progress_title),
        body = stringResource(Res.string.migration_progress_body),
        footnote = stringResource(Res.string.migration_progress_note)
    ) {
        val done = state.entriesDone + state.photosDone
        val total = (state.entriesTotal + state.photosTotal).coerceAtLeast(1)
        DiaryProgressBar(done.toFloat() / total)
        Spacer(Modifier.height(DiarySpacing.x4))
        CountRow(
            stringResource(Res.string.migration_progress_entries),
            stringResource(Res.string.migration_progress_entries_value, state.entriesDone.grouped(), state.entriesTotal.grouped())
        )
        if (state.photosTotal > 0) {
            CountRow(
                stringResource(Res.string.migration_progress_photos),
                stringResource(Res.string.migration_progress_photos_value, state.photosDone.grouped(), state.photosTotal.grouped())
            )
        }
    }
}

@Composable
private fun CountRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = DiarySpacing.x2), horizontalArrangement = Arrangement.SpaceBetween) {
        BasicText(label, style = DiaryTheme.typography.body.copy(color = DiaryTheme.colors.textSecondary))
        BasicText(value, style = DiaryTheme.typography.bodyStrong.copy(color = DiaryTheme.colors.textPrimary))
    }
}

@Composable
private fun ResultContent(state: MigrationUiState.Result, onStart: () -> Unit, onExportOriginal: (() -> Unit)?, onContact: (() -> Unit)?) {
    val report = state.report
    val partial = report.outcome == MigrationOutcome.PARTIAL
    val notes =
        buildList {
            if (report.photosMissing > 0) {
                add(
                    DiaryNote(
                        DiaryIcons.ImageOff,
                        pluralStringResource(
                            Res.plurals.migration_missing_photos_title,
                            report.photosMissing,
                            report.photosMissing.grouped()
                        ),
                        stringResource(
                            if (partial) Res.string.migration_missing_photos_body_short else Res.string.migration_missing_photos_body
                        )
                    )
                )
            }
            if (report.dateDiffers > 0 && !partial) {
                add(
                    DiaryNote(
                        DiaryIcons.CalendarCheck,
                        pluralStringResource(Res.plurals.migration_date_fixed_title, report.dateDiffers, report.dateDiffers.grouped()),
                        stringResource(Res.string.migration_date_fixed_body)
                    )
                )
            }
        }
    Frame(
        icon = if (partial) DiaryIcons.Info else DiaryIcons.CircleCheck,
        title =
            if (partial) {
                stringResource(Res.string.migration_partial_title)
            } else {
                pluralStringResource(Res.plurals.migration_done_title, report.entriesMigrated, report.entriesMigrated.grouped())
            },
        body =
            if (partial) {
                pluralStringResource(
                    Res.plurals.migration_partial_body,
                    report.entriesMigrated,
                    report.entriesMigrated.grouped(),
                    report.entriesUnreadable.grouped()
                )
            } else if (report.photosFound > 0) {
                pluralStringResource(Res.plurals.migration_done_photos, report.photosFound, report.photosFound.grouped())
            } else {
                null
            },
        footnote = if (partial) stringResource(Res.string.migration_partial_footnote) else null,
        secondaryActions =
            if (partial) {
                listOfNotNull(
                    onExportOriginal?.let { stringResource(Res.string.migration_export_original) to it },
                    onContact?.let { stringResource(Res.string.migration_contact) to it }
                )
            } else {
                emptyList()
            },
        primary = stringResource(Res.string.migration_start) to onStart
    ) {
        DiaryNoteBox(notes)
    }
}

@Composable
private fun FailedContent(
    state: MigrationUiState.Failed,
    onRetry: () -> Unit,
    onSkip: () -> Unit,
    onViewLegacy: (() -> Unit)?,
    onExportOriginal: (() -> Unit)?,
    onContact: (() -> Unit)?
) {
    val noSpace = state.kind == MigrationFailureKind.NO_SPACE
    val body =
        when (state.kind) {
            MigrationFailureKind.NO_SPACE -> stringResource(Res.string.migration_no_space_body, formatBytes(state.shortfallBytes ?: 0))
            MigrationFailureKind.MISMATCH -> stringResource(Res.string.migration_failed_body_mismatch)
            MigrationFailureKind.SNAPSHOT_IO -> stringResource(Res.string.migration_failed_body_snapshot)
            MigrationFailureKind.READ_ERROR -> stringResource(Res.string.migration_failed_body_read)
            MigrationFailureKind.WRITE_ERROR -> stringResource(Res.string.migration_failed_body_write)
            MigrationFailureKind.UNKNOWN -> stringResource(Res.string.migration_failed_body_unknown)
        }
    Frame(
        icon = DiaryIcons.CircleAlert,
        tone = DiaryBadgeTone.Danger,
        title = stringResource(if (noSpace) Res.string.migration_no_space_title else Res.string.migration_failed_title),
        body = body,
        // 공간 부족은 까닭이 분명해서 안내하고 멈춘다.
        // 그 밖에는 이유를 알 수 없으니 먼저 앱을 쓰게 한다.
        secondaryActions = if (noSpace) emptyList() else listOf(stringResource(Res.string.migration_skip) to onSkip),
        primary = stringResource(if (noSpace) Res.string.migration_check_again else Res.string.migration_retry) to onRetry,
        fullWidthContent = true
    ) {
        onViewLegacy?.let { DiarySettingsRow(stringResource(Res.string.migration_view_legacy), it, leadingIcon = DiaryIcons.BookOpen) }
        if (!noSpace) {
            onExportOriginal?.let {
                DiarySettingsRow(stringResource(Res.string.migration_export_original), it, leadingIcon = DiaryIcons.Download)
            }
            onContact?.let { DiarySettingsRow(stringResource(Res.string.migration_contact), it, leadingIcon = DiaryIcons.Mail) }
        }
    }
}

/**
 * 배지 · 제목 · 설명 · 내용 · (안내 글) · (글자 버튼) · 큰 버튼.
 * 넓은 화면에서는 가운데 폭 480.
 */
@Composable
private fun Frame(
    icon: ImageVector,
    title: String,
    body: String?,
    tone: DiaryBadgeTone = DiaryBadgeTone.Accent,
    footnote: String? = null,
    secondaryActions: List<Pair<String, () -> Unit>> = emptyList(),
    primary: Pair<String, () -> Unit>? = null,
    fullWidthContent: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = DiaryTheme.colors
    Column(
        modifier = Modifier.fillMaxSize().systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .widthIn(max = DiaryWriteSize.settingsMax)
                    .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 머리가 위아래 24를 스스로 두어서 그만큼 빼고, 배지와 내용의 자리는 전과 같게 둔다.
            Spacer(Modifier.height(DiarySpacing.x14 + DiarySpacing.x14 - DiarySpacing.x6))
            DiaryResultHeader(icon, title, description = body, tone = tone)
            Spacer(Modifier.height(DiarySpacing.x8 - DiarySpacing.x6))
            Column(
                Modifier.fillMaxWidth().padding(horizontal = if (fullWidthContent) DiarySpacing.x0_5 * 0 else DiarySpacing.screenCompact)
            ) {
                content()
            }
        }
        Column(
            modifier = Modifier.widthIn(max = DiaryWriteSize.settingsMax).padding(horizontal = DiarySpacing.screenCompact),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (footnote != null) {
                BasicText(footnote, style = DiaryTheme.typography.caption.copy(color = colors.textSecondary, textAlign = TextAlign.Center))
                Spacer(Modifier.height(DiarySpacing.x3))
            }
            if (secondaryActions.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2)) {
                    for ((label, action) in secondaryActions) {
                        DiaryButton(label, action, size = DiaryButtonSize.Medium, kind = DiaryButtonKind.Text)
                    }
                }
                Spacer(Modifier.height(DiarySpacing.x2))
            }
            if (primary != null) {
                DiaryButton(primary.first, primary.second, Modifier.fillMaxWidth(), size = DiaryButtonSize.XLarge)
            }
            Spacer(Modifier.height(DiarySpacing.x5))
        }
    }
}
