package org.sjhstudio.diary.core.platform

import kotlin.concurrent.Volatile
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import org.sjhstudio.diary.core.model.BackupFailure

/**
 * 받은 토큰을 잠깐 다시 씀. 사진마다 플랫폼 인증을 부르면 그것만으로 한 장에 시간이 듦.
 * 토큰이 만료돼 사진 여섯 장이 함께 물어도 [fetch]는 한 번만 돎.
 */
class DriveTokenCache(private val timeSource: TimeSource = TimeSource.Monotonic, private val fetch: suspend () -> TokenResult) {
    private class Kept(val token: TokenResult.Token, val until: TimeMark, val connection: Any)

    private val lock = Mutex()

    /** [clear]할 때마다 바뀜. 이 값이 다른 토큰은 지난 연결에서 받은 것 */
    @Volatile
    private var connection = Any()

    @Volatile
    private var kept: Kept? = null

    suspend fun get(): TokenResult = reusable() ?: lock.withLock { fetchOnce() }

    /** 연결을 바꾸거나 끊을 때, 드라이브가 토큰을 거절했을 때 부름 */
    fun clear() {
        connection = Any()
        kept = null
    }

    private suspend fun fetchOnce(): TokenResult {
        while (true) {
            // 앞선 호출이 받아 뒀으면 그것을 씀
            reusable()?.let { return it }
            val asked = connection
            // 응답이 오지 않아도 뒤에서 기다리는 호출과 백업이 함께 멈추지 않게
            val result = withTimeoutOrNull(FETCH_TIMEOUT) { fetch() } ?: TokenResult.Failed(BackupFailure.OFFLINE)
            // 받는 사이에 연결이 바뀌었으면 옛 계정의 결과라 넘기지도 남기지도 않고 다시 받음
            if (asked !== connection) continue
            if (result is TokenResult.Token) kept = Kept(result, timeSource.markNow() + REUSE_FOR, asked)
            return result
        }
    }

    private fun reusable(): TokenResult.Token? = kept?.takeIf { it.connection === connection && it.until.hasNotPassedNow() }?.token

    private companion object {
        val REUSE_FOR = 5.minutes
        val FETCH_TIMEOUT = 60.seconds
    }
}
