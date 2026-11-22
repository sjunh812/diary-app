package org.sjhstudio.diary.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * 테마 색이 바꾸는 브랜드색 자리 토큰 한 벌(스펙 010 FR-003)
 * 회색 구조, 기분 색, 삭제 빨강, 본문 글자색은 테마와 상관없음
 */
@Immutable
data class BrandTokens(
    val accent: Color,
    val accentIcon: Color,
    val onAccent: Color,
    val accentText: Color,
    val selected: Color,
    val surfaceCard: Color,
    val paper: Color,
    val envelopeFlap: Color,
    val lilac: Color,
    val toastAction: Color
)

/**
 * 테마 색. 라이트·다크 값을 따로 가짐
 * 정한 색은 고정 값(`specs/010-app-theme/research.md` "정한 7색"), 직접 고른 색만 [custom]으로 계산
 */
@Immutable
data class DiaryBrand(val light: BrandTokens, val dark: BrandTokens) {
    fun tokens(dark: Boolean): BrandTokens = if (dark) this.dark else light

    companion object {
        /** 지금 앱 값 그대로 */
        val Lavender =
            preset(
                light = LightSet(0x6E56CF, 0xFFFFFF, 0x6550B9, 0xEBE4FF, 0xF8F8FF, 0xCAA6FE, 0xCAA6FE),
                dark = DarkSet(0x6E56CF, 0xFFFFFF, 0xBAA7FF, 0x291F43, 0x433C5C, 0x3A2F63, 0xBAA7FF)
            )
        val Plum =
            preset(
                light = LightSet(0x88569D, 0xFFFFFF, 0x7B4F8E, 0xEFE4F4, 0xFAF8FC, 0xD0AAE0, 0xC6A1D6),
                dark = DarkSet(0x88569D, 0xFFFFFF, 0xCCA7DC, 0x2F2035, 0x493C4F, 0x452F4E, 0xCCA7DC)
            )
        val Rose =
            preset(
                light = LightSet(0xAA4A60, 0xFFFFFF, 0x9A4558, 0xFAE2E5, 0xFDF7F8, 0xEDA2AF, 0xE49BA8),
                dark = DarkSet(0xAA4A60, 0xFFFFFF, 0xE9A0AC, 0x3A1D22, 0x54393E, 0x542B33, 0xE9A0AC)
            )
        val Maple =
            preset(
                light = LightSet(0xA2552C, 0xFFFFFF, 0x924E2A, 0xF7E4DB, 0xFCF8F5, 0xE7AB8D, 0xDCA184),
                dark = DarkSet(0xA2552C, 0xFFFFFF, 0xE2A78A, 0x372014, 0x513C32, 0x51301E, 0xE2A78A)
            )
        val Forest =
            preset(
                light = LightSet(0x407552, 0xFFFFFF, 0x3C6A4B, 0xE1ECE3, 0xF7FAF7, 0x9FC5AA, 0x92B79C),
                dark = DarkSet(0x407552, 0xFFFFFF, 0x97BDA2, 0x1C2B20, 0x38453C, 0x293F30, 0x97BDA2)
            )
        val Sea =
            preset(
                light = LightSet(0x406E96, 0xFFFFFF, 0x3C6487, 0xE0EAF4, 0xF7F9FB, 0x9DBFDD, 0x91B2D0),
                dark = DarkSet(0x406E96, 0xFFFFFF, 0x97B8D6, 0x1B2834, 0x37434E, 0x283B4D, 0x97B8D6)
            )

        /** 다크에서는 밝은 회색 버튼에 어두운 글자로 뒤집힘 */
        val Black =
            preset(
                light = LightSet(0x2B2D33, 0xFFFFFF, 0x2B2D33, 0xDCDEE2, 0xF8F9F9, 0xB7BAC0, 0xC9CCD1),
                dark = DarkSet(0xE6E8EB, CANVAS_DARK, 0xE6E8EB, 0x3A3D44, 0x404243, 0x37393C, 0xE6E8EB)
            )

        /** 사용자가 고른 색(0xRRGGBB)에서 계산. 고를 때 한 번만 부름 */
        fun custom(rgb: Int): DiaryBrand = deriveBrand(rgb and RGB)

        /** lilac은 두 모드가 같고, 라이트 덮개는 selected와 같음. 정한 색 버튼은 바탕과 3:1이 넘어 아이콘도 같은 색 */
        private fun preset(light: LightSet, dark: DarkSet): DiaryBrand = DiaryBrand(
            light =
                BrandTokens(
                    accent = color(light.accent),
                    accentIcon = color(light.accent),
                    onAccent = color(light.onAccent),
                    accentText = color(light.accentText),
                    selected = color(light.selected),
                    surfaceCard = color(light.surfaceCard),
                    paper = color(PAPER_LIGHT),
                    envelopeFlap = color(light.selected),
                    lilac = color(light.lilac),
                    toastAction = color(light.toastAction)
                ),
            dark =
                BrandTokens(
                    accent = color(dark.accent),
                    accentIcon = color(dark.accent),
                    onAccent = color(dark.onAccent),
                    accentText = color(dark.accentText),
                    selected = color(dark.selected),
                    surfaceCard = color(SURFACE_CARD_DARK),
                    paper = color(dark.paper),
                    envelopeFlap = color(dark.envelopeFlap),
                    lilac = color(light.lilac),
                    toastAction = color(dark.toastAction)
                )
        )
    }
}

