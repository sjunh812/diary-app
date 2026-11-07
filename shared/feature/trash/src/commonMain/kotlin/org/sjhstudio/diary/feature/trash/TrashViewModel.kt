package org.sjhstudio.diary.feature.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.sjhstudio.diary.core.data.diary.TrashRepository
import org.sjhstudio.diary.core.model.TrashedEntry
import org.sjhstudio.diary.core.ui.launchWrite

/**
 * 휴지통(FR-034).
 * 처음 읽기 전에는 null이라 빈 상태가 잠깐 비치지 않는다.
 */
class TrashViewModel(private val trash: TrashRepository) : ViewModel() {
    val items: StateFlow<List<TrashedEntry>?> =
        trash.items().stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MILLIS), null)

    fun restore(id: String) {
        viewModelScope.launchWrite { trash.restore(id) }
    }

    fun deleteForever(id: String) {
        viewModelScope.launchWrite { trash.deleteForever(listOf(id)) }
    }

    fun empty() {
        viewModelScope.launchWrite { trash.empty() }
    }

    private companion object {
        const val STOP_MILLIS = 5_000L
    }
}
