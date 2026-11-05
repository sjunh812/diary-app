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

/** 고급 통계. 차이를 찾으려면 일기가 많을수록 좋아서 전체부터 보인다. */
class AdvancedStatsViewModel(private val stats: StatsRepository, private val today: () -> LocalDate = { currentDate() }) : ViewModel() {
    private val _period = MutableStateFlow(AdvancedPeriod.ALL)
    val period: StateFlow<AdvancedPeriod> = _period.asStateFlow()

    private val _data = MutableStateFlow<AdvancedStats?>(null)

    /** 기간을 바꾸면 계산이 끝날 때까지 뼈대를 보인다. 다른 화면에 다녀올 때는 그대로 둔다. */
    val data: StateFlow<AdvancedStats?> = _data.asStateFlow()

    init {
        viewModelScope.launch {
            _period.collectLatest { period ->
                _data.value = null
                stats.advanced(period, today()).collect { _data.value = it }
            }
        }
    }

    fun select(period: AdvancedPeriod) {
        _period.value = period
    }

    /** 기분을 눌러 그 일기들을 볼 때 쓰는 기간. */
    fun range(): DateRange? = _period.value.range(today())
}
