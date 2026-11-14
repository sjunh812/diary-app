package org.sjhstudio.diary.core.model

/**
 * 앱 글꼴 10종과 시스템 글꼴. 고른 글꼴은 일기 본문과 앱 전체에 씀(헌법 V)
 * 레거시 `font_key` 대응은 contracts/legacy-mapping.md.
 */
enum class DiaryFont(val id: String) {
    PRETENDARD("pretendard"),
    SYSTEM("system"),
    HAKGYOANSIM_BADASSEUGI("hakgyoansim_badasseugi"),
    CHANGWON_ROUND("changwon_round"),
    KIMJUNGCHUL_MYUNGJO("kimjungchul_myungjo"),
    GOWUN_BATANG("gowun_batang"),
    KYOBO_2020("kyobo_2020"),
    NANUM_JEOMGGOL("nanum_jeomggol"),
    NANUM_CHEER("nanum_cheer"),
    RIX_WATERMELON("rix_watermelon"),
    SEJONG_GEULGGOT("sejong_geulggot")
    ;

    companion object {
        val DEFAULT = PRETENDARD

        fun fromId(id: String?): DiaryFont? = entries.firstOrNull { it.id == (RENAMED[id] ?: id) }

        /** 시험판에서 앞서 고른 글꼴은 자리를 이은 글꼴로 이음 */
        private val RENAMED =
            listOf("cafe24_surround", "line_seed", "binggrae", "hyemin").associateWith { "changwon_round" } +
                mapOf(
                    "gowun_dodum" to "hakgyoansim_badasseugi",
                    "gmarket_sans" to "hakgyoansim_badasseugi",
                    "maru_buri" to "kimjungchul_myungjo",
                    "nanum_pen" to "kyobo_2020",
                    "kcc_jeongbeom" to "kyobo_2020",
                    "gamja_flower" to "rix_watermelon",
                    "hi_melody" to "sejong_geulggot"
                )
    }
}
