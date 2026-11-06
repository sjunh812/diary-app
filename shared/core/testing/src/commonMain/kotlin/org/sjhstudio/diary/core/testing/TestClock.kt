package org.sjhstudio.diary.core.testing

import kotlin.time.Clock
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration
import kotlin.time.Instant
import kotlin.time.TestTimeSource
import kotlin.time.TimeSource

/**
 * 시험용 시계.
 *
 * 벽시계([now])와 단조 시계([markNow])를 [advance]로 함께 움직인다.
 * 다시 잠그기는 단조 시계로 재므로(research R13), 사용자가 기기 시각을 바꾼 경우는 [setWallClock]으로 벽시계만 옮겨 본다.
 */
class TestClock(start: Instant = Instant.parse("2026-11-01T00:00:00Z")) :
    Clock,
    TimeSource.WithComparableMarks {
    private val monotonic = TestTimeSource()
    private var wall = start

    override fun now(): Instant = wall

    override fun markNow(): ComparableTimeMark = monotonic.markNow()

    /** 실제로 시간이 흐른 것처럼 두 시계를 같이 움직인다. */
    fun advance(duration: Duration) {
        require(!duration.isNegative()) { "시간은 뒤로 흐르지 않는다: $duration" }
        wall += duration
        monotonic += duration
    }

    /**
     * 사용자가 기기 시각을 바꾼 것처럼 벽시계만 옮긴다.
     * 단조 시계는 그대로다.
     */
    fun setWallClock(instant: Instant) {
        wall = instant
    }
}
