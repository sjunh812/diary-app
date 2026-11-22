package org.sjhstudio.diary.core.designsystem.theme

import kotlin.math.abs
import kotlin.math.pow

/** 상대 휘도(WCAG 2) */
internal fun luminance(rgb: Int): Double {
    val (r, g, b) = linearRgb(rgb)
    return LUMA_RED * r + LUMA_GREEN * g + LUMA_BLUE * b
}

/** WCAG 2 대비 */
internal fun contrast(a: Int, b: Int): Double {
    val x = luminance(a)
    val y = luminance(b)
    return (maxOf(x, y) + FLARE) / (minOf(x, y) + FLARE)
}

/**
 * APCA 밝기 대비 |Lc|(WCAG 3 초안, 0.0.98G)
 * 파랑 위 흰 글자처럼 WCAG 2가 낮게 잡는 색을 눈에 가깝게 잼
 */
internal fun apca(text: Int, background: Int): Double {
    val t = apcaY(text)
    val b = apcaY(background)
    val s =
        if (b > t) {
            (b.pow(NORMAL_BG) - t.pow(NORMAL_TEXT)) * APCA_SCALE
        } else {
            (b.pow(REVERSE_BG) - t.pow(REVERSE_TEXT)) * APCA_SCALE
        }
    return if (abs(s) < APCA_LOW_CLIP) 0.0 else (abs(s) - APCA_OFFSET) * APCA_PERCENT
}

/** APCA 화면 밝기. 아주 어두운 색은 화면 반사광만큼 올림 */
private fun apcaY(rgb: Int): Double {
    val y =
        APCA_RED * channel(rgb shr RED_SHIFT) + APCA_GREEN * channel(rgb shr GREEN_SHIFT) + APCA_BLUE * channel(rgb)
    return if (y > BLACK_THRESHOLD) y else y + (BLACK_THRESHOLD - y).pow(BLACK_CLAMP)
}

private fun channel(v: Int) = ((v and CHANNEL_MAX) / CHANNEL_MAX.toDouble()).pow(APCA_GAMMA)

/**
 * [from]에서 [to]로 OKLCH 명도를 고르게 훑어 [ok]를 처음 만족하는 색
 * 큰 쪽에서 내려오면 가장 밝은 값, 작은 쪽에서 올라가면 가장 어두운 값
 */
internal fun searchLightness(c: Double, h: Double, from: Double, to: Double, ok: (Int) -> Boolean): Int? {
    for (i in 0..SEARCH_STEPS) {
        val rgb = okLchToRgb(from + (to - from) * i / SEARCH_STEPS, c, h)
        if (ok(rgb)) return rgb
    }
    return null
}

private const val LUMA_RED = 0.2126
private const val LUMA_GREEN = 0.7152
private const val LUMA_BLUE = 0.0722
private const val FLARE = 0.05
private const val SEARCH_STEPS = 400

private const val RED_SHIFT = 16
private const val GREEN_SHIFT = 8
private const val CHANNEL_MAX = 0xFF
private const val APCA_GAMMA = 2.4
private const val APCA_RED = 0.2126729
private const val APCA_GREEN = 0.7151522
private const val APCA_BLUE = 0.0721750
private const val BLACK_THRESHOLD = 0.022
private const val BLACK_CLAMP = 1.414
private const val NORMAL_BG = 0.56
private const val NORMAL_TEXT = 0.57
private const val REVERSE_BG = 0.65
private const val REVERSE_TEXT = 0.62
private const val APCA_SCALE = 1.14
private const val APCA_LOW_CLIP = 0.1
private const val APCA_OFFSET = 0.027
private const val APCA_PERCENT = 100
