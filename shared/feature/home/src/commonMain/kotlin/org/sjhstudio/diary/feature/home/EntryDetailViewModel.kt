package org.sjhstudio.diary.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import org.sjhstudio.diary.core.data.SettingsRepository
import org.sjhstudio.diary.core.data.diary.EntryRepository
import org.sjhstudio.diary.core.model.EntryDetail
import org.sjhstudio.diary.core.platform.FontInstaller
import org.sjhstudio.diary.core.ui.BodyLook
import org.sjhstudio.diary.core.ui.WhileUiSubscribed
import org.sjhstudio.diary.core.ui.bodyLookIn
import org.sjhstudio.diary.core.ui.launchWrite
import org.sjhstudio.diary.core.ui.stateInSafely

/** 상세 화면이 보는 일기. 읽는 중과 없는 일기를 구분 */
sealed interface EntryDetailState {
    data object Loading : EntryDetailState

    data object Missing : EntryDetailState

    /** 읽기 실패. 불러오는 중으로 두지 않고 다시 시도를 보임 */
    data object Failed : EntryDetailState

    data class Ready(val detail: EntryDetail) : EntryDetailState
}

/** 일기 상세. 휴지통으로 옮기면 [trashed]로 알려 앱 틀이 되돌리기를 띄움(FR-032) */
class EntryDetailViewModel(
    private val entryId: String,
    private val entries: EntryRepository,
    settings: SettingsRepository,
    fonts: FontInstaller
) : ViewModel() {
    /** 휴지통으로 옮기는 중. 화면이 닫히는 동안 "찾을 수 없어요"가 비치지 않게 */
    private val leaving = MutableStateFlow(false)

    /** 다시 시도마다 올라 읽기를 새로 시작 */
    private val attempt = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<EntryDetailState> =
        attempt.flatMapLatest {
            combine(entries.entry(entryId), leaving) { detail, leaving ->
                when {
                    detail != null -> EntryDetailState.Ready(detail)
                    leaving -> EntryDetailState.Loading
                    else -> EntryDetailState.Missing
                }
            }.catch { if (it is Exception) emit(EntryDetailState.Failed) else throw it }
        }.stateInSafely(viewModelScope, WhileUiSubscribed, EntryDetailState.Loading)

    fun retry() {
        attempt.value++
    }

    val look: StateFlow<BodyLook> =
        settings.bodyLookIn(viewModelScope, fonts::installedPath)

    private val _trashed = Channel<Unit>(Channel.BUFFERED)
    val trashed: Flow<Unit> = _trashed.receiveAsFlow()

    fun toggleFavorite() {
        val entry = (state.value as? EntryDetailState.Ready)?.detail?.entry ?: return
        viewModelScope.launchWrite { entries.setFavorite(entryId, !entry.isFavorite) }
    }

    fun toggleCheck(index: Int) {
        viewModelScope.launchWrite { entries.toggleCheck(entryId, index) }
    }

    fun dismissKeptBoth() {
        viewModelScope.launchWrite { entries.clearKeptBoth(entryId) }
    }

    /** 이쪽만 남김. 다른 쪽은 지우지 않고 휴지통으로 옮겨 복원할 수 있음. 두 표시는 함께 사라짐 */
    fun keepThisVersion() {
        val other = (state.value as? EntryDetailState.Ready)?.detail?.keptBothWith ?: return
        viewModelScope.launchWrite { entries.moveToTrash(other) }
    }

    fun moveToTrash() {
        leaving.value = true
        viewModelScope.launchWrite {
            entries.moveToTrash(entryId)
            _trashed.send(Unit)
        }
    }
}
