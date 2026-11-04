package org.sjhstudio.diary.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.sjhstudio.diary.core.data.SettingsRepository
import org.sjhstudio.diary.core.data.diary.EntryRepository
import org.sjhstudio.diary.core.model.EntryDetail
import org.sjhstudio.diary.core.platform.FontInstaller
import org.sjhstudio.diary.core.ui.BodyLook

/** 일기 상세. 휴지통으로 옮기면 [trashed]로 알리고 앱 틀이 되돌리기를 띄운다(FR-032). */
class EntryDetailViewModel(
    private val entryId: String,
    private val entries: EntryRepository,
    settings: SettingsRepository,
    fonts: FontInstaller
) : ViewModel() {
    val detail: StateFlow<EntryDetail?> = entries.entry(entryId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MILLIS), null)

    val look: StateFlow<BodyLook> =
        settings.settings
            .map { BodyLook(it.font, it.bodyTextSize, fonts.installedPath(it.font.id)) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, BodyLook())

    private val _trashed = Channel<Unit>(Channel.BUFFERED)
    val trashed: Flow<Unit> = _trashed.receiveAsFlow()

    fun toggleFavorite() {
        val entry = detail.value?.entry ?: return
        viewModelScope.launch { entries.setFavorite(entryId, !entry.isFavorite) }
    }

    fun dismissKeptBoth() {
        viewModelScope.launch { entries.clearKeptBoth(entryId) }
    }

    fun moveToTrash() {
        viewModelScope.launch {
            entries.moveToTrash(entryId)
            _trashed.send(Unit)
        }
    }

    private companion object {
        const val STOP_MILLIS = 5_000L
    }
}
