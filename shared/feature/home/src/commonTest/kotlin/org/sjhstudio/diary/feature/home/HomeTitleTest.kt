package org.sjhstudio.diary.feature.home

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate

// 홈 큰 제목의 날짜(스펙 002 FR-015)
class HomeTitleTest {
    private val today = LocalDate(2026, 11, 7)
    private val seen = LocalDate(2024, 3, 2)
    private val held = LocalDate(2025, 8, 15)

    @Test
    fun the_top_row_sets_the_title() {
        assertEquals(seen, shownDate(seen, held, hasRows = true, today))
    }

    /** 먼 달로 건너뛰는 동안 맨 위가 자리 표시 줄이어도 오늘로 튀지 않음 */
    @Test
    fun a_placeholder_keeps_the_previous_title() {
        assertEquals(held, shownDate(null, held, hasRows = true, today))
    }

    @Test
    fun an_empty_list_shows_today() {
        assertEquals(today, shownDate(null, held, hasRows = false, today))
    }

    @Test
    fun nothing_seen_yet_shows_today() {
        assertEquals(today, shownDate(null, null, hasRows = true, today))
    }
}
