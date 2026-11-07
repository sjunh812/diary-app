package org.sjhstudio.diary.core.model

/**
 * 일기 본문 글꼴 10종과 시스템 글꼴.
 * 앱의 틀은 늘 Pretendard이고, 이 글꼴은 일기 본문에만 쓴다(헌법 V).
 * 레거시 `font_key` 대응은 contracts/legacy-mapping.md에 있다.
 */
enum class DiaryFont(val id: String) {
    PRETENDARD("pretendard"),
    SYSTEM("system"),
    GOWUN_DODUM("gowun_dodum"),
    LINE_SEED("line_seed"),
    MARU_BURI("maru_buri"),
    GOWUN_BATANG("gowun_batang"),
    NANUM_PEN("nanum_pen"),
    NANUM_JEOMGGOL("nanum_jeomggol"),
    NANUM_CHEER("nanum_cheer"),
    GAMJA_FLOWER("gamja_flower"),
    HI_MELODY("hi_melody")
    ;

    companion object {
        val DEFAULT = PRETENDARD

        fun fromId(id: String?): DiaryFont? = entries.firstOrNull { it.id == (RENAMED[id] ?: id) }

        /** 시험판에서 카페24 써라운드를 고른 사람은 이 글꼴로 잇는다(2026-11-07). */
        private val RENAMED = mapOf("cafe24_surround" to "line_seed")
    }
}
