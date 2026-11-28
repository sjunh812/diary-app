package org.sjhstudio.diary.core.model

/**
 * 기분 얼굴 테마(스펙 013). 테마 화면 차례 그대로
 * [id]는 저장 값이라 바꾸지 않음
 */
enum class MoodTheme(val id: String, val plus: Boolean) {
    BASIC("basic", false),
    SHAPE("shape", false),
    GOUACHE("gouache", false),
    LINE("line", false),
    DOT("dot", false),
    BUBBLE("bubble", true),
    DOODLE("doodle", true),
    VEGGIE("veggie", true),
    CLAY("clay", true)
    ;

    companion object {
        val DEFAULT = BASIC

        fun fromId(id: String?): MoodTheme? = entries.firstOrNull { it.id == id }
    }
}

/** 클레이 얼굴 머리 모양. 머리색은 늘 기분 색 */
enum class ClayHair(val id: String) {
    BOB("bob"),
    LONG("long"),
    SHORT("short"),
    DANDY("dandy"),
    PART("part"),
    BUN("bun"),
    PONYTAIL("ponytail"),
    CURLY("curly"),
    BUZZ("buzz")
    ;

    companion object {
        val DEFAULT = BOB

        fun fromId(id: String?): ClayHair? = entries.firstOrNull { it.id == id }
    }
}

/** 클레이 얼굴 피부톤. 밝은 쪽부터 */
enum class ClaySkin(val id: String) {
    SKIN1("skin1"),
    SKIN2("skin2"),
    SKIN3("skin3"),
    SKIN4("skin4"),
    SKIN5("skin5"),
    SKIN6("skin6")
    ;

    companion object {
        /** Figma "밝음" 단계 */
        val DEFAULT = SKIN2

        fun fromId(id: String?): ClaySkin? = entries.firstOrNull { it.id == id }
    }
}

/**
 * 기분 테마 설정
 * 원문 그대로 둠. 모르는 id는 기본으로 보이지만 사용자가 다시 고를 때까지 지우지 않음
 */
data class MoodLook(
    val themeId: String? = null,
    val clayHair: String? = null,
    val claySkin: String? = null,
    val clayGlasses: Boolean? = null,
    /** 도형 테마 스티커 효과 */
    val shapeSticker: Boolean? = null
) {
    val theme: MoodTheme get() = MoodTheme.fromId(themeId) ?: MoodTheme.DEFAULT
    val hair: ClayHair get() = ClayHair.fromId(clayHair) ?: ClayHair.DEFAULT
    val skin: ClaySkin get() = ClaySkin.fromId(claySkin) ?: ClaySkin.DEFAULT
    val glasses: Boolean get() = clayGlasses ?: false
    val sticker: Boolean get() = shapeSticker ?: false

    /** 보이는 테마. Plus 테마인데 Plus가 아니면 기본 */
    fun shownTheme(plusAllowed: Boolean): MoodTheme = theme.takeIf { !it.plus || plusAllowed } ?: MoodTheme.DEFAULT

    fun withTheme(theme: MoodTheme): MoodLook = copy(themeId = theme.id)

    /** 넘긴 칸만 바꿈. 나머지는 모르는 id여도 원문 그대로 */
    fun withClay(hair: ClayHair? = null, skin: ClaySkin? = null, glasses: Boolean? = null): MoodLook =
        copy(clayHair = hair?.id ?: clayHair, claySkin = skin?.id ?: claySkin, clayGlasses = glasses ?: clayGlasses)

    fun withSticker(on: Boolean): MoodLook = copy(shapeSticker = on)

    /** Plus 테마를 쓰면 기본으로. 클레이 꼴은 다시 Plus가 되면 돌아오게 남김 */
    fun withoutPlusTheme(): MoodLook = if (theme.plus) copy(themeId = MoodTheme.DEFAULT.id) else this
}
