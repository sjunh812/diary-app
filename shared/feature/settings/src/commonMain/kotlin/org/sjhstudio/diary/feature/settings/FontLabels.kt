package org.sjhstudio.diary.feature.settings

import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.sjhstudio.diary.core.model.DiaryFont
import org.sjhstudio.diary.feature.settings.resources.Res
import org.sjhstudio.diary.feature.settings.resources.font_name_gamja_flower
import org.sjhstudio.diary.feature.settings.resources.font_name_gamja_flower_ko
import org.sjhstudio.diary.feature.settings.resources.font_name_gamja_flower_latin
import org.sjhstudio.diary.feature.settings.resources.font_name_gowun_batang
import org.sjhstudio.diary.feature.settings.resources.font_name_gowun_batang_ko
import org.sjhstudio.diary.feature.settings.resources.font_name_gowun_batang_latin
import org.sjhstudio.diary.feature.settings.resources.font_name_gowun_dodum
import org.sjhstudio.diary.feature.settings.resources.font_name_gowun_dodum_ko
import org.sjhstudio.diary.feature.settings.resources.font_name_gowun_dodum_latin
import org.sjhstudio.diary.feature.settings.resources.font_name_hi_melody
import org.sjhstudio.diary.feature.settings.resources.font_name_hi_melody_ko
import org.sjhstudio.diary.feature.settings.resources.font_name_hi_melody_latin
import org.sjhstudio.diary.feature.settings.resources.font_name_line_seed
import org.sjhstudio.diary.feature.settings.resources.font_name_maru_buri
import org.sjhstudio.diary.feature.settings.resources.font_name_nanum_cheer
import org.sjhstudio.diary.feature.settings.resources.font_name_nanum_jeomggol
import org.sjhstudio.diary.feature.settings.resources.font_name_nanum_jeomggol_ko
import org.sjhstudio.diary.feature.settings.resources.font_name_nanum_jeomggol_latin
import org.sjhstudio.diary.feature.settings.resources.font_name_nanum_pen
import org.sjhstudio.diary.feature.settings.resources.font_name_pretendard
import org.sjhstudio.diary.feature.settings.resources.font_name_system

/**
 * 글꼴 목록 순서(Figma "글꼴 (G1)").
 * 시스템 글꼴이 맨 위다.
 */
internal val FONT_ORDER: List<DiaryFont> = listOf(DiaryFont.SYSTEM) + DiaryFont.entries.filter { it != DiaryFont.SYSTEM }

/**
 * 받는 글꼴의 크기(공식 배포 파일, `docs/design.md` 일기 본문 글꼴 10종).
 * 받기 버튼에 보인다.
 */
internal val FONT_DOWNLOAD_MB: Map<DiaryFont, Double> =
    mapOf(
        DiaryFont.GOWUN_DODUM to 7.2,
        DiaryFont.GOWUN_BATANG to 8.4,
        DiaryFont.NANUM_JEOMGGOL to 6.7,
        DiaryFont.GAMJA_FLOWER to 12.6,
        DiaryFont.HI_MELODY to 12.6
    )

internal fun DiaryFont.label(): StringResource = when (this) {
    DiaryFont.SYSTEM -> Res.string.font_name_system
    DiaryFont.PRETENDARD -> Res.string.font_name_pretendard
    DiaryFont.GOWUN_DODUM -> Res.string.font_name_gowun_dodum
    DiaryFont.LINE_SEED -> Res.string.font_name_line_seed
    DiaryFont.MARU_BURI -> Res.string.font_name_maru_buri
    DiaryFont.GOWUN_BATANG -> Res.string.font_name_gowun_batang
    DiaryFont.NANUM_PEN -> Res.string.font_name_nanum_pen
    DiaryFont.NANUM_JEOMGGOL -> Res.string.font_name_nanum_jeomggol
    DiaryFont.NANUM_CHEER -> Res.string.font_name_nanum_cheer
    DiaryFont.GAMJA_FLOWER -> Res.string.font_name_gamja_flower
    DiaryFont.HI_MELODY -> Res.string.font_name_hi_melody
}

/**
 * 받기 전 글꼴의 이름 그림(그 글꼴로 그린 이름, 투명 바탕).
 * 한국어 화면은 한국어 이름, 그 밖은 로마자 이름이다.
 * 이 글꼴들에는 가나가 없어 일본어 화면도 로마자 이름을 쓴다.
 */
internal fun DiaryFont.namePreview(korean: Boolean): DrawableResource? = when (this) {
    DiaryFont.GOWUN_DODUM -> if (korean) Res.drawable.font_name_gowun_dodum_ko else Res.drawable.font_name_gowun_dodum_latin
    DiaryFont.GOWUN_BATANG -> if (korean) Res.drawable.font_name_gowun_batang_ko else Res.drawable.font_name_gowun_batang_latin
    DiaryFont.NANUM_JEOMGGOL -> if (korean) Res.drawable.font_name_nanum_jeomggol_ko else Res.drawable.font_name_nanum_jeomggol_latin
    DiaryFont.GAMJA_FLOWER -> if (korean) Res.drawable.font_name_gamja_flower_ko else Res.drawable.font_name_gamja_flower_latin
    DiaryFont.HI_MELODY -> if (korean) Res.drawable.font_name_hi_melody_ko else Res.drawable.font_name_hi_melody_latin
    else -> null
}
