package org.sjhstudio.diary.core.model

/** 목록 보기(FR-018). 저장 값은 [id] */
enum class ListStyle(val id: String) {
    FEED("feed"),
    CARD("card"),
    PHOTO_GRID("photo_grid")
    ;

    companion object {
        fun fromId(id: String?): ListStyle = idOrNull(id) ?: FEED

        /** 모르는 값이면 null. 복원처럼 모르는 값을 건너뛸 때 씀 */
        fun idOrNull(id: String?): ListStyle? = entries.firstOrNull { it.id == id }
    }
}

/** 목록 미리보기 길이(스펙 008). 저장 값은 [id] */
enum class PreviewLength(val id: String) {
    THREE_LINES("three"),
    FIVE_LINES("five"),
    FULL("full")
    ;

    companion object {
        fun fromId(id: String?): PreviewLength = idOrNull(id) ?: THREE_LINES

        /** 모르는 값이면 null. 복원처럼 모르는 값을 건너뛸 때 씀 */
        fun idOrNull(id: String?): PreviewLength? = entries.firstOrNull { it.id == id }
    }
}

/**
 * 홈 목록 설정
 * [showFormatting]은 저장 값. 실제로 서식을 그리는지는 요금제와 함께 봄([PreviewMode.of])
 * [previewChosen]은 이 기기에서 길이를 직접 고른 적이 있음. 옛 설정을 다시 잇지 않으려고 둠
 * [legacyChecked]는 옛 1.7.4 "일기 3줄 보기"를 한 번 판단했음. 기기 사정이라 백업하지 않음
 * [favoritesOnly]는 더 쓰지 않음. 저장과 백업 형식을 그대로 두려고 칸만 남김
 */
data class ListPreferences(
    val listStyle: ListStyle = ListStyle.FEED,
    val favoritesOnly: Boolean = false,
    val previewLength: PreviewLength = PreviewLength.THREE_LINES,
    val showFormatting: Boolean = false,
    val previewChosen: Boolean = false,
    val legacyChecked: Boolean = false
)

/** 목록 미리보기를 만드는 방식. 설정과 요금제로 정함 */
data class PreviewMode(val length: PreviewLength = PreviewLength.THREE_LINES, val formatting: Boolean = false) {
    companion object {
        val DEFAULT = PreviewMode()

        /** 사진 격자는 칸에 글 몇 줄만 보여 길이·서식과 상관없음. 쓰지 않을 글을 더 읽지 않게 기본 방식 */
        fun of(list: ListPreferences, plan: Plan?): PreviewMode = if (list.listStyle == ListStyle.PHOTO_GRID) {
            DEFAULT
        } else {
            PreviewMode(list.previewLength, formatting = list.showFormatting && plan?.listFormatting == true)
        }
    }
}
