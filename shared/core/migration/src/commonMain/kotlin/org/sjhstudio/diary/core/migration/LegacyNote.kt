package org.sjhstudio.diary.core.migration

/**
 * 레거시 `note.db`의 `Note` 표 한 줄을 읽은 그대로 담음
 * 값은 고치지 않음
 * 고치는 일은 [LegacyEntryMapper] 몫
 * `star`는 버전 1에서 2로 올린 DB의 옛 줄에서 null.
 */
data class LegacyNote(
    val id: Long,
    val createDate: String?,
    val modifyDate: String?,
    val contents: String?,
    val mood: Long?,
    val weather: Long?,
    val address: String?,
    val star: Long?,
    val locationX: String?,
    val locationY: String?,
    val picture: String?
)
