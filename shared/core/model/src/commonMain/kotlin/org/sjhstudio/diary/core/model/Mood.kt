package org.sjhstudio.diary.core.model

/**
 * 기분 9종. DB와 백업에는 소문자 [id]를 둠
 * 순서 번호로 두지 않아 종류를 추가해도 기존 값이 바뀌지 않음
 */
enum class Mood(
    val id: String,
    /** 레거시 `mood` 값 */
    val legacyCode: Int
) {
    ANGRY("angry", 0),
    COOL("cool", 1),
    SAD("sad", 2),
    SICK("sick", 3),
    LAUGH("laugh", 4),
    MEH("meh", 5),
    BAD("bad", 6),
    GOOD("good", 7),
    TIRED("tired", 8)
    ;

    companion object {
        /**
         * 화면에 늘어놓는 순서. 좋은 기분부터 힘든 기분까지 이어져 찾기 쉬움
         * [entries]는 옛 앱 번호 순서라 화면에 쓰지 않음
         */
        val ordered: List<Mood> = listOf(GOOD, LAUGH, COOL, MEH, TIRED, SAD, SICK, ANGRY, BAD)

        fun fromId(id: String?): Mood? = entries.firstOrNull { it.id == id }

        fun fromLegacyCode(code: Int): Mood? = entries.firstOrNull { it.legacyCode == code }
    }
}
