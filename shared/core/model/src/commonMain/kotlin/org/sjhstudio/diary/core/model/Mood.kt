package org.sjhstudio.diary.core.model

/**
 * 기분 15종. DB와 백업에는 소문자 [id]를 둠
 * 순서 번호로 두지 않아 종류를 추가해도 기존 값이 바뀌지 않음
 */
enum class Mood(
    val id: String,
    /** 레거시 `mood` 값. 1.7.4에 없던 기분은 null */
    val legacyCode: Int?
) {
    ANGRY("angry", 0),
    COOL("cool", 1),
    SAD("sad", 2),
    SICK("sick", 3),
    LAUGH("laugh", 4),
    MEH("meh", 5),
    BAD("bad", 6),
    GOOD("good", 7),
    TIRED("tired", 8),
    EXCITED("excited", null),
    PROUD("proud", null),
    CALM("calm", null),
    BORED("bored", null),
    ANXIOUS("anxious", null),
    ANNOYED("annoyed", null)
    ;

    companion object {
        /**
         * 화면에 늘어놓는 순서. 좋은 기분, 힘든 기분, 세지 않는 기분 차례이고 묶음 안은 색 차례(무지개)
         * 쓰기 시트와 통계 기준 시트가 같은 순서로 읽힘. [entries]는 추가한 순서라 화면에 쓰지 않음
         */
        val ordered: List<Mood> = listOf(
            EXCITED, GOOD, LAUGH, PROUD, CALM,
            ANGRY, ANNOYED, SICK, TIRED, SAD,
            ANXIOUS, BAD, COOL, MEH, BORED
        )

        fun fromId(id: String?): Mood? = entries.firstOrNull { it.id == id }

        fun fromLegacyCode(code: Int): Mood? = entries.firstOrNull { it.legacyCode == code }
    }
}
