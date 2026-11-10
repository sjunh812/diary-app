package org.sjhstudio.diary.core.platform

import kotlin.time.Instant

/**
 * 매일 알림 예약(스펙 001 FR-022, research R15).
 * Android는 다음 한 번만 예약하고 알림이 오면 그다음을 다시 예약.
 */
interface ReminderScheduler {
    fun schedule(at: Instant)

    fun cancel()
}

/** 알림을 예약하지 않는 구현. 시험과 미리보기에서 씀 */
object NoReminderScheduler : ReminderScheduler {
    override fun schedule(at: Instant) = Unit

    override fun cancel() = Unit
}
