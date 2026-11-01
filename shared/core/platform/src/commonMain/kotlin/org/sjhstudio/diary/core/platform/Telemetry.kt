package org.sjhstudio.diary.core.platform

import org.sjhstudio.diary.core.model.MigrationStep

/**
 * 사용 통계와 비정상 종료 보고를 실제로 보내는 쪽.
 * Android는 Firebase Analytics·Crashlytics, iOS는 Swift가 Firebase iOS SDK로 구현해 넘긴다(research R21).
 */
interface TelemetrySink {
    fun log(event: String, params: Map<String, String>)

    /** Crashlytics 사용자 정의 키. `migration_step` 하나만 쓴다. */
    fun setCustomKey(key: String, value: String)
}

/** Firebase 설정 파일이 없는 빌드와 시험에서 쓴다. 아무것도 보내지 않는다. */
object NoTelemetrySink : TelemetrySink {
    override fun log(event: String, params: Map<String, String>) = Unit

    override fun setCustomKey(key: String, value: String) = Unit
}

/**
 * 고정 표([AnalyticsEvents])에 있는 이벤트와 값만 보낸다.
 * 표에 없으면 개발 빌드([strict])에서는 예외를 던져 바로 알아채게 하고, 출시 빌드에서는 조용히 버린다.
 */
class Telemetry(private val sink: TelemetrySink, private val strict: Boolean) {
    fun log(event: String, params: Map<String, String> = emptyMap()) {
        val violation = AnalyticsEvents.violation(event, params)
        if (violation != null) {
            if (strict) throw IllegalArgumentException(violation)
            return
        }
        sink.log(event, params)
    }

    fun setMigrationStep(step: MigrationStep) = sink.setCustomKey(MIGRATION_STEP_KEY, step.id)

    private companion object {
        const val MIGRATION_STEP_KEY = "migration_step"
    }
}
