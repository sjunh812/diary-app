package org.sjhstudio.diary.feature.home

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.sjhstudio.diary.core.data.diary.EntryRepository
import org.sjhstudio.diary.core.model.MonthCount

/**
 * 달별 일기 수. 홈 큰 제목이 아직 읽지 않은 줄의 달을 세고, 연·월 고르기 시트가 이 달만 고르게 함
 * 앱 범위에서 기록 이전 관문을 지난 뒤 한 번 읽기 시작해, 홈을 처음 그릴 때 이미 알고 있음(제목이 비었다 나타나지 않게)
 * 읽기 전에는 null. 빈 목록이면 일기가 없다는 뜻
 * 전체와 즐겨찾기를 따로 둠. 바꿔 끼우면 한 순간 옛 필터의 달이 제목에 보임
 */
class HomeMonths(private val entries: EntryRepository, private val scope: CoroutineScope) {
    private val allRead = MutableStateFlow<List<MonthCount>?>(null)
    private val favoritesRead = MutableStateFlow<List<MonthCount>?>(null)
    val all: StateFlow<List<MonthCount>?> = allRead.asStateFlow()
    val favorites: StateFlow<List<MonthCount>?> = favoritesRead.asStateFlow()
    private var reading: Job? = null

    /** 일기 DB를 읽으므로 기록 이전 관문을 지난 뒤에 부름. 두 번 불러도 한 번만 읽음 */
    fun start() {
        if (reading != null) return
        reading =
            scope.launch {
                launch { entries.monthCounts(false).collect { allRead.value = it } }
                launch { entries.monthCounts(true).collect { favoritesRead.value = it } }
            }
    }
}
