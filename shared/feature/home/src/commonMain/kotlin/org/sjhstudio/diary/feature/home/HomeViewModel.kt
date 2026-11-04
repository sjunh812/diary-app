package org.sjhstudio.diary.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.YearMonth
import org.sjhstudio.diary.core.data.SettingsRepository
import org.sjhstudio.diary.core.data.diary.EntryRepository
import org.sjhstudio.diary.core.model.EntryRow
import org.sjhstudio.diary.core.model.ListStyle
import org.sjhstudio.diary.core.platform.FontInstaller
import org.sjhstudio.diary.core.ui.BodyLook

/** 홈 목록(FR-015~FR-018, FR-021). 줄은 60편씩 읽어서 1만 편이어도 첫 화면이 빠르다. */
class HomeViewModel(private val entries: EntryRepository, private val settings: SettingsRepository, fonts: FontInstaller) : ViewModel() {
    val style: StateFlow<ListStyle> =
        settings.settings
            .map { it.list.listStyle }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.Eagerly, ListStyle.FEED)

    val favoritesOnly: StateFlow<Boolean> =
        settings.settings
            .map { it.list.favoritesOnly }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val rows: Flow<PagingData<EntryRow>> = favoritesOnly.flatMapLatest { entries.timeline(it) }.cachedIn(viewModelScope)

    val look: StateFlow<BodyLook> =
        settings.settings
            .map { BodyLook(it.font, it.bodyTextSize, fonts.installedPath(it.font.id)) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, BodyLook())

    /** 일기가 있는 달. 연·월 고르기 시트가 이 달만 고르게 한다. */
    val months: StateFlow<Set<YearMonth>> = entries.months().stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    fun toggleFavoritesOnly() {
        viewModelScope.launch { settings.setFavoritesOnly(!favoritesOnly.value) }
    }

    /** 그 달 첫 일기의 목록 위치. */
    suspend fun positionOf(month: YearMonth): Int = entries.positionOf(month, favoritesOnly.value)
}