/** 정한 색 라이트 값(0xRRGGBB) */
private class LightSet(
    val accent: Int,
    val onAccent: Int,
    val accentText: Int,
    val selected: Int,
    val surfaceCard: Int,
    val lilac: Int,
    val toastAction: Int
)

/** 정한 색 다크 값(0xRRGGBB) */
private class DarkSet(
    val accent: Int,
    val onAccent: Int,
    val accentText: Int,
    val selected: Int,
    val paper: Int,
    val envelopeFlap: Int,
    val toastAction: Int
)

/**
 * 고른 색 하나로 라이트·다크 토큰 계산(`research.md` "계산 규칙")
 * 버튼은 고른 그대로, 위 글자는 흰색·검은색 중 APCA로 더 잘 보이는 쪽. 흰 바탕에 묻히는 거의 흰 색만 진하게
 */
internal fun deriveBrand(seed: Int): DiaryBrand {
    val source = seed.toOkLch()
    val neutral = source.c < NEUTRAL_CHROMA
    if (neutral && source.l < NEUTRAL_DARK) return DiaryBrand.Black
    val s = Seed(hue = if (neutral) NEUTRAL_HUE else source.h, chroma = if (neutral) NEUTRAL_C else minOf(source.c, MAX_CHROMA), neutral)
    // 채도 상한은 계산한 면에만. 버튼 면은 고른 색 그대로에서 시작
    val (accent, onAccent) = lightAccent(if (neutral) s.at(source.l) else seed, source.l, s)
    return DiaryBrand(light = lightTokens(s, accent, onAccent), dark = darkTokens(s, accent))
}

/** 고른 색의 색상과 채도. 면 색은 여기에 명도와 채도 비율을 줘서 만듦 */
private class Seed(val hue: Double, val chroma: Double, val neutral: Boolean) {
    fun at(l: Double, share: Double = 1.0) = okLchToRgb(l, chroma * share, hue)

    fun tone(t: Tone) = okLchToRgb(t.l, minOf(chroma * t.share, t.max), hue)
}

/** 명도 [l]에 고른 색 채도의 [share](최대 [max])를 쓰는 면 */
private class Tone(val l: Double, val share: Double, val max: Double)

private fun lightTokens(s: Seed, accent: Int, onAccent: Int): BrandTokens {
    val selected = s.tone(if (s.neutral) NEUTRAL_SELECTED else SELECTED)
    val accentText =
        searchLightness(s.chroma * TEXT_SHARE, textHue(s.hue, s.neutral), minOf(accent.toOkLch().l, TEXT_MAX_L), 0.0) {
            contrast(it, selected) >= ON_SELECTED && contrast(it, WHITE) >= BODY
        } ?: INK
    val toastAction = searchLightness(s.chroma * TOAST_SHARE, s.hue, MID_L, 1.0) { contrast(it, TOAST_LIGHT) >= ON_TOAST } ?: WHITE
    // 밝은 버튼 색은 흰 바탕·고른 칸 위 별·고리로 쓰면 묻혀 3:1까지만 진하게. 노랑은 글자처럼 호박 쪽으로 옮겨 올리브를 피함
    val icon = iconColor(accent, s, listOf(SUNKEN_LIGHT, selected), toward = 0.0, hue = textHue(s.hue, s.neutral)) ?: accentText
    return BrandTokens(
        accent = color(accent),
        accentIcon = color(icon),
        onAccent = color(onAccent),
        accentText = color(accentText),
        selected = color(selected),
        surfaceCard = color(s.tone(SURFACE_CARD)),
        paper = color(PAPER_LIGHT),
        envelopeFlap = color(selected),
        lilac = color(s.tone(PLUM)),
        toastAction = color(toastAction)
    )
}

/** 다크는 따로 계산. 버튼이 다크 바탕과 3:1이 안 되면 다크에서만 밝힘 */
private fun darkTokens(s: Seed, accent: Int): BrandTokens {
    val darkAccent =
        if (contrast(accent, CANVAS_DARK) >= LARGE) {
            accent
        } else {
            searchLightness(s.chroma, s.hue, accent.toOkLch().l, 1.0) { contrast(it, CANVAS_DARK) >= LARGE } ?: WHITE
        }
    val text = searchLightness(s.chroma * TOAST_SHARE, s.hue, MID_L, 1.0) { contrast(it, CANVAS_DARK) >= DARK_TEXT } ?: WHITE
    val selected = s.tone(if (s.neutral) NEUTRAL_SELECTED_DARK else SELECTED_DARK)
    // 다크는 카드 면·입력 칸·고른 칸이 바탕보다 밝아 그 위 별·고리는 3:1까지 밝힘
    val icon = iconColor(darkAccent, s, listOf(SURFACE_CARD_DARK, SUNKEN_DARK, selected), toward = 1.0) ?: text
    return BrandTokens(
        accent = color(darkAccent),
        accentIcon = color(icon),
        onAccent = color(textOn(darkAccent)),
        accentText = color(text),
        selected = color(selected),
        surfaceCard = color(SURFACE_CARD_DARK),
        paper = color(s.tone(PAPER_DARK)),
        envelopeFlap = color(s.tone(FLAP_DARK)),
        lilac = color(s.tone(PLUM)),
        toastAction = color(text)
    )
}

