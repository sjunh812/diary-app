package org.sjhstudio.diary.core.ui

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.component.DiaryBottomSheet
import org.sjhstudio.diary.core.designsystem.component.DiarySheetAction
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.ui.resources.Res
import org.sjhstudio.diary.core.ui.resources.entry_menu_edit
import org.sjhstudio.diary.core.ui.resources.entry_menu_favorite
import org.sjhstudio.diary.core.ui.resources.entry_menu_share
import org.sjhstudio.diary.core.ui.resources.entry_menu_trash
import org.sjhstudio.diary.core.ui.resources.entry_menu_unfavorite

/**
 * 일기 줄을 길게 눌렀을 때의 메뉴.
 * 홈과 달력이 같은 메뉴를 쓴다.
 * 즐겨찾기는 그 일기의 상태에 따라 넣기와 해제로 바뀐다.
 */
@Composable
fun EntryMenu(
    favorite: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onFavorite: () -> Unit,
    onShare: () -> Unit,
    onTrash: () -> Unit
) {
    val colors = DiaryTheme.colors
    DiaryBottomSheet(onDismiss = onDismiss) {
        DiarySheetAction(DiaryIcons.Pen, stringResource(Res.string.entry_menu_edit), colors.textPrimary, onClick = onEdit)
        DiarySheetAction(
            if (favorite) DiaryIcons.StarFilled else DiaryIcons.StarOutline,
            stringResource(if (favorite) Res.string.entry_menu_unfavorite else Res.string.entry_menu_favorite),
            colors.textPrimary,
            onClick = onFavorite
        )
        DiarySheetAction(DiaryIcons.Share, stringResource(Res.string.entry_menu_share), colors.textPrimary, onClick = onShare)
        DiarySheetAction(DiaryIcons.Trash, stringResource(Res.string.entry_menu_trash), colors.danger, onClick = onTrash)
    }
}
