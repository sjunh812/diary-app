package org.sjhstudio.diary.core.data.diary

import androidx.paging.PagingSource
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.sjhstudio.diary.core.database.EntryListRow
import org.sjhstudio.diary.core.model.PreviewLength
import org.sjhstudio.diary.core.model.PreviewMode
import org.sjhstudio.diary.core.model.RichSpan
import org.sjhstudio.diary.core.model.RichStyle
import org.sjhstudio.diary.core.model.richtext.PreviewText

// 목록 미리보기 방식(스펙 008 US1·US2·US5)
class EntryPreviewModeTest {
    private val db = memoryDatabase()

    @AfterTest
    fun tearDown() {
        db.close()
    }

    private suspend fun listRow(id: String): EntryListRow =
        assertIs<PagingSource.LoadResult.Page<Int, EntryListRow>>(db.entryDao().visibleEntries().loadOnce(10)).data.single { it.id == id }

    @Test
    fun three_lines_keep_line_breaks_and_drop_blank_lines() = runTest {
        db.seed(entry("a", text = "09:00 병원\n\n11:00 은행\n14:00 회의\n16:30 택배"))

        val row = listRow("a").toRow(emptyList(), MEDIA_DIR)

        assertEquals("09:00 병원\n11:00 은행\n14:00 회의\n16:30 택배", row.preview)
        assertEquals(PreviewText.THREE_LINES, row.previewLines)
        assertTrue(row.previewSpans.isEmpty())
    }

    @Test
    fun search_keeps_the_found_word_in_one_line() = runTest {
        db.seed(entry("a", text = "첫 줄\n둘째 줄\n은행 들르기"))

        val row = listRow("a").toRow(emptyList(), MEDIA_DIR, query = "은행")

        assertEquals("첫 줄 둘째 줄 은행 들르기", row.preview)
    }

    @Test
    fun search_preview_does_not_start_inside_an_emoji() {
        val shown = preview("a".repeat(100) + "😀" + "b".repeat(11) + "needle", "needle")
        assertTrue(shown.startsWith("…😀"), shown)
    }

    @Test
    fun full_reads_more_text_than_the_list_query() = runTest {
        val long = (1..60).joinToString("\n") { "줄 $it 은 꽤 길게 쓴 일정입니다" }
        db.seed(entry("a", text = long))
        val listed = listRow("a")
        val more = db.entryDao().previewTextsOf(listOf("a")) { previewTexts(it, PreviewText.UNLIMITED) }

        val row = listed.toRow(emptyList(), MEDIA_DIR, mode = PreviewMode(PreviewLength.FULL), text = more["a"])

        assertTrue(listed.plainText.length < more.getValue("a").length)
        assertEquals(PreviewText.UNLIMITED, row.previewLines)
        // 전체는 끝까지 읽고 줄임표 없이 보임
        assertEquals(long.length, row.preview.length)
        assertTrue(!row.previewCut)
    }

    @Test
    fun formatting_reads_html_and_keeps_spans() = runTest {
        db.seed(entry("a", text = "x"))
        val stored = db.entryDao().get("a")!!
        db.entryDao().update(stored.copy(bodyHtml = "<p><b>굵게</b><br>보통</p>", plainText = "굵게\n보통"))
        val html = db.entryDao().previewTextsOf(listOf("a")) { previewHtmls(it, THREE_LINES_HTML_CHARS) }

        val row = listRow("a").toRow(emptyList(), MEDIA_DIR, mode = PreviewMode(formatting = true), html = html["a"])

        assertEquals("굵게\n보통", row.preview)
        assertEquals(listOf(RichSpan(0, 2, RichStyle.Bold)), row.previewSpans)
    }

    @Test
    fun cut_tag_at_the_end_is_dropped() {
        assertEquals("<p>글", "<p>글<sp".withoutCutTag())
        assertEquals("<p>글</p>", "<p>글</p>".withoutCutTag())
        assertEquals("<p>글", "<p>글&nb".withoutCutTag())
        assertEquals("<p>글&amp;", "<p>글&amp;".withoutCutTag())
    }
}
