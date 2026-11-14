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
    HOME_LIST_STYLE("home_list_style"),

    /** 사진을 길게 눌러 끌면 순서를 바꿀 수 있다는 안내. 사진이 둘 이상일 때 */
    PHOTO_REORDER("photo_reorder")
}

/** 안내마다 따로, 닫거나 할 일을 누를 때까지 보임(FR-011) */
interface ContextTips {
    /** 아직 닫지 않은 안내면 true */
    suspend fun claim(tip: ContextTip): Boolean

    suspend fun done(tip: ContextTip)
}
