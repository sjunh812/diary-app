package org.sjhstudio.diary.core.model

/** 목록 모양(FR-018). 저장 값은 [id]다. */
enum class ListStyle(val id: String) {
    FEED("feed"),
    CARD("card"),
    PHOTO_GRID("photo_grid")
    ;

    companion object {
        fun fromId(id: String?): ListStyle = entries.firstOrNull { it.id == id } ?: FEED
    }
}

/**
 * 홈 목록 설정. 미리보기는 늘 3줄이라 줄 수 설정은 없다.
 * [favoritesOnly]는 더 쓰지 않는다. 저장과 백업 형식을 그대로 두려고 칸만 남겼다.
 */
data class ListPreferences(val listStyle: ListStyle = ListStyle.FEED, val favoritesOnly: Boolean = false)
