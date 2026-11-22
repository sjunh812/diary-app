package org.sjhstudio.diary.core.designsystem.theme

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * OKLCH 색(Björn Ottosson, CSS Color 4)
 * 색은 0xRRGGBB 정수로 다룸. 알파는 쓰지 않음
 */
internal data class OkLch(val l: Double, val c: Double, val h: Double) {
    companion object {
        // 선형 sRGB → LMS
        val TO_LMS =
            arrayOf(
                doubleArrayOf(0.4122214708, 0.5363325363, 0.0514459929),
                doubleArrayOf(0.2119034982, 0.6806995451, 0.1073969566),
                doubleArrayOf(0.0883024619, 0.2817188376, 0.6299787005)
            )

        // LMS(세제곱근) → Lab
        val TO_LAB =
            arrayOf(
                doubleArrayOf(0.2104542553, 0.7936177850, -0.0040720468),
                doubleArrayOf(1.9779984951, -2.4285922050, 0.4505937099),
                doubleArrayOf(0.0259040371, 0.7827717662, -0.8086757660)
            )

        // Lab → LMS(세제곱근)
        val FROM_LAB =
            arrayOf(
                doubleArrayOf(1.0, 0.3963377774, 0.2158037573),
                doubleArrayOf(1.0, -0.1055613458, -0.0638541728),
                doubleArrayOf(1.0, -0.0894841775, -1.2914855480)
            )

        // LMS → 선형 sRGB
        val FROM_LMS =
            arrayOf(
                doubleArrayOf(4.0767416621, -3.3077115913, 0.2309699292),
                doubleArrayOf(-1.2684380046, 2.6097574011, -0.3413193965),
                doubleArrayOf(-0.0041960863, -0.7034186147, 1.7076147010)
            )

        // sRGB 전달 함수(IEC 61966-2-1)
        const val ENCODE_LIMIT = 0.0031308
        const val DECODE_LIMIT = 0.04045
        const val LINEAR_SLOPE = 12.92
        const val GAMMA = 2.4
        const val OFFSET = 0.055
    }
}

internal fun Int.toOkLch(): OkLch {
    val lms = OkLch.TO_LMS.times(linearRgb(this)).map(::cbrt).toDoubleArray()
    val (l, a, b) = OkLch.TO_LAB.times(lms)
    val hue = (atan2(b, a) * HALF_TURN / PI).let { if (it < 0) it + FULL_TURN else it }
    return OkLch(l, hypot(a, b), hue)
}

/** sRGB 밖이면 명도·색상은 두고 채도만 줄임(토스 TDS와 같은 방식) */
internal fun okLchToRgb(l: Double, c: Double, h: Double): Int {
    if (inGamut(toLinear(l, c, h))) return toRgb(toLinear(l, c, h))
    var lo = 0.0
    var hi = c
    repeat(GAMUT_STEPS) {
        val mid = (lo + hi) / 2
        if (inGamut(toLinear(l, mid, h))) lo = mid else hi = mid
    }
    return toRgb(toLinear(l, lo, h))
}

/** 0xRRGGBB → 선형 sRGB 세 값 */
internal fun linearRgb(rgb: Int): DoubleArray = doubleArrayOf(
    decode(rgb shr RED_SHIFT and CHANNEL),
    decode(rgb shr GREEN_SHIFT and CHANNEL),
    decode(rgb and CHANNEL)
)

private fun toLinear(l: Double, c: Double, h: Double): DoubleArray {
    val rad = h * PI / HALF_TURN
    val lms = OkLch.FROM_LAB.times(doubleArrayOf(l, c * cos(rad), c * sin(rad))).map { it.pow(CUBE) }.toDoubleArray()
    return OkLch.FROM_LMS.times(lms)
}

private fun inGamut(linear: DoubleArray): Boolean = linear.all { it in -GAMUT_EPSILON..1 + GAMUT_EPSILON }

private fun toRgb(linear: DoubleArray): Int {
    val (r, g, b) = linear.map { encode(it.coerceIn(0.0, 1.0)) }
    return (r shl RED_SHIFT) or (g shl GREEN_SHIFT) or b
}

private fun encode(linear: Double): Int {
    val s = if (linear <=
        OkLch.ENCODE_LIMIT
    ) {
        OkLch.LINEAR_SLOPE * linear
    } else {
        (1 + OkLch.OFFSET) * linear.pow(1 / OkLch.GAMMA) - OkLch.OFFSET
    }
    return (s.coerceIn(0.0, 1.0) * CHANNEL).roundToInt()
}

private fun decode(channel: Int): Double {
    val s = channel / CHANNEL.toDouble()
    return if (s <= OkLch.DECODE_LIMIT) s / OkLch.LINEAR_SLOPE else ((s + OkLch.OFFSET) / (1 + OkLch.OFFSET)).pow(OkLch.GAMMA)
}

private fun Array<DoubleArray>.times(v: DoubleArray): DoubleArray = DoubleArray(size) { row ->
    this[row].indices.sumOf {
        this[row][it] *
            v[it]
    }
}

private const val GAMUT_STEPS = 30
private const val GAMUT_EPSILON = 1e-6
private const val HALF_TURN = 180
private const val FULL_TURN = 360
private const val CUBE = 3
private const val CHANNEL = 0xFF
private const val RED_SHIFT = 16
private const val GREEN_SHIFT = 8
