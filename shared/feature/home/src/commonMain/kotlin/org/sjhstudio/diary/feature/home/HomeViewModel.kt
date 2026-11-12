package org.sjhstudio.diary.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.YearMonth
import org.sjhstudio.diary.core.data.SettingsRepository
import org.sjhstudio.diary.core.data.diary.EntryRepository
import org.sjhstudio.diary.core.model.EntryRow
import org.sjhstudio.diary.core.model.ListStyle
import org.sjhstudio.diary.core.platform.FontInstaller
import org.sjhstudio.diary.core.ui.BodyLook
import org.sjhstudio.diary.core.ui.EntryMenuWork
import org.sjhstudio.diary.core.ui.bodyLookIn
import org.sjhstudio.diary.core.ui.launchSafely
import org.sjhstudio.diary.core.ui.stateInSafely

/**
 * 홈 목록(FR-015~FR-018, FR-021)
 * 60편씩 읽어 1만 편이어도 첫 화면이 빠름
 */
class HomeViewModel(private val entries: EntryRepository, private val settings: SettingsRepository, fonts: FontInstaller) : ViewModel() {
    /** 일기 줄 길게 누르기 메뉴(달력과 같음) */
    val menu = EntryMenuWork(viewModelScope, entries)

    val style: StateFlow<ListStyle> =
        settings.settings
            .map { it.list.listStyle }
            .distinctUntilChanged()
            .stateInSafely(viewModelScope, SharingStarted.Eagerly, ListStyle.FEED)

    // 앱을 다시 열면 풀림. 켠 채로 남으면 쓴 일기가 사라진 것처럼 보임
    private val favorites = MutableStateFlow(false)
    val favoritesOnly: StateFlow<Boolean> = favorites.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val rows: Flow<PagingData<EntryRow>> = favoritesOnly.flatMapLatest { entries.timeline(it) }.cachedIn(viewModelScope)

    val look: StateFlow<BodyLook> =
        settings.bodyLookIn(viewModelScope, fonts::installedPath)

    /** 일기가 있는 달. 연·월 고르기 시트가 이 달만 고르게 함 */
    val months: StateFlow<Set<YearMonth>> =
        favoritesOnly.flatMapLatest { only -> if (only) entries.favoriteMonths() else entries.months() }
            .stateInSafely(viewModelScope, SharingStarted.Eagerly, emptySet())

    fun toggleFavoritesOnly() {
        favorites.update { !it }
    }

    private val _jumps = Channel<Int>(Channel.CONFLATED)

    /** 고른 달 첫 일기의 목록 위치. 화면이 받아 그 자리로 스크롤 */
    val jumps: Flow<Int> = _jumps.receiveAsFlow()

    /** 읽지 못하면 목록을 그대로 둠 */
    fun jumpTo(month: YearMonth) {
        viewModelScope.launchSafely { _jumps.send(entries.positionOf(month, favoritesOnly.value)) }
    }
}
