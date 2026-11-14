package org.sjhstudio.diary.core.platform

import kotlin.time.Instant

/**
 * 매일 알림 예약(스펙 001 FR-022, research R15)
 * Android는 다음 한 번만 예약하고 알림이 오면 그다음을 다시 예약
 */
interface ReminderScheduler {
    fun schedule(at: Instant)

    fun cancel()
}
