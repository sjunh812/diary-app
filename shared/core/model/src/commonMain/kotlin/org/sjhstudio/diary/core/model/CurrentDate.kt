package org.sjhstudio.diary.core.model

import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/**
 * 지금 기기 시간대의 오늘.
 * 시험은 [clock]과 [zone]을 넘겨 날짜를 고정한다.
 */
fun currentDate(clock: Clock = Clock.System, zone: TimeZone = TimeZone.currentSystemDefault()): LocalDate = clock.todayIn(zone)
