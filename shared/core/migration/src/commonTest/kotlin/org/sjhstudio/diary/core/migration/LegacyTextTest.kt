package org.sjhstudio.diary.core.migration

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import org.sjhstudio.diary.core.model.richtext.toPlainText

// contracts/legacy-mapping.md "본문 변환"의 고정 값과 왕복 규칙(스펙 001 T032).
class LegacyTextTest {
    private val fixed =
        listOf(
            "" to "",
            "a" to "<p>a</p>",
            "a\n" to "<p>a<br></p>",
            "\n" to "<p><br></p>",
            "\n\n" to "<p></p><p></p>",
            "a\n\nb" to "<p>a</p><p>b</p>",
            "a\n\n\nb" to "<p>a</p><p><br>b</p>",
            "a\n\n\n\nb" to "<p>a</p><p></p><p>b</p>",
            "<b>&amp;</b>" to "<p>&lt;b&gt;&amp;amp;&lt;/b&gt;</p>",
            "  두 칸\t탭\r\n" to "<p>  두 칸\t탭\r<br></p>",
            "😀 &nbsp;" to "<p>😀 &amp;nbsp;</p>"
        )

    @Test
    fun fixed_values_match_the_contract() {
        for ((contents, html) in fixed) {
            assertEquals(html, legacyToHtml(contents), "원문 ${contents.encodeForMessage()}")
            assertEquals(contents, toPlainText(html), "되돌리기 ${contents.encodeForMessage()}")
        }
    }

    @Test
    fun hundred_thousand_characters_round_trip() {
        val text = buildString { repeat(12_500) { append("오늘은 맑음.\n") } }
        assertEquals(100_000, text.length)
        assertEquals(text, toPlainText(legacyToHtml(text)))
    }

    @Test
    fun surrogate_pairs_next_to_paragraph_breaks_round_trip() {
        val text = "😀\n\n😀\n\n\n🎉끝😀"
        assertEquals(text, toPlainText(legacyToHtml(text)))
    }

    @Test
    fun random_text_round_trips() {
        val alphabet = listOf("a", "가", " ", "\t", "\r", "\n", "\n\n", "&", "<", ">", "\"", "'", ";", "#", "😀", "&amp;", "&#39;", "<p>")
        val random = Random(20261101)
        repeat(2_000) {
            val text = buildString { repeat(random.nextInt(0, 40)) { append(alphabet.random(random)) } }
            assertEquals(text, toPlainText(legacyToHtml(text)), text.encodeForMessage())
        }
    }

    private fun String.encodeForMessage() = replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t")
}
