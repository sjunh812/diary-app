package org.sjhstudio.diary.core.model

import kotlin.time.Instant

/** 일기에 붙은 사진 한 장. 같은 파일(같은 해시)을 여러 번 가리키면 파일은 하나, 줄은 여럿 */
data class Media(
    val id: String,
    val entryId: String,
    /** 0부터. 한 일기 안에서 겹치지 않음 */
    val sortOrder: Int,
    val status: MediaStatus,
    /** `<sha256>.<ext>`. 절대경로는 담지 않음. [MediaStatus.MISSING]이면 null */
    val fileName: String?,
    /** 소문자 hex. [MediaStatus.MISSING]이면 null */
    val sha256: String?,
    /** EXIF 방향을 반영한 픽셀 크기 */
    val width: Int?,
    val height: Int?,
    val byteSize: Long?,
    /** 이전된 사진은 찾았든 못 찾았든 원래 경로. 새로 붙인 사진은 null */
    val legacyPath: String? = null,
    val createdAt: Instant
)

enum class MediaStatus(val id: String) {
    NORMAL("normal"),

    /** 이전 때 원본을 찾지 못한 사진. 줄은 남겨 "원래 기기에 있는 사진"으로 보임 */
    MISSING("missing"),

    /** 내용과 이름은 알고 파일은 아직 기기에 없음. 복원 중이거나 백업에 못 올라간 사진(스펙 003) */
    PENDING("pending")
    ;

    companion object {
        fun fromId(id: String): MediaStatus? = entries.firstOrNull { it.id == id }
    }
}
