package org.sjhstudio.diary.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.sjhstudio.diary.core.data.SettingsRepository
import org.sjhstudio.diary.core.data.diary.EntryRepository
import org.sjhstudio.diary.core.model.EntryDetail
import org.sjhstudio.diary.core.platform.FontInstaller
import org.sjhstudio.diary.core.ui.BodyLook

/** 상세 화면이 보는 일기. 읽는 중과 없는 일기를 구별한다. */
sealed interface EntryDetailState {
    data object Loading : EntryDetailState

    data object Missing : EntryDetailState

    data class Ready(val detail: EntryDetail) : EntryDetailState
}

/** 일기 상세. 휴지통으로 옮기면 [trashed]로 알리고 앱 틀이 되돌리기를 띄운다(FR-032). */
class EntryDetailViewModel(
    private val entryId: String,
    private val entries: EntryRepository,
    settings: SettingsRepository,
    fonts: FontInstaller
) : ViewModel() {
    /** 휴지통으로 옮기는 중. 화면이 닫히는 동안 일기가 사라져도 "찾을 수 없어요"가 비치지 않게 한다. */
    private val leaving = MutableStateFlow(false)

    val state: StateFlow<EntryDetailState> =
        combine(entries.entry(entryId), leaving) { detail, leaving ->
            when {
                detail != null -> EntryDetailState.Ready(detail)
                leaving -> EntryDetailState.Loading
                else -> EntryDetailState.Missing
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MILLIS), EntryDetailState.Loading)

    val look: StateFlow<BodyLook> =
        settings.settings
            .map { BodyLook(it.font, it.bodyTextSize, fonts.installedPath(it.font.id)) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, BodyLook())

    private val _trashed = Channel<Unit>(Channel.BUFFERED)
    val trashed: Flow<Unit> = _trashed.receiveAsFlow()

    fun toggleFavorite() {
        val entry = (state.value as? EntryDetailState.Ready)?.detail?.entry ?: return
        viewModelScope.launch { entries.setFavorite(entryId, !entry.isFavorite) }
    }

    fun toggleCheck(index: Int) {
        viewModelScope.launch { entries.toggleCheck(entryId, index) }
    }

    fun dismissKeptBoth() {
        viewModelScope.launch { entries.clearKeptBoth(entryId) }
    }

    fun moveToTrash() {
        leaving.value = true
        viewModelScope.launch {
            entries.moveToTrash(entryId)
            _trashed.send(Unit)
        }
    }

    private companion object {
        const val STOP_MILLIS = 5_000L
    }
}
