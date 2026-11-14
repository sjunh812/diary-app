package org.sjhstudio.diary.core.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** 앱이 백그라운드로 간 뒤 다시 잠그기까지의 시간. 기기 시각이 아니라 단조 시계로 측정(research R13) */
enum class RelockAfter(
    val id: String,
    /** null이면 시간으로 잠그지 않고 앱 프로세스가 새로 시작할 때만 잠금 */
    val duration: Duration?
) {
    IMMEDIATELY("immediately", 0.seconds),

    /** 기본값(스펙 001 FR-017) */
    ONE_MINUTE("one_minute", 1.minutes),
    FIVE_MINUTES("five_minutes", 5.minutes),
    COLD_START("cold_start", null)
    ;

    companion object {
        val DEFAULT = ONE_MINUTE

        fun fromId(id: String?): RelockAfter? = entries.firstOrNull { it.id == id }
    }
}
