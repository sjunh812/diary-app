package org.sjhstudio.diary.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
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
 * 일기 줄을 길게 눌렀을 때의 메뉴
 * 홈과 달력이 같은 메뉴를 씀
 * 즐겨찾기는 그 일기의 상태에 따라 넣기와 해제로 바뀜
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
        DiarySheetAction(DiaryIcons.EditText, stringResource(Res.string.entry_menu_edit), colors.textPrimary, onClick = onEdit)
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

/** 일기 메뉴의 일이 끝나 화면이 이어서 할 일 */
sealed interface EntryMenuEvent {
    data class Trashed(val id: String) : EntryMenuEvent

    data class Share(val entry: Entry) : EntryMenuEvent
}

/**
 * 일기 메뉴가 저장소에 맡기는 일
 * 홈과 달력의 화면 모델이 하나씩 들고 같은 일을 함
 * 끝난 일은 [events]로 알림. 화면이 다시 만들어져도 새 화면이 받음
 */
class EntryMenuWork(private val scope: CoroutineScope, private val entries: EntryRepository) {
    private val _events = Channel<EntryMenuEvent>(Channel.BUFFERED)
    val events: Flow<EntryMenuEvent> = _events.receiveAsFlow()

    fun toggleFavorite(id: String) {
        scope.launchWrite {
            val entry = entries.entry(id).first()?.entry ?: return@launchWrite
            entries.setFavorite(id, !entry.isFavorite)
        }
    }

    /** 휴지통에 넣은 뒤 [EntryMenuEvent.Trashed]. 실패하면 보내지 않고 실패 안내만 뜸 */
    fun moveToTrash(id: String) {
        scope.launchWrite {
            entries.moveToTrash(id)
            _events.send(EntryMenuEvent.Trashed(id))
        }
    }

    /**
     * 공유할 일기를 읽어 [EntryMenuEvent.Share]로 넘김
     * 메뉴는 누르자마자 닫혀 화면에서 빠지므로 화면 모델의 범위에서 읽음
     */
    fun share(id: String) {
        scope.launchSafely {
            entries.entry(id).first()?.entry?.let { _events.send(EntryMenuEvent.Share(it)) }
        }
    }
}

/**
 * [EntryMenuWork.events]를 받아 공유 창을 띄우고, 휴지통에 넣었으면 [onTrash]를 부름
 * 메뉴가 닫힌 뒤에 끝나는 일이라 메뉴가 아닌 화면에 둠
 */
@Composable
fun EntryMenuEvents(work: EntryMenuWork, onTrash: (String) -> Unit) {
    val trashed by rememberUpdatedState(onTrash)
    var sharing by remember { mutableStateOf<Entry?>(null) }
    LaunchedEffect(work) {
        work.events.collect { event ->
            when (event) {
                is EntryMenuEvent.Trashed -> trashed(event.id)
                is EntryMenuEvent.Share -> sharing = event.entry
            }
        }
    }
    sharing?.let { entry -> ShareEntry(entry, onShow = { sharing = null }) }
}

// iOS의 공유 창은 화면 컨트롤러가 있어야 해서 공유할 때만 준비
@Composable
private fun ShareEntry(entry: Entry, onShow: () -> Unit) {
    val share = rememberShareText()
    val shareText = rememberEntryShareText()
    val shown by rememberUpdatedState(onShow)
    LaunchedEffect(entry) {
        share(shareText(entry))
        shown()
    }
}

/**
 * [EntryMenu]를 띄우고 고른 일을 [work]에 넘김
 * 홈과 달력이 같은 순서로 닫고 실행해야 해서 한곳에 둠
 */
@Composable
fun EntryMenuFor(id: String, favorite: Boolean, work: EntryMenuWork, onDismiss: () -> Unit, onEdit: (String) -> Unit) {
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
            work.share(id)
        },
        onTrash = {
            onDismiss()
            work.moveToTrash(id)
        }
    )
}
