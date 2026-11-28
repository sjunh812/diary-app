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
         * 화면에 늘어놓는 순서. 고르기 묶음 차례([MoodGroup])
         * 쓰기 시트와 통계 기준 시트가 같은 순서로 읽힘. [entries]는 추가한 순서라 화면에 쓰지 않음
         */
        val ordered: List<Mood> by lazy { MoodGroup.entries.flatMap { it.moods } }

        fun fromId(id: String?): Mood? = entries.firstOrNull { it.id == id }

        fun fromLegacyCode(code: Int): Mood? = entries.firstOrNull { it.legacyCode == code }
    }
}

/**
 * 기분 고르기 묶음. 시트 제목 "기분은 어땠나요?"에 답하는 이름을 붙임
 * 마음·몸 묶음은 통계에서 모두 부정적인 기분. 묶음 안은 색 차례
 */
enum class MoodGroup(val moods: List<Mood>) {
    GOOD(listOf(Mood.EXCITED, Mood.GOOD, Mood.LAUGH, Mood.PROUD, Mood.CALM)),
    MIND(listOf(Mood.ANGRY, Mood.ANNOYED, Mood.ANXIOUS, Mood.SAD, Mood.BAD)),
    BODY(listOf(Mood.SICK, Mood.TIRED)),
    NEUTRAL(listOf(Mood.COOL, Mood.MEH, Mood.BORED))
}
