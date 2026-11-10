package org.sjhstudio.diary.core.model

enum class ThemeMode(val id: String) {
    /** 기기 설정을 따름 */
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark")
    ;

    companion object {
        val DEFAULT = SYSTEM

        fun fromId(id: String?): ThemeMode? = entries.firstOrNull { it.id == id }
    }
}
