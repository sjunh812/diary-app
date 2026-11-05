package org.sjhstudio.diary.feature.backup

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonKind
import org.sjhstudio.diary.core.designsystem.component.DiaryStatusBox
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.model.BackupNudge
import org.sjhstudio.diary.core.ui.grouped
import org.sjhstudio.diary.feature.backup.resources.Res
import org.sjhstudio.diary.feature.backup.resources.backup_nudge_action
import org.sjhstudio.diary.feature.backup.resources.backup_nudge_body
import org.sjhstudio.diary.feature.backup.resources.backup_nudge_entries
import org.sjhstudio.diary.feature.backup.resources.backup_nudge_entries_photos
import org.sjhstudio.diary.feature.backup.resources.backup_nudge_later

/** 홈 목록 맨 위의 권하기 카드(Figma B11, FR-036). 창을 띄워 막지 않고, 조건이 맞을 때만 보이며, 목록과 함께 스크롤된다. */
@Composable
fun BackupNudgeCard(modifier: Modifier = Modifier) {
    val viewModel = koinViewModel<BackupViewModel>()
    val nudge by viewModel.nudge.collectAsStateWithLifecycle()
    nudge?.let { current ->
        BackupNudgeContent(current, { viewModel.connect(current.suggestedAccount) }, viewModel::dismissNudge, modifier)
    }
}

/** 연결했더니 찾은 백업을 알리는 시트. 카드가 스크롤로 사라져도 뜨도록 카드 밖에 둔다. */
@Composable
fun BackupFoundPrompt(onRestoreEntry: (String) -> Unit) {
    val viewModel = koinViewModel<BackupViewModel>()
    val found by viewModel.found.collectAsStateWithLifecycle()
    found?.let { entry ->
        BackupFoundSheet(entry, viewModel::foundShown) {
            viewModel.foundShown()
            onRestoreEntry(entry.snapshotName)
        }
    }
}

@Composable
internal fun BackupNudgeContent(nudge: BackupNudge, onConnect: () -> Unit, onLater: () -> Unit, modifier: Modifier = Modifier) {
    DiaryStatusBox(
        DiaryIcons.ShieldCheck,
        if (nudge.photoCount > 0) {
            stringResource(Res.string.backup_nudge_entries_photos, nudge.entryCount.grouped(), nudge.photoCount.grouped())
        } else {
            stringResource(Res.string.backup_nudge_entries, nudge.entryCount.grouped())
        },
        stringResource(Res.string.backup_nudge_body),
        modifier.padding(top = DiarySpacing.x3),
        actionLabel = stringResource(Res.string.backup_nudge_action),
        onAction = onConnect,
        secondaryLabel = stringResource(Res.string.backup_nudge_later),
        onSecondary = onLater,
        secondaryKind = DiaryButtonKind.Text
    )
}
