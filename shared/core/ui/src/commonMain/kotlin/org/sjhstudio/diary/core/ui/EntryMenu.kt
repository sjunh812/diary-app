package org.sjhstudio.diary.core.ui

import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.data.diary.EntryRepository
import org.sjhstudio.diary.core.designsystem.component.DiaryBottomSheet
import org.sjhstudio.diary.core.designsystem.component.DiarySheetAction
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.Entry
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
            if (favorite) DiaryIcons.StarOff else DiaryIcons.StarOutline,
            stringResource(if (favorite) Res.string.entry_menu_unfavorite else Res.string.entry_menu_favorite),
            colors.textPrimary,
            onClick = onFavorite
        )
        DiarySheetAction(DiaryIcons.Share, stringResource(Res.string.entry_menu_share), colors.textPrimary, onClick = onShare)
        DiarySheetAction(DiaryIcons.Trash, stringResource(Res.string.entry_menu_trash), colors.danger, onClick = onTrash)
    }
}

/**
 * 일기 메뉴가 저장소에 시키는 일.
 * 홈과 달력의 화면 모델이 하나씩 들고 같은 일을 한다.
 */
class EntryMenuWork(private val scope: CoroutineScope, private val entries: EntryRepository) {
    fun toggleFavorite(id: String) {
        scope.launchWrite {
            val entry = entries.entry(id).first()?.entry ?: return@launchWrite
            entries.setFavorite(id, !entry.isFavorite)
        }
    }

    /** 휴지통에 넣은 뒤에 [onDone]을 부른다. 실패하면 부르지 않고 실패 안내만 뜬다. */
    fun moveToTrash(id: String, onDone: () -> Unit) {
        scope.launchWrite {
            entries.moveToTrash(id)
            onDone()
        }
    }

    // 공유할 일기. 화면이 날짜·기분과 함께 글로 만든다.
    suspend fun entryToShare(id: String): Entry? = entries.entry(id).first()?.entry

    /**
     * 공유할 일기를 읽어 [onReady]로 건넨다.
     * 메뉴는 누르자마자 닫혀 화면에서 빠지므로 화면 모델의 범위에서 읽는다.
     * 메뉴 안에서 읽으면 닫히면서 함께 취소되어 공유 창이 뜨지 않는다.
     */
    fun share(id: String, onReady: (Entry) -> Unit) {
        scope.launchSafely { entryToShare(id)?.let(onReady) }
    }
}

/**
 * [EntryMenu]를 띄우고 고른 일을 [work]에 넘긴다.
 * 홈과 달력이 같은 순서로 닫고 실행해야 해서 한곳에 둔다.
 */
@Composable
fun EntryMenuFor(
    id: String,
    favorite: Boolean,
    work: EntryMenuWork,
    onDismiss: () -> Unit,
    onEdit: (String) -> Unit,
    onTrash: (String) -> Unit
) {
    // iOS의 공유 창은 화면 컨트롤러가 있어야 해서 메뉴를 열 때만 준비한다.
    val share = rememberShareText()
    val shareText = rememberEntryShareText()
    EntryMenu(
        favorite = favorite,
        onDismiss = onDismiss,
        onEdit = {
            onDismiss()
            onEdit(id)
        },
        onFavorite = {
            onDismiss()
            work.toggleFavorite(id)
        },
        onShare = {
            onDismiss()
            work.share(id) { entry -> share(shareText(entry)) }
        },
        onTrash = {
            onDismiss()
            work.moveToTrash(id) { onTrash(id) }
        }
    )
}
