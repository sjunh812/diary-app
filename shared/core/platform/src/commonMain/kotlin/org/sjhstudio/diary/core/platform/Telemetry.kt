package org.sjhstudio.diary.core.platform

import org.sjhstudio.diary.core.model.MigrationStep

/**
 * 사용 통계와 비정상 종료 보고를 실제로 보내는 쪽
 * Android는 Firebase Analytics·Crashlytics, iOS는 Swift가 Firebase iOS SDK로 구현해 넘김(research R21)
 */
interface TelemetrySink {
    fun log(event: String, params: Map<String, String>)

    /**
     * Crashlytics 사용자 정의 키. `migration_step`, `screen`, `recent_screens`만 씀
     * 값은 표에 있는 이름뿐
     */
    fun setCustomKey(key: String, value: String)

    /**
     * 앱이 받아 넘긴 예외를 비정상 종료 보고의 "처리된 예외"로 남김
     * 메시지에는 일기 글이 섞일 수 있어 구현은 예외 종류와 위치만 보냄(헌법 II)
     */
    fun recordCaught(error: Throwable) = Unit
}

/** Firebase 설정 파일이 없는 빌드와 시험에서 씀. 아무것도 보내지 않음 */
object NoTelemetrySink : TelemetrySink {
    override fun log(event: String, params: Map<String, String>) = Unit

    override fun setCustomKey(key: String, value: String) = Unit
}

/**
 * 고정 표([AnalyticsEvents])에 있는 이벤트와 값만 보냄
 * 표에 없으면 개발 빌드([strict])에서는 예외를 던져 바로 알아채게 하고, 출시 빌드에서는 조용히 버림
 */
class Telemetry(private val sink: TelemetrySink, private val strict: Boolean) {
    private val recentScreens = ArrayDeque<String>()

    fun log(event: String, params: Map<String, String> = emptyMap()) {
        val violation = AnalyticsEvents.violation(event, params)
        if (violation != null) {
            if (strict) throw IllegalArgumentException(violation)
            return
        }
        sink.log(event, params)
    }

    /** 화면 모델의 안전망이 받은 예외. 앱은 꺼지지 않았지만 고칠 곳이 있다는 뜻 */
    fun caught(error: Throwable) = sink.recordCaught(error)

    fun setMigrationStep(step: MigrationStep) = sink.setCustomKey(MIGRATION_STEP_KEY, step.id)

    /**
     * 보는 화면을 알림
     * 비정상 종료 보고에서 어디서 어떻게 왔는지 보이게 지금 화면과 지나온 화면을 키로 남김
     * 화면 키 이름만 쓰고 일기 번호 같은 인자는 넣지 않음
     */
    fun screen(name: String) {
        val params = mapOf("screen" to name)
        log(AnalyticsEvents.SCREEN_VIEW, params)
        if (AnalyticsEvents.violation(AnalyticsEvents.SCREEN_VIEW, params) != null) return
        recentScreens.addLast(name)
        if (recentScreens.size > RECENT_SCREENS) recentScreens.removeFirst()
        sink.setCustomKey(SCREEN_KEY, name)
        sink.setCustomKey(RECENT_SCREENS_KEY, recentScreens.joinToString(" > "))
    }

    private companion object {
        const val MIGRATION_STEP_KEY = "migration_step"
        const val SCREEN_KEY = "screen"
        const val RECENT_SCREENS_KEY = "recent_screens"
        const val RECENT_SCREENS = 8
    }
}
