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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.sink
import io.github.vinceglb.filekit.size
import io.github.vinceglb.filekit.source
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.sjhstudio.diary.core.designsystem.component.DiaryBadgeTone
import org.sjhstudio.diary.core.designsystem.component.DiaryButton
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonSize
import org.sjhstudio.diary.core.designsystem.component.DiaryDialog
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
import org.sjhstudio.diary.core.designsystem.theme.LocalScreenGutter
import org.sjhstudio.diary.core.model.BundleEstimate
import org.sjhstudio.diary.core.model.ExpectedLeave
import org.sjhstudio.diary.core.model.RestoreFailure
import org.sjhstudio.diary.core.model.currentDate
import org.sjhstudio.diary.core.ui.KeepScreenOn
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
import org.sjhstudio.diary.feature.backup.resources.file_read_no_space
import org.sjhstudio.diary.feature.backup.resources.file_read_no_space_plain
import org.sjhstudio.diary.feature.backup.resources.file_saved_body
import org.sjhstudio.diary.feature.backup.resources.file_saved_title
import org.sjhstudio.diary.feature.backup.resources.file_stop_action
import org.sjhstudio.diary.feature.backup.resources.file_stop_body
import org.sjhstudio.diary.feature.backup.resources.file_stop_title
import org.sjhstudio.diary.feature.backup.resources.file_title
import org.sjhstudio.diary.feature.backup.resources.file_top
import org.sjhstudio.diary.feature.backup.resources.file_trust_body
import org.sjhstudio.diary.feature.backup.resources.file_trust_title
import org.sjhstudio.diary.feature.backup.resources.file_write_failed
import org.sjhstudio.diary.feature.backup.resources.file_write_failed_removed
import org.sjhstudio.diary.feature.backup.resources.file_writing
import org.sjhstudio.diary.feature.backup.resources.restore_count_entries
import org.sjhstudio.diary.feature.backup.resources.restore_count_photos
import org.sjhstudio.diary.feature.backup.resources.restore_failed_busy_body

/**
 * 파일로 백업하기(Figma "파일로 백업하기 (B8)").
 * 저장 위치는 사용자가 시스템 창에서 고른다(FR-031).
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FileBackupRoute(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel = koinViewModel<FileBackupViewModel>()
    val estimate by viewModel.estimate.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val saver =
        rememberFileSaverLauncher(FileKitDialogSettings.createDefault(), onError = {
            ExpectedLeave.clear()
            viewModel.saveFailed()
        }) { file: PlatformFile? ->
            ExpectedLeave.clear()
            file?.let { target ->
                viewModel.write(target.name, { OkioSink(target.sink()) }, { target.delete(mustExist = false) })
            }
        }
    // 나가면 파일을 끝까지 만들지 못한다.
    // 모르고 나가지 않게 한 번 묻는다.
    var asking by remember { mutableStateOf(false) }
    val writing = state is FileBackupUi.Writing
    BackHandler(enabled = writing) { asking = true }
    if (asking) {
        DiaryDialog(
            title = stringResource(Res.string.file_stop_title),
            confirmLabel = stringResource(Res.string.file_stop_action),
            onConfirm = {
                asking = false
                viewModel.stop()
                onBack()
            },
            onDismiss = { asking = false },
            message = stringResource(Res.string.file_stop_body),
            destructive = true
        )
    }
    FileBackupScreen(
        estimate,
        state,
        onMake = {
            scope.launch {
                val today = currentDate()
                ExpectedLeave.mark()
                saver.launch(suggestedName = getString(Res.string.file_name, today.toString()), defaultExtension = "zip")
            }
        },
        onDone = viewModel::clear,
        onBack = { if (writing) asking = true else onBack() },
        modifier = modifier
    )
}

@Composable
fun FileBackupScreen(
    estimate: BundleEstimate?,
    state: FileBackupUi,
    onMake: () -> Unit,
    onDone: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DiaryTheme.colors
    Column(modifier.fillMaxSize().background(colors.canvas).statusBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        DiaryTopBar(stringResource(Res.string.file_top), onBack = onBack)
        Column(Modifier.widthIn(max = DiaryWriteSize.settingsMax).fillMaxWidth().weight(1f)) {
            when (state) {
                is FileBackupUi.Saved -> Saved(state, onDone)
                else -> Start(estimate, state, onMake)
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.Start(estimate: BundleEstimate?, state: FileBackupUi, onMake: () -> Unit) {
    val busy = state is FileBackupUi.Writing || state == FileBackupUi.Reading
    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
        Heading(DiaryIcons.Download, stringResource(Res.string.file_title), estimate?.let { bodyOf(it) }.orEmpty())
        Column(Modifier.padding(horizontal = LocalScreenGutter.current), verticalArrangement = Arrangement.spacedBy(DiarySpacing.x4)) {
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
                is FileBackupUi.WriteFailed ->
                    DiaryNotice(
                        stringResource(if (state.left) Res.string.file_write_failed else Res.string.file_write_failed_removed),
                        failure = true
                    )

                else -> Unit
            }
        }
    }
    Column(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = LocalScreenGutter.current, vertical = DiarySpacing.x3),
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x3)
    ) {
        if (state is FileBackupUi.Writing) {
            KeepScreenOn()
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
        Heading(DiaryIcons.CircleCheck, stringResource(Res.string.file_saved_title), stringResource(Res.string.file_saved_body))
        // 파일 이름은 설명 글과 섞이지 않게 따로 둔다.
        // 파일 앱에서 찾을 때 이 줄을 본다.
        DiaryNoteBox(
            listOf(DiaryNote(DiaryIcons.Download, saved.name, sizeText(saved.bytes))),
            Modifier.padding(horizontal = LocalScreenGutter.current)
        )
    }
    Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = LocalScreenGutter.current, vertical = DiarySpacing.x3)) {
        DiaryButton(stringResource(Res.string.file_done), onDone, Modifier.fillMaxWidth(), size = DiaryButtonSize.XLarge)
    }
}

@Composable
internal fun readFailedText(failed: FileBackupUi.ReadFailed): String = when (failed.reason) {
    RestoreFailure.NEEDS_UPDATE -> stringResource(Res.string.file_read_needs_update)

    RestoreFailure.BUSY -> stringResource(Res.string.restore_failed_busy_body)

    RestoreFailure.NO_SPACE ->
        if (failed.bytesShort > 0) {
            stringResource(Res.string.file_read_no_space, sizeText(failed.bytesShort))
        } else {
            stringResource(Res.string.file_read_no_space_plain)
        }

    else -> stringResource(Res.string.file_read_failed)
}

/** 크기를 알려 주지 않는 저장소도 있다. */
internal fun PlatformFile.sizeOrNull(): Long? = try {
    size().takeIf { it > 0 }
} catch (_: Exception) {
    null
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
        Modifier.fillMaxWidth().padding(horizontal = LocalScreenGutter.current, vertical = DiarySpacing.x6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x2)
    ) {
        DiaryHeroBadge(icon, tone = DiaryBadgeTone.Accent)
        Spacer(Modifier.height(DiarySpacing.x1))
        BasicText(title, style = DiaryTheme.typography.title2.copy(color = colors.textPrimary, textAlign = TextAlign.Center))
        BasicText(body, style = DiaryTheme.typography.body.copy(color = colors.textSecondary, textAlign = TextAlign.Center))
    }
}
