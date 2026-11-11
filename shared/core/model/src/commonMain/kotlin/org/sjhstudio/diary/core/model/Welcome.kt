package org.sjhstudio.diary.core.model

/** 앱을 연 뒤 일기 전에 지나는 곳(스펙 004) */
enum class WelcomeStep { LETTER, ONBOARDING_UPDATED, ONBOARDING_NEW, DONE }

/** 처음 만날 때 한 번 보이는 팁. [id]가 저장 값 */
enum class ContextTip(val id: String) {
    /**
     * 돌아보기에서 칸과 기분 막대를 누르면 일기를 모아 볼 수 있다는 안내
     * 예전 "insight_mood"(기분 막대만 알리던 것)를 대신함
     * 이름을 새로 지어 예전 안내를 닫은 사람에게도 한 번 더 보임
     */
    INSIGHT_OPEN("insight_open"),

    /** 일기 목록을 카드나 사진 격자로도 볼 수 있다는 안내. 설정 안에만 있어 모르는 사람이 많음 */
    HOME_LIST_STYLE("home_list_style")
}

/** 세션당 하나, 팁마다 한 번(FR-011) */
interface ContextTips {
    /** 지금 보여도 되면 true. 한 세션에 하나만 받음 */
    suspend fun claim(tip: ContextTip): Boolean

    suspend fun done(tip: ContextTip)
}
