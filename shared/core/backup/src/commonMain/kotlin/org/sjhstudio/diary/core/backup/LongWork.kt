package org.sjhstudio.diary.core.backup

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * 앱을 나가도 끝까지 이어야 하는 일(백업, 복원, 파일 만들기·가져오기)
 * 도는 동안 플랫폼이 시스템에 시간을 받아 둠
 */
class LongWork {
    private val count = MutableStateFlow(0)

    /** 하나라도 돌면 true */
    val running: Flow<Boolean> = count.map { it > 0 }.distinctUntilChanged()

    suspend fun <T> holding(block: suspend () -> T): T {
        begin()
        return try {
            block()
        } finally {
            end()
        }
    }

    internal fun begin() = count.update { it + 1 }

    internal fun end() = count.update { it - 1 }
}
