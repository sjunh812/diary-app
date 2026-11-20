package org.sjhstudio.diary.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.YearMonth
import org.sjhstudio.diary.core.data.ContextTips
import org.sjhstudio.diary.core.data.PlanRepository
import org.sjhstudio.diary.core.data.SettingsRepository
import org.sjhstudio.diary.core.data.diary.EntryRepository
import org.sjhstudio.diary.core.model.EntryRow
import org.sjhstudio.diary.core.model.ListStyle
import org.sjhstudio.diary.core.model.MonthCount
import org.sjhstudio.diary.core.model.PreviewLength
import org.sjhstudio.diary.core.model.PreviewMode
import org.sjhstudio.diary.core.platform.FontInstaller
import org.sjhstudio.diary.core.ui.BodyLook
import org.sjhstudio.diary.core.ui.EntryMenuWork
import org.sjhstudio.diary.core.ui.bodyLookIn
import org.sjhstudio.diary.core.ui.launchSafely
import org.sjhstudio.diary.core.ui.launchWrite
import org.sjhstudio.diary.core.ui.stateInSafely

/**
 * 홈 목록(FR-015~FR-018, FR-021)
 * 60편씩 읽어 1만 편이어도 첫 화면이 빠름
 */
class HomeViewModel(
    private val entries: EntryRepository,
    private val settings: SettingsRepository,
    fonts: FontInstaller,
    plans: PlanRepository,
    private val tips: ContextTips,
    months: HomeMonths
) : ViewModel() {
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

    /**
     * 미리보기 방식(스펙 008). 서식은 확정된 요금제가 Plus일 때만
     * 바뀌면 저장소가 같은 목록을 보던 자리 근처에서 다시 읽음
     */
    private val modes: Flow<PreviewMode> =
        combine(settings.settings.map { it.list }, plans.confirmed) { list, plan -> PreviewMode.of(list, plan) }.distinctUntilChanged()

    /** 목록 위 팁을 고르고, 팁의 [켜기]·[3줄로 보기]가 바꿈(스펙 008) */
    val previewLength: StateFlow<PreviewLength?> =
        settings.settings
            .map { it.list.previewLength }
            .distinctUntilChanged()
            // 읽기 전에는 null. 기본값 3줄로 팁을 고르거나 횟수를 세지 않음
            .stateInSafely(viewModelScope, SharingStarted.Eagerly, null)

    fun setPreviewLength(length: PreviewLength) {
        viewModelScope.launchWrite { settings.setPreviewLength(length) }
    }

    /** 3줄에서 잘린 일기를 상세로 엶. 전체 보기면 셀 까닭이 없음 */
    fun clippedOpened() {
        if (previewLength.value != PreviewLength.FULL) tips.clippedOpened()
    }

    /**
     * 전체 목록과 즐겨찾기 목록은 따로 페이징
     * 한 흐름을 바꿔 끼우면 새 목록이 오기 전 한 순간 옛 필터의 빈 목록이 새 필터 문구로 보임
     */
    val allRows: Flow<PagingData<EntryRow>> = entries.timeline(false, modes).cachedIn(viewModelScope)
    val favoriteRows: Flow<PagingData<EntryRow>> = entries.timeline(true, modes).cachedIn(viewModelScope)

    val look: StateFlow<BodyLook> =
        settings.bodyLookIn(viewModelScope, fonts)

    /** 달별 일기 수([HomeMonths]). 앱이 미리 읽어 두어 홈을 처음 그릴 때 이미 앎 */
    val allMonths: StateFlow<List<MonthCount>?> = months.all
    val favoriteMonths: StateFlow<List<MonthCount>?> = months.favorites

    init {
        months.start()
    }

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
