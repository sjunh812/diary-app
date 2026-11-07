package org.sjhstudio.diary.core.model

import kotlinx.datetime.LocalDate

/** 양 끝을 포함하는 날짜 범위. */
data class DateRange(val start: LocalDate, val end: LocalDate)

/**
 * 찾기 조건(FR-025, FR-026).
 * 통계에서 넘어오면 기분과 기간이 채워져 있다(FR-031).
 */
data class SearchFilter(
    val query: String = "",
    val moods: Set<Mood> = emptySet(),
    val weathers: Set<Weather> = emptySet(),
    val hasPhoto: Boolean = false,
    val favoritesOnly: Boolean = false,
    val period: DateRange? = null
) {
    /** 글자도 조건도 없으면 찾지 않고 처음 화면을 보인다. */
    val isEmpty: Boolean
        get() = query.isBlank() && moods.isEmpty() && weathers.isEmpty() && !hasPhoto && !favoritesOnly && period == null

    override fun toString(): String = "SearchFilter"
}
