package org.sjhstudio.diary.core.model

/**
 * 앱 테마 색(스펙 010)
 * 정한 7색은 무료, [CUSTOM]은 Plus가 직접 고른 색
 */
enum class ThemeColor(val id: String) {
    LAVENDER("lavender"),
    PLUM("plum"),
    ROSE("rose"),
    MAPLE("maple"),
    FOREST("forest"),
    SEA("sea"),
    BLACK("black"),
    CUSTOM("custom")
    ;

    companion object {
        val DEFAULT = LAVENDER

        /** 견본 차례. 직접 고르기는 맨 끝 칸 */
        val presets: List<ThemeColor> = entries - CUSTOM

        fun fromId(id: String?): ThemeColor? = entries.firstOrNull { it.id == id }
    }
}

/**
 * 테마 색 설정
 * 저장값은 원문 그대로 둠. 모르는 이름이나 깨진 값은 라벤더로 보이지만 사용자가 다시 고를 때까지 지우지 않음
 */
data class ThemeChoice(
    /** [ThemeColor.id]. 없으면 라벤더 */
    val colorId: String? = null,
    /** 직접 고른 색 `#RRGGBB`. 정한 색을 골라도 지우지 않고 기억함 */
    val customHex: String? = null
) {
    /** 고른 색. 모르는 이름이면 라벤더 */
    val color: ThemeColor get() = ThemeColor.fromId(colorId) ?: ThemeColor.DEFAULT

    /** 직접 고른 색(0xRRGGBB). 없거나 깨졌으면 null */
    val customRgb: Int? get() = ThemeHex.parse(customHex)
}

/** 정한 색을 고른 설정. 직접 고른 색은 기억해 둠 */
fun ThemeChoice.withPreset(color: ThemeColor): ThemeChoice = copy(colorId = color.id)

/** 직접 고른 색(0xRRGGBB)을 쓰는 설정 */
fun ThemeChoice.withCustom(rgb: Int): ThemeChoice = ThemeChoice(colorId = ThemeColor.CUSTOM.id, customHex = ThemeHex.format(rgb))

/** 직접 고른 색을 지운 설정. 쓰던 중이면 라벤더로, 모르는 이름은 그대로 */
fun ThemeChoice.withoutCustom(): ThemeChoice =
    ThemeChoice(colorId = if (colorId == ThemeColor.CUSTOM.id) ThemeColor.DEFAULT.id else colorId, customHex = null)

/** 직접 고른 색을 `#RRGGBB`로 적고 읽음 */
object ThemeHex {
    private val PATTERN = Regex("#?[0-9A-Fa-f]{6}")

    fun parse(text: String?): Int? = text?.trim()?.takeIf { PATTERN.matches(it) }?.removePrefix("#")?.toInt(HEX)

    fun format(rgb: Int): String = "#" + (rgb and RGB).toString(HEX).padStart(DIGITS, '0').uppercase()

    private const val HEX = 16
    private const val DIGITS = 6
    private const val RGB = 0xFFFFFF
}
