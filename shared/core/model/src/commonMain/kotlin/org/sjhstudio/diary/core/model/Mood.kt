package org.sjhstudio.diary.core.model

/**
 * 기분 9종.
 * DB와 백업에는 소문자 [id]를 둔다.
 * 순서 번호로 두지 않아 종류를 더해도 기존 값이 바뀌지 않는다.
 */
enum class Mood(
    val id: String,
    /** 레거시 `mood` 값. */
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
        fun fromId(id: String?): Mood? = entries.firstOrNull { it.id == id }

        fun fromLegacyCode(code: Int): Mood? = entries.firstOrNull { it.legacyCode == code }
    }
}
