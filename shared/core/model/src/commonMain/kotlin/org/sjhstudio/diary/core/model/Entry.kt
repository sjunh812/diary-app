package org.sjhstudio.diary.core.model

import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/**
 * 일기 한 편.
 * 뜻과 화면 규칙은 스펙 002 data-model.md, 저장 형식은 스펙 001 data-model.md `entry` 표를 따른다.
 */
data class Entry(
    val id: String,
    /** 사용자가 고른 일기 날짜. 시간대가 바뀌어도 그대로다. */
    val date: LocalDate,
    /** 시와 분. 초는 쓰지 않는다. */
    val time: LocalTime,
    /** 제한된 HTML(스펙 002 contracts/rich-text-html.md). 빈 글은 "". */
    val bodyHtml: String,
    /** 서식 없는 글. 레거시에서 옮긴 일기는 원문 그대로다. */
    val plainText: String,
    val mood: Mood?,
    val weather: Weather?,
    /** 빈 문자열은 null로 둔다. */
    val placeName: String?,
    val isFavorite: Boolean,
    val createdAt: Instant,
    /** 일기에 보이는 값이 바뀔 때마다 바꾼다(스펙 003 합치기가 쓴다). */
    val updatedAt: Instant,
    /** null이면 보이는 일기, 값이 있으면 휴지통(스펙 002). */
    val deletedAt: Instant? = null,
    /** 레거시 `_id`. 새로 쓴 일기는 null. */
    val legacyId: Int? = null,
    /** 레거시 `create_date` 원문. */
    val legacyCreateDate: String? = null,
    /** 레거시 `location_x|location_y` 원문. 둘 다 비어 있으면 null. */
    val legacyLocation: String? = null
)
