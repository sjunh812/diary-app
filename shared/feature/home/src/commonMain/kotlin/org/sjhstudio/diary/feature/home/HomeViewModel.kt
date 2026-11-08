package org.sjhstudio.diary.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.YearMonth
import org.sjhstudio.diary.core.data.SettingsRepository
import org.sjhstudio.diary.core.data.diary.EntryRepository
import org.sjhstudio.diary.core.model.Entry
import org.sjhstudio.diary.core.model.EntryRow
import org.sjhstudio.diary.core.model.ListStyle
import org.sjhstudio.diary.core.platform.FontInstaller
import org.sjhstudio.diary.core.ui.BodyLook
import org.sjhstudio.diary.core.ui.EntryMenuWork
import org.sjhstudio.diary.core.ui.bodyLook
import org.sjhstudio.diary.core.ui.launchWrite

/**
 * 홈 목록(FR-015~FR-018, FR-021).
 * 줄은 60편씩 읽어서 1만 편이어도 첫 화면이 빠르다.
 */
class HomeViewModel(private val entries: EntryRepository, private val settings: SettingsRepository, fonts: FontInstaller) : ViewModel() {
    /** 일기 줄을 길게 눌렀을 때의 메뉴(홈과 달력이 같다). */
    val menu = EntryMenuWork(viewModelScope, entries)

    val style: StateFlow<ListStyle> =
        settings.settings
            .map { it.list.listStyle }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.Eagerly, ListStyle.FEED)

    // 앱을 다시 열면 풀린다.
    // 켠 채로 남으면 쓴 일기가 사라진 것처럼 보인다.
    private val favorites = MutableStateFlow(false)
    val favoritesOnly: StateFlow<Boolean> = favorites.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val rows: Flow<PagingData<EntryRow>> = favoritesOnly.flatMapLatest { entries.timeline(it) }.cachedIn(viewModelScope)

    val look: StateFlow<BodyLook> =
        settings.settings
            .map { it.bodyLook(fonts::installedPath) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, BodyLook())

    /**
     * 일기가 있는 달.
     * 연·월 고르기 시트가 이 달만 고르게 한다.
     */
    val months: StateFlow<Set<YearMonth>> =
        favoritesOnly.flatMapLatest { only -> if (only) entries.favoriteMonths() else entries.months() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    fun toggleFavoritesOnly() {
        favorites.update { !it }
    }

    /** 그 달 첫 일기의 목록 위치. */
    suspend fun positionOf(month: YearMonth): Int = entries.positionOf(month, favoritesOnly.value)
}
