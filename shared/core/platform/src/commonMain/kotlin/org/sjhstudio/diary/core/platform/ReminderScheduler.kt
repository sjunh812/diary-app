package org.sjhstudio.diary.core.platform

import kotlin.time.Instant

/**
 * 매일 알림 예약(스펙 001 FR-022, research R15).
 * Android는 다음 한 번만 예약하고 알림이 오면 그다음을 다시 예약한다.
 */
interface ReminderScheduler {
    fun schedule(at: Instant)

    fun cancel()
}

/**
 * 예약할 곳이 아직 없는 플랫폼.
 * iOS 알림 예약은 알림 설정 화면과 함께 붙인다.
 */
object NoReminderScheduler : ReminderScheduler {
    override fun schedule(at: Instant) = Unit

    override fun cancel() = Unit
}
