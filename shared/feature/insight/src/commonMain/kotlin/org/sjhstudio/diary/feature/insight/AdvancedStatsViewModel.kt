package org.sjhstudio.diary.feature.insight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.sjhstudio.diary.core.data.diary.StatsRepository
import org.sjhstudio.diary.core.model.AdvancedPeriod
import org.sjhstudio.diary.core.model.AdvancedStats
import org.sjhstudio.diary.core.model.DateRange
import org.sjhstudio.diary.core.model.currentDate

/**
 * 고급 통계.
 * 차이를 찾으려면 일기가 많을수록 좋아서 전체부터 보인다.
 */
class AdvancedStatsViewModel(private val stats: StatsRepository, private val today: () -> LocalDate = { currentDate() }) : ViewModel() {
    private val _period = MutableStateFlow(AdvancedPeriod.ALL)
    val period: StateFlow<AdvancedPeriod> = _period.asStateFlow()

    private val _data = MutableStateFlow<AdvancedStats?>(null)

    /**
     * 읽는 동안 null이다.
     * 기간을 바꾸면 읽어 둔 값을 바로 보이고, 없으면 앞 기간 숫자가 새 기간 이름 아래 남지 않게 비운다.
     * 뼈대는 0.3초 뒤에 나타나 짧은 읽기에서는 그대로 비어 있다.
     */
    val data: StateFlow<AdvancedStats?> = _data.asStateFlow()

    private val day = MutableStateFlow(today())

    /** 읽어 둔 기간. 탭을 누를 때 비웠다 그리면 번쩍인다. */
    private val cache = mutableMapOf<AdvancedPeriod, AdvancedStats>()

    init {
        viewModelScope.launch {
            combine(_period, day, ::Pair).collectLatest { (period, date) ->
                stats.advanced(period, date).collect {
                    cache[period] = it
                    _data.value = it
                }
            }
        }
        // 다른 기간도 미리 읽어 탭을 눌러도 바로 보인다.
        viewModelScope.launch {
            AdvancedPeriod.entries.filter { it != _period.value }.forEach { period ->
                cache.getOrPut(period) { stats.advanced(period, day.value).first() }
            }
        }
    }

    /** 앱이 돌아올 때 부른다. 날이 바뀌었으면 기간을 새 오늘로 다시 잰다. */
    fun refreshToday() {
        val now = today()
        if (now != day.value) cache.clear()
        day.value = now
    }

    fun select(period: AdvancedPeriod) {
        if (period == _period.value) return
        _data.value = cache[period]
        _period.value = period
    }

    /** 기분을 눌러 그 일기들을 볼 때 쓰는 기간. */
    fun range(): DateRange? = _period.value.range(today())
}
