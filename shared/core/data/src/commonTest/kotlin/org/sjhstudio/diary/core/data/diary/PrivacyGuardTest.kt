package org.sjhstudio.diary.core.data.diary

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.sjhstudio.diary.core.model.Draft
import org.sjhstudio.diary.core.model.DraftKey
import org.sjhstudio.diary.core.model.EntryDetail
import org.sjhstudio.diary.core.model.EntryRow
import org.sjhstudio.diary.core.model.RichText
import org.sjhstudio.diary.core.model.SearchFilter
import org.sjhstudio.diary.core.model.TrashedEntry
import org.sjhstudio.diary.core.platform.AnalyticsEvents

// 일기 내용이 로그나 비정상 종료 보고에 섞이지 않는지 본다(헌법 II, 스펙 002 T019).
// 문자열로 바뀌는 값에 내용이 없으면 실수로 찍어도 새지 않는다.
class PrivacyGuardTest {
    private val secret = "비밀스러운 글"

    private val row =
        EntryRow("e1", LocalDate(2026, 11, 1), LocalTime(9, 0), secret, null, null, secret, false, listOf(secret))

    @Test
    fun values_with_diary_content_hide_it_in_to_string() {
        val values =
            listOf<Any>(
                row,
                EntryDetail(entry("e1", text = secret, placeName = secret), emptyList()),
                TrashedEntry(row, T0, 3),
                newDraft("<p>$secret</p>").copy(placeName = secret),
                DraftKey.Edit(secret),
                SearchFilter(query = secret),
                RichText(secret, emptyList())
            )
        for (value in values) assertFalse(secret in value.toString(), "${value::class.simpleName}에 내용이 보인다")
    }

    @Test
    fun screen_names_carry_no_content() {
        // 화면 이름은 고정 표에 있는 값만 보낸다.
        // 표에 공백이나 한글이 있으면 자유 글이 섞인 것이다.
        for (name in AnalyticsEvents.SCREENS) {
            assertFalse(name.any { it == ' ' || it in '가'..'힣' }, name)
        }
    }

    @Test
    fun draft_type_stays_a_data_class_for_saving_but_hides_text() {
        val draft: Draft = newDraft("<p>$secret</p>")
        assertFalse(secret in "$draft")
    }
}
