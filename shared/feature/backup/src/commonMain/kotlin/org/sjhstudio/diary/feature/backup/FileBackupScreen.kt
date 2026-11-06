package org.sjhstudio.diary.feature.backup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.sink
import io.github.vinceglb.filekit.source
import kotlinx.coroutines.launch
import okio.buffer
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.sjhstudio.diary.core.designsystem.component.DiaryBadgeTone
import org.sjhstudio.diary.core.designsystem.component.DiaryButton
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonSize
import org.sjhstudio.diary.core.designsystem.component.DiaryHeroBadge
import org.sjhstudio.diary.core.designsystem.component.DiaryNote
import org.sjhstudio.diary.core.designsystem.component.DiaryNoteBox
import org.sjhstudio.diary.core.designsystem.component.DiaryNotice
import org.sjhstudio.diary.core.designsystem.component.DiaryProgressBar
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsRow
import org.sjhstudio.diary.core.designsystem.component.DiaryTopBar
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.model.BundleEstimate
import org.sjhstudio.diary.core.model.RestoreFailure
import org.sjhstudio.diary.core.model.currentDate
import org.sjhstudio.diary.core.ui.grouped
import org.sjhstudio.diary.feature.backup.resources.Res
import org.sjhstudio.diary.feature.backup.resources.file_action
import org.sjhstudio.diary.feature.backup.resources.file_body
import org.sjhstudio.diary.feature.backup.resources.file_body_empty
import org.sjhstudio.diary.feature.backup.resources.file_done
import org.sjhstudio.diary.feature.backup.resources.file_move_body
import org.sjhstudio.diary.feature.backup.resources.file_move_title
import org.sjhstudio.diary.feature.backup.resources.file_name
import org.sjhstudio.diary.feature.backup.resources.file_place_body
import org.sjhstudio.diary.feature.backup.resources.file_place_title
import org.sjhstudio.diary.feature.backup.resources.file_read_failed
import org.sjhstudio.diary.feature.backup.resources.file_read_needs_update
import org.sjhstudio.diary.feature.backup.resources.file_reading
import org.sjhstudio.diary.feature.backup.resources.file_restore
import org.sjhstudio.diary.feature.backup.resources.file_restore_body
import org.sjhstudio.diary.feature.backup.resources.file_saved_body
import org.sjhstudio.diary.feature.backup.resources.file_saved_title
import org.sjhstudio.diary.feature.backup.resources.file_title
import org.sjhstudio.diary.feature.backup.resources.file_top
import org.sjhstudio.diary.feature.backup.resources.file_trust_body
import org.sjhstudio.diary.feature.backup.resources.file_trust_title
import org.sjhstudio.diary.feature.backup.resources.file_write_failed
import org.sjhstudio.diary.feature.backup.resources.file_writing
import org.sjhstudio.diary.feature.backup.resources.restore_count_entries
import org.sjhstudio.diary.feature.backup.resources.restore_count_photos

/** 파일로 백업하기(Figma "파일로 백업하기 (B8)"). 저장 위치는 사용자가 시스템 창에서 고른다(FR-031). */
@Composable
fun FileBackupRoute(onBack: () -> Unit, onPreview: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel = koinViewModel<FileBackupViewModel>()
    val estimate by viewModel.estimate.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val saver =
        rememberFileSaverLauncher(FileKitDialogSettings.createDefault()) { file: PlatformFile? ->
            file?.let { target -> viewModel.write(target.name) { OkioSink(target.sink()).buffer() } }
        }
    val picker =
        rememberFilePickerLauncher(type = FileKitType.File()) { file: PlatformFile? ->
            file?.let { picked -> viewModel.read({ OkioSource(picked.source()) }, onPreview) }
        }
    FileBackupScreen(
        estimate,
        state,
        onMake = {
            scope.launch {
                val today = currentDate()
                saver.launch(suggestedName = getString(Res.string.file_name, today.toString()), defaultExtension = "zip")
            }
        },
        onRestore = { picker.launch() },
        onDone = viewModel::clear,
        onBack = onBack,
        modifier = modifier
    )
}

