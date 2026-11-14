package org.sjhstudio.diary.feature.insight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.datetime.LocalDate
import org.sjhstudio.diary.core.data.diary.StatsRepository
import org.sjhstudio.diary.core.model.AdvancedPeriod
import org.sjhstudio.diary.core.model.AdvancedStats
import org.sjhstudio.diary.core.model.DateRange
import org.sjhstudio.diary.core.model.currentDate
import org.sjhstudio.diary.core.model.quietly
import org.sjhstudio.diary.core.ui.WhileUiSubscribed
import org.sjhstudio.diary.core.ui.launchSafely
import org.sjhstudio.diary.core.ui.stateInSafely

/** 기간과 그 기간의 숫자. [data]가 null이면 읽는 중, [failed]면 다시 시도를 보임 */
data class AdvancedState(val period: AdvancedPeriod, val data: AdvancedStats?, val failed: Boolean = false)

/** 고급 통계. 차이를 찾으려면 일기가 많을수록 좋아 전체부터 보임 */
class AdvancedStatsViewModel(private val stats: StatsRepository, private val today: () -> LocalDate = { currentDate() }) : ViewModel() {
    private val selected = MutableStateFlow(AdvancedPeriod.ALL)

    private val day = MutableStateFlow(today())

    /** 다시 시도마다 올라 고른 기간을 새로 읽음 */
    private val attempt = MutableStateFlow(0)

    /** 읽어 둔 기간. 탭을 누를 때 비웠다 그리면 번쩍임 */
    private val cache = mutableMapOf<AdvancedPeriod, AdvancedStats>()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val read: Flow<AdvancedState> =
        combine(selected, day, attempt) { period, date, _ -> period to date }.flatMapLatest { (period, date) ->
            stats.advanced(period, date)
                .map { data ->
                    cache[period] = data
                    AdvancedState(period, data)
                }
                // 다시 시도하면 읽는 중으로 바꿔 누른 것이 보이게
                .onStart { emit(AdvancedState(period, cache[period])) }
                // 읽기 실패 시 읽는 중으로 남기지 않고 다시 시도를 보임
                .catch { if (it is Exception) emit(AdvancedState(period, null, failed = true)) else throw it }
        }

    /**
     * 기간을 바꾸면 읽어 둔 값을 바로, 없으면 읽는 중. 앞 기간 숫자는 내놓지 않음
     * 화면이 볼 때만 계산
     * 스켈레톤은 0.3초 뒤에 나타나 짧은 읽기에서는 비어 있음
     */
    val state: StateFlow<AdvancedState> =
        combine(selected, read) { period, latest -> latest.takeIf { it.period == period } ?: AdvancedState(period, cache[period]) }
            .stateInSafely(viewModelScope, WhileUiSubscribed, AdvancedState(selected.value, null))

    init {
        // 다른 기간도 미리 읽어 탭을 눌러도 바로 보이게
        viewModelScope.launchSafely {
            AdvancedPeriod.entries.filter { it != selected.value }.forEach { period ->
                quietly { cache.getOrPut(period) { stats.advanced(period, day.value).first() } }
            }
        }
    }

    /** 앱이 돌아올 때. 날이 바뀌었으면 기간을 새 오늘로 다시 계산 */
    fun refreshToday() {
        val now = today()
        if (now != day.value) cache.clear()
        day.value = now
    }

    fun select(period: AdvancedPeriod) {
        if (period == selected.value) return
        selected.value = period
    }

    fun retry() {
        attempt.value++
    }

    /** 기분을 눌러 그 일기들을 볼 때 쓰는 기간 */
    fun range(): DateRange? = selected.value.range(today())
}
