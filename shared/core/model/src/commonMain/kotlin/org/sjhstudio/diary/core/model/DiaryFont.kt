package org.sjhstudio.diary.core.model

/**
 * 앱 글꼴 10종과 시스템 글꼴. 고른 글꼴은 일기 본문과 앱 전체에 씀(헌법 V)
 * 레거시 `font_key` 대응은 contracts/legacy-mapping.md.
 */
enum class DiaryFont(val id: String) {
    PRETENDARD("pretendard"),
    SYSTEM("system"),
    GOWUN_DODUM("gowun_dodum"),
    CHANGWON_ROUND("changwon_round"),
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

        /** 시험판에서 앞서 고른 글꼴은 이 글꼴로 이음 */
        private val RENAMED = listOf("cafe24_surround", "line_seed", "binggrae", "hyemin").associateWith { "changwon_round" }
    }
}
