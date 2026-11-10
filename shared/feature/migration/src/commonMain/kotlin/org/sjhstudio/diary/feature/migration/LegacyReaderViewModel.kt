package org.sjhstudio.diary.feature.migration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.sjhstudio.diary.core.migration.LegacyArchive
import org.sjhstudio.diary.core.migration.LegacyEntryView
import org.sjhstudio.diary.core.model.orElseOnFailure
import org.sjhstudio.diary.core.ui.launchSafely

/**
 * [failed]면 쪽을 읽지 못한 것이다.
 * 읽을 일기가 없는 빈 목록과 구별해 다시 시도를 보인다.
 */
data class LegacyReaderState(
    val entries: List<LegacyEntryView> = emptyList(),
    val loading: Boolean = true,
    val end: Boolean = false,
    val failed: Boolean = false
)

/**
 * 예전 일기 보기(M4).
 * 사본을 일기 날짜 최근 순으로 60편씩 더 읽는다.
 * 수정·삭제는 없다.
 */
class LegacyReaderViewModel(private val archive: LegacyArchive) : ViewModel() {
    private val _state = MutableStateFlow(LegacyReaderState())
    val state: StateFlow<LegacyReaderState> = _state.asStateFlow()

    /**
     * 읽는 중인 쪽.
     * 끝나기 전에는 다음 쪽을 부르지 않는다(같은 쪽이 두 번 붙지 않게).
     */
    private var loading: Job? = null

    init {
        loadMore()
    }

    fun loadMore() {
        if (loading?.isActive == true || _state.value.end) return
        _state.update { it.copy(loading = true, failed = false) }
        loading =
            viewModelScope.launchSafely {
                val after = _state.value.entries.lastOrNull()
                val page =
                    orElseOnFailure(null) { archive.page(after = after) }
                _state.update {
                    if (page == null) {
                        it.copy(loading = false, failed = true)
                    } else {
                        it.copy(entries = it.entries + page, loading = false, end = page.size < LegacyArchive.PAGE)
                    }
                }
            }
    }
}
