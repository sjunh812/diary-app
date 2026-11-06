package org.sjhstudio.diary.feature.insight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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
     * 기간을 바꾸면 앞 기간 숫자가 새 기간 이름 아래 남지 않게 비운다.
     * 뼈대는 0.3초 뒤에 나타나 짧은 읽기에서는 그대로 비어 있다.
     */
    val data: StateFlow<AdvancedStats?> = _data.asStateFlow()

    init {
        viewModelScope.launch {
            _period.collectLatest { period -> stats.advanced(period, today()).collect { _data.value = it } }
        }
    }

    fun select(period: AdvancedPeriod) {
        if (period == _period.value) return
        _data.value = null
        _period.value = period
    }

    /** 기분을 눌러 그 일기들을 볼 때 쓰는 기간. */
    fun range(): DateRange? = _period.value.range(today())
}