@Composable
fun FileBackupScreen(
    estimate: BundleEstimate?,
    state: FileBackupUi,
    onMake: () -> Unit,
    onRestore: () -> Unit,
    onDone: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DiaryTheme.colors
    Column(modifier.fillMaxSize().background(colors.canvas).statusBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        DiaryTopBar(stringResource(Res.string.file_top), onBack = onBack)
        Column(Modifier.widthIn(max = DiaryWriteSize.bodyMax).fillMaxWidth().weight(1f)) {
            when (state) {
                is FileBackupUi.Saved -> Saved(state, onDone)
                else -> Start(estimate, state, onMake, onRestore)
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.Start(
    estimate: BundleEstimate?,
    state: FileBackupUi,
    onMake: () -> Unit,
    onRestore: () -> Unit
) {
    val busy = state is FileBackupUi.Writing || state == FileBackupUi.Reading
    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
        Heading(DiaryIcons.Download, stringResource(Res.string.file_title), estimate?.let { bodyOf(it) }.orEmpty())
        Column(Modifier.padding(horizontal = DiarySpacing.screenCompact), verticalArrangement = Arrangement.spacedBy(DiarySpacing.x3)) {
            DiaryNoteBox(
                listOf(
                    DiaryNote(DiaryIcons.Download, stringResource(Res.string.file_place_title), stringResource(Res.string.file_place_body)),
                    DiaryNote(DiaryIcons.Copy, stringResource(Res.string.file_move_title), stringResource(Res.string.file_move_body)),
                    DiaryNote(
                        DiaryIcons.ShieldCheck,
                        stringResource(Res.string.file_trust_title),
                        stringResource(Res.string.file_trust_body)
                    )
                )
            )
            when (state) {
                FileBackupUi.WriteFailed -> DiaryNotice(stringResource(Res.string.file_write_failed))

                is FileBackupUi.ReadFailed ->
                    DiaryNotice(
                        stringResource(
                            if (state.reason ==
                                RestoreFailure.NEEDS_UPDATE
                            ) {
                                Res.string.file_read_needs_update
                            } else {
                                Res.string.file_read_failed
                            }
                        )
                    )

                FileBackupUi.Reading -> DiaryNotice(stringResource(Res.string.file_reading))

                else -> Unit
            }
        }
        Spacer(Modifier.height(DiarySpacing.x3))
        if (!busy) {
            DiarySettingsRow(
                stringResource(Res.string.file_restore),
                onClick = onRestore,
                description = stringResource(Res.string.file_restore_body),
                leadingIcon = DiaryIcons.Download
            )
        }
    }
    Column(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = DiarySpacing.screenCompact, vertical = DiarySpacing.x3),
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x3)
    ) {
        if (state is FileBackupUi.Writing) {
            BasicText(
                stringResource(Res.string.file_writing),
                style = DiaryTheme.typography.caption.copy(color = DiaryTheme.colors.textSecondary)
            )
            DiaryProgressBar(state.done.toFloat() / state.total.coerceAtLeast(1))
        } else {
            DiaryButton(
                stringResource(Res.string.file_action),
                onMake,
                Modifier.fillMaxWidth(),
                size = DiaryButtonSize.XLarge,
                enabled = !busy && estimate?.entries != 0
            )
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.Saved(saved: FileBackupUi.Saved, onDone: () -> Unit) {
    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(DiarySpacing.x10))
        Heading(
            DiaryIcons.CircleCheck,
            stringResource(Res.string.file_saved_title),
            stringResource(Res.string.file_saved_body, saved.name, sizeText(saved.bytes))
        )
    }
    Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = DiarySpacing.screenCompact, vertical = DiarySpacing.x3)) {
        DiaryButton(stringResource(Res.string.file_done), onDone, Modifier.fillMaxWidth(), size = DiaryButtonSize.XLarge)
    }
}

@Composable
private fun bodyOf(estimate: BundleEstimate): String = if (estimate.entries == 0) {
    stringResource(Res.string.file_body_empty)
} else {
    stringResource(
        Res.string.file_body,
        pluralStringResource(Res.plurals.restore_count_entries, estimate.entries, estimate.entries.grouped()),
        pluralStringResource(Res.plurals.restore_count_photos, estimate.photos, estimate.photos.grouped()),
        sizeText(estimate.bytes)
    )
}

@Composable
private fun Heading(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) {
    val colors = DiaryTheme.colors
    Column(
        Modifier.fillMaxWidth().padding(horizontal = DiarySpacing.screenCompact, vertical = DiarySpacing.x6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x2)
    ) {
        DiaryHeroBadge(icon, tone = DiaryBadgeTone.Accent)
        Spacer(Modifier.height(DiarySpacing.x1))
        BasicText(title, style = DiaryTheme.typography.title2.copy(color = colors.textPrimary, textAlign = TextAlign.Center))
        BasicText(body, style = DiaryTheme.typography.body.copy(color = colors.textSecondary, textAlign = TextAlign.Center))
    }
}
