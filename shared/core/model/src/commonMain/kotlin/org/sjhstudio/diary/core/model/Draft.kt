package org.sjhstudio.diary.core.model

import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/**
 * 임시 저장본 열쇠. 수정하는 일기는 일기마다 하나(FR-009)
 * 새 일기는 보통 하나지만, 쓰던 글을 두고 [새로 쓰기]를 고르면 버리기 전까지 함께 남아야 해 여럿일 수 있음(FR-010)
 */
sealed interface DraftKey {
    val value: String

    data class New(override val value: String = PREFIX) : DraftKey {
        override fun toString(): String = "New"

        companion object {
            const val PREFIX = "new"
        }
    }

    data class Edit(val entryId: String) : DraftKey {
        override val value: String = entryId

        override fun toString(): String = "Edit"
    }
}

/** 임시 저장본 사진. 파일은 고르는 즉시 앱 저장소에 있음(research R6) */
data class DraftPhoto(val fileName: String, val sha256: String, val width: Int?, val height: Int?, val byteSize: Long?)

/** 쓰는 중인 일기(data-model.md Draft). 일기 내용이 있어 [toString]에 담지 않음(헌법 II) */
data class Draft(
    val key: DraftKey,
    val date: LocalDate,
    val time: LocalTime,
    val bodyHtml: String,
    val mood: Mood?,
    val weather: Weather?,
    val placeName: String?,
    val isFavorite: Boolean,
    /** 자동으로 채운 날씨인지. 날짜를 바꾸면 이것만 지움(FR-005) */
    val weatherAutoFilled: Boolean,
    val updatedAt: Instant,
    val photos: List<DraftPhoto> = emptyList()
) {
    override fun toString(): String = "Draft"
}

/** 사진 고르기 화면이 넘긴 그림 하나. 바이트는 쓸 때 읽어 메모리에 오래 두지 않음 */
class PickedImage(val read: suspend () -> ByteArray)

/** 사진 추가 결과. 한도나 공간 때문에 못 추가해도 저장은 막지 않음(FR-006) */
sealed interface AddPhotosResult {
    /** [added]장을 추가함. [failed]는 읽지 못한 그림 수 */
    data class Added(val added: Int, val failed: Int = 0) : AddPhotosResult

    /** 한도에 닿아 [added]장만 추가함 */
    data class LimitReached(val limit: Int, val canUpgrade: Boolean, val added: Int) : AddPhotosResult

    /** 기기 공간이 부족해 [added]장만 추가함 */
    data class NoSpace(val added: Int) : AddPhotosResult
}

/** 한 편에 새로 붙일 수 있는 사진 수(헌법 III 예외). 이미 넘은 일기는 그대로 둠 */
object PhotoLimit {
    const val FREE = 10
    const val PLUS = 30
}
