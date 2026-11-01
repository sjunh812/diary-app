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
    CAFE24_SURROUND("cafe24_surround"),
    MARU_BURI("maru_buri"),
    GOWUN_BATANG("gowun_batang"),
    NANUM_PEN("nanum_pen"),
    NANUM_JEOMGGOL("nanum_jeomggol"),
    KKOMI("kkomi"),
    GAMJA_FLOWER("gamja_flower"),
    HI_MELODY("hi_melody")
    ;

    companion object {
        val DEFAULT = PRETENDARD

        fun fromId(id: String?): DiaryFont? = entries.firstOrNull { it.id == id }
    }
}
