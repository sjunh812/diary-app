package org.sjhstudio.diary.feature.insight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import org.sjhstudio.diary.core.data.diary.StatsRepository
import org.sjhstudio.diary.core.model.AdvancedPeriod
import org.sjhstudio.diary.core.model.AdvancedStats
import org.sjhstudio.diary.core.model.DateRange
import org.sjhstudio.diary.core.model.currentDate
import org.sjhstudio.diary.core.model.orElseOnFailure
import org.sjhstudio.diary.core.ui.launchSafely

/** 고급 통계. 차이를 찾으려면 일기가 많을수록 좋아 전체부터 보임 */
class AdvancedStatsViewModel(private val stats: StatsRepository, private val today: () -> LocalDate = { currentDate() }) : ViewModel() {
    private val _period = MutableStateFlow(AdvancedPeriod.ALL)
    val period: StateFlow<AdvancedPeriod> = _period.asStateFlow()

    private val _data = MutableStateFlow<AdvancedStats?>(null)

    /**
     * 읽는 동안 null.
     * 기간을 바꾸면 읽어 둔 값을 바로 보이고, 없으면 앞 기간 숫자가 새 기간 이름 아래 남지 않게 비움
     * 스켈레톤은 0.3초 뒤에 나타나 짧은 읽기에서는 비어 있음
     */
    val data: StateFlow<AdvancedStats?> = _data.asStateFlow()

    private val day = MutableStateFlow(today())

    /** 읽어 둔 기간. 탭을 누를 때 비웠다 그리면 번쩍임 */
    private val cache = mutableMapOf<AdvancedPeriod, AdvancedStats>()

    init {
        viewModelScope.launchSafely {
            combine(_period, day, ::Pair).collectLatest { (period, date) ->
                // 읽기 실패 시 앱을 끄지 않고 보던 숫자 유지
                stats.advanced(period, date).catch {}.collect {
                    cache[period] = it
                    _data.value = it
                }
            }
        }
        // 다른 기간도 미리 읽어 탭을 눌러도 바로 보이게
        viewModelScope.launchSafely {
            AdvancedPeriod.entries.filter { it != _period.value }.forEach { period ->
                orElseOnFailure(Unit) { cache.getOrPut(period) { stats.advanced(period, day.value).first() } }
            }
        }
    }

    /** 앱이 돌아올 때. 날이 바뀌었으면 기간을 새 오늘로 다시 잼 */
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

    /** 기분을 눌러 그 일기들을 볼 때 쓰는 기간 */
    fun range(): DateRange? = _period.value.range(today())
}
