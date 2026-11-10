package org.sjhstudio.diary.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.sjhstudio.diary.core.data.SettingsRepository
import org.sjhstudio.diary.core.data.diary.EntryRepository
import org.sjhstudio.diary.core.model.EntryDetail
import org.sjhstudio.diary.core.platform.FontInstaller
import org.sjhstudio.diary.core.ui.BodyLook
import org.sjhstudio.diary.core.ui.bodyLook
import org.sjhstudio.diary.core.ui.launchWrite
import org.sjhstudio.diary.core.ui.stateInSafely

/**
 * 상세 화면이 보는 일기.
 * 읽는 중과 없는 일기를 구별한다.
 */
sealed interface EntryDetailState {
    data object Loading : EntryDetailState

    data object Missing : EntryDetailState

    /** 읽다가 실패했다. 계속 불러오는 중으로 두지 않고 다시 시도를 보인다. */
    data object Failed : EntryDetailState

    data class Ready(val detail: EntryDetail) : EntryDetailState
}

/**
 * 일기 상세.
 * 휴지통으로 옮기면 [trashed]로 알리고 앱 틀이 되돌리기를 띄운다(FR-032).
 */
class EntryDetailViewModel(
    private val entryId: String,
    private val entries: EntryRepository,
    settings: SettingsRepository,
    fonts: FontInstaller
) : ViewModel() {
    /**
     * 휴지통으로 옮기는 중.
     * 화면이 닫히는 동안 일기가 사라져도 "찾을 수 없어요"가 비치지 않게 한다.
     */
    private val leaving = MutableStateFlow(false)

    /** 다시 시도할 때마다 올라가서 읽기를 새로 시작한다. */
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
        }.stateInSafely(viewModelScope, SharingStarted.WhileSubscribed(STOP_MILLIS), EntryDetailState.Loading)

    fun retry() {
        attempt.value++
    }

    val look: StateFlow<BodyLook> =
        settings.settings
            .map { it.bodyLook(fonts::installedPath) }
            .stateInSafely(viewModelScope, SharingStarted.Eagerly, BodyLook())

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

    /**
     * 이쪽만 남긴다.
     * 다른 쪽은 지우지 않고 휴지통으로 옮겨서 복원할 수 있다.
     * 두 표시는 함께 사라진다.
     */
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

    private companion object {
        const val STOP_MILLIS = 5_000L
    }
}
