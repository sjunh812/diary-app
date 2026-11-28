package org.sjhstudio.diary.core.testing

import org.sjhstudio.diary.core.platform.NoTelemetrySink
import org.sjhstudio.diary.core.platform.Telemetry

/** 시험용 사용 통계. 보내지 않고, 고정 표에 없는 이름은 바로 실패 */
val TestTelemetry = Telemetry(NoTelemetrySink, strict = true)
