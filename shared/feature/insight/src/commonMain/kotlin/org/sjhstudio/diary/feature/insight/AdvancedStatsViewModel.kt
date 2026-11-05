package org.sjhstudio.diary.feature.insight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.time.Clock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.sjhstudio.diary.core.data.diary.StatsRepository
import org.sjhstudio.diary.core.model.AdvancedPeriod
import org.sjhstudio.diary.core.model.AdvancedStats
import org.sjhstudio.diary.core.model.DateRange

/** 고급 통계. 차이를 찾으려면 일기가 많을수록 좋아서 전체부터 보인다. */
class AdvancedStatsViewModel(
    private val stats: StatsRepository,
    private val today: () -> LocalDate = { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
) : ViewModel() {
    private val _period = MutableStateFlow(AdvancedPeriod.ALL)
    val period: StateFlow<AdvancedPeriod> = _period.asStateFlow()

    /** 기간을 바꾸면 계산이 끝날 때까지 뼈대를 보인다. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val data: StateFlow<AdvancedStats?> =
        _period
            .flatMapLatest { period -> stats.advanced(period, today()).map<AdvancedStats, AdvancedStats?> { it }.onStart { emit(null) } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MILLIS), null)

    fun select(period: AdvancedPeriod) {
        _period.value = period
    }

    /** 기분을 눌러 그 일기들을 볼 때 쓰는 기간. */
    fun range(): DateRange? = _period.value.range(today())

    private companion object {
        const val STOP_MILLIS = 5_000L
    }
}