/** 라이트 버튼 색과 그 위 글자. 흰 바탕과 거의 같으면 버튼 면이 보이게 조금 진하게 */
private fun lightAccent(start: Int, l: Double, s: Seed): Pair<Int, Int> {
    val accent =
        if (contrast(start, WHITE) < NEAR_WHITE) {
            searchLightness(s.chroma, s.hue, l, 0.0) { contrast(it, WHITE) >= NEAR_WHITE } ?: start
        } else {
            start
        }
    return accent to textOn(accent)
}

/**
 * 버튼 위 흰 글자와 검은 글자 중 APCA로 더 잘 보이는 쪽
 * WCAG 2는 밝은 파랑 위 흰 글자를 실제보다 낮게 잡아 검은 글자를 고름
 */
private fun textOn(button: Int): Int = if (apca(WHITE, button) >= apca(INK, button)) WHITE else INK

/** [start]가 [backgrounds] 모두와 3:1이면 그대로, 아니면 색상 [hue]·같은 채도로 명도를 [toward] 쪽으로 옮긴 첫 색 */
private fun iconColor(start: Int, s: Seed, backgrounds: List<Int>, toward: Double, hue: Double = s.hue): Int? {
    fun clear(rgb: Int) = backgrounds.all { contrast(rgb, it) >= LARGE }
    return if (clear(start)) start else searchLightness(s.chroma, hue, start.toOkLch().l, toward, ::clear)
}

/** 노랑은 호박 쪽, 연두는 초록 쪽으로 옮겨 진하게 해도 올리브가 되지 않게 */
private fun textHue(h: Double, neutral: Boolean): Double = when {
    neutral -> h
    h > YELLOW_FROM && h <= YELLOW_TO -> maxOf(YELLOW_FROM, h - YELLOW_SHIFT)
    h > YELLOW_TO && h <= LIME_TO -> h + LIME_SHIFT
    else -> h
}

private fun color(rgb: Int) = Color(OPAQUE or rgb.toLong())

private val SELECTED = Tone(l = 0.932, share = 0.21, max = 0.045)

/** 회색 테마의 고른 칸은 `sunken`과 구별되게 더 진하게 */
private val NEUTRAL_SELECTED = Tone(l = 0.90, share = 0.21, max = 0.045)
private val SURFACE_CARD = Tone(l = 0.981, share = 0.05, max = 0.010)
private val PLUM = Tone(l = 0.789, share = 0.71, max = 0.13)
private val SELECTED_DARK = Tone(l = 0.271, share = 0.36, max = 0.065)
private val NEUTRAL_SELECTED_DARK = Tone(l = 0.36, share = 0.36, max = 0.065)
private val PAPER_DARK = Tone(l = 0.377, share = 0.30, max = 0.054)
private val FLAP_DARK = Tone(l = 0.345, share = 0.49, max = 0.088)

private const val OPAQUE = 0xFF000000
private const val RGB = 0xFFFFFF
private const val WHITE = 0xFFFFFF

/** 본문 글자색 `textPrimary` */
private const val INK = 0x1A1C20
private const val CANVAS_DARK = 0x16171B

/** 라이트에서 가장 진한 바탕 `sunken` */
private const val SUNKEN_LIGHT = 0xF3F4F5

/** 다크 입력 칸 바탕 `sunken`. 카드 면보다 밝음 */
private const val SUNKEN_DARK = 0x2B2E35
private const val TOAST_LIGHT = 0x2A3038
private const val PAPER_LIGHT = 0xFFFFFF
private const val SURFACE_CARD_DARK = 0x1D2025

private const val BODY = 4.5
private const val LARGE = 3.0
private const val NEAR_WHITE = 1.3

/** 브랜드 글자는 고른 칸 위에서도 또렷하게 */
private const val ON_SELECTED = 5.0
private const val ON_TOAST = 6.0

/** 라벤더 다크 브랜드 글자와 같은 대비 */
private const val DARK_TEXT = 8.6
private const val TEXT_SHARE = 0.9
private const val TEXT_MAX_L = 0.62
private const val TOAST_SHARE = 0.7
private const val MID_L = 0.5

private const val MAX_CHROMA = 0.19
private const val NEUTRAL_CHROMA = 0.03
private const val NEUTRAL_DARK = 0.5

/** SEED 회색과 같은 차가운 색조 */
private const val NEUTRAL_HUE = 262.0
private const val NEUTRAL_C = 0.012

private const val YELLOW_FROM = 70.0
private const val YELLOW_TO = 105.0
private const val LIME_TO = 125.0
private const val YELLOW_SHIFT = 25.0
private const val LIME_SHIFT = 20.0
