package org.sjhstudio.diary.feature.settings

import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.sjhstudio.diary.core.model.DiaryFont
import org.sjhstudio.diary.feature.settings.resources.Res
import org.sjhstudio.diary.feature.settings.resources.font_name_busan_dandi_myeongjo
import org.sjhstudio.diary.feature.settings.resources.font_name_busan_dandi_myeongjo_ko
import org.sjhstudio.diary.feature.settings.resources.font_name_busan_dandi_myeongjo_latin
import org.sjhstudio.diary.feature.settings.resources.font_name_changwon_round
import org.sjhstudio.diary.feature.settings.resources.font_name_gowun_batang
import org.sjhstudio.diary.feature.settings.resources.font_name_gowun_batang_ko
import org.sjhstudio.diary.feature.settings.resources.font_name_gowun_batang_latin
import org.sjhstudio.diary.feature.settings.resources.font_name_hakgyoansim_badasseugi
import org.sjhstudio.diary.feature.settings.resources.font_name_nanum_cheer
import org.sjhstudio.diary.feature.settings.resources.font_name_nanum_jeomggol
import org.sjhstudio.diary.feature.settings.resources.font_name_nanum_jeomggol_ko
import org.sjhstudio.diary.feature.settings.resources.font_name_nanum_jeomggol_latin
import org.sjhstudio.diary.feature.settings.resources.font_name_ownglyph_wiseelist
import org.sjhstudio.diary.feature.settings.resources.font_name_ownglyph_wiseelist_ko
import org.sjhstudio.diary.feature.settings.resources.font_name_ownglyph_wiseelist_latin
import org.sjhstudio.diary.feature.settings.resources.font_name_pretendard
import org.sjhstudio.diary.feature.settings.resources.font_name_rix_watermelon
import org.sjhstudio.diary.feature.settings.resources.font_name_sejong_geulggot
import org.sjhstudio.diary.feature.settings.resources.font_name_sejong_geulggot_ko
import org.sjhstudio.diary.feature.settings.resources.font_name_sejong_geulggot_latin
import org.sjhstudio.diary.feature.settings.resources.font_name_system

/**
 * 글꼴 목록 순서(Figma "글꼴 (G1)")
 * 시스템 글꼴이 맨 위
 */
internal val FONT_ORDER: List<DiaryFont> = listOf(DiaryFont.SYSTEM) + DiaryFont.entries.filter { it != DiaryFont.SYSTEM }

/**
 * 받는 글꼴의 크기(공식 배포 파일, `docs/design.md` 일기 본문 글꼴 10종)
 * 받기 버튼에 보임
 */
internal val FONT_DOWNLOAD_MB: Map<DiaryFont, Double> =
    mapOf(
        DiaryFont.BUSAN_DANDI_MYEONGJO to 7.0,
        DiaryFont.GOWUN_BATANG to 8.4,
        DiaryFont.OWNGLYPH_WISEELIST to 7.2,
        DiaryFont.NANUM_JEOMGGOL to 6.7,
        DiaryFont.SEJONG_GEULGGOT to 7.8
    )

internal fun DiaryFont.label(): StringResource = when (this) {
    DiaryFont.SYSTEM -> Res.string.font_name_system
    DiaryFont.PRETENDARD -> Res.string.font_name_pretendard
    DiaryFont.HAKGYOANSIM_BADASSEUGI -> Res.string.font_name_hakgyoansim_badasseugi
    DiaryFont.CHANGWON_ROUND -> Res.string.font_name_changwon_round
    DiaryFont.BUSAN_DANDI_MYEONGJO -> Res.string.font_name_busan_dandi_myeongjo
    DiaryFont.GOWUN_BATANG -> Res.string.font_name_gowun_batang
    DiaryFont.OWNGLYPH_WISEELIST -> Res.string.font_name_ownglyph_wiseelist
    DiaryFont.NANUM_JEOMGGOL -> Res.string.font_name_nanum_jeomggol
    DiaryFont.NANUM_CHEER -> Res.string.font_name_nanum_cheer
    DiaryFont.RIX_WATERMELON -> Res.string.font_name_rix_watermelon
    DiaryFont.SEJONG_GEULGGOT -> Res.string.font_name_sejong_geulggot
}

/**
 * 받기 전 글꼴의 이름 그림(그 글꼴로 그린 이름, 투명 바탕)
 * 한국어 화면은 한국어 이름, 그 밖은 로마자 이름
 * 이 글꼴들에는 가나가 없어 일본어 화면도 로마자 이름을 씀
 */
internal fun DiaryFont.namePreview(korean: Boolean): DrawableResource? = when (this) {
    DiaryFont.BUSAN_DANDI_MYEONGJO ->
        if (korean) Res.drawable.font_name_busan_dandi_myeongjo_ko else Res.drawable.font_name_busan_dandi_myeongjo_latin

    DiaryFont.GOWUN_BATANG -> if (korean) Res.drawable.font_name_gowun_batang_ko else Res.drawable.font_name_gowun_batang_latin

    DiaryFont.OWNGLYPH_WISEELIST ->
        if (korean) Res.drawable.font_name_ownglyph_wiseelist_ko else Res.drawable.font_name_ownglyph_wiseelist_latin

    DiaryFont.NANUM_JEOMGGOL -> if (korean) Res.drawable.font_name_nanum_jeomggol_ko else Res.drawable.font_name_nanum_jeomggol_latin

    DiaryFont.SEJONG_GEULGGOT -> if (korean) Res.drawable.font_name_sejong_geulggot_ko else Res.drawable.font_name_sejong_geulggot_latin

    else -> null
}
