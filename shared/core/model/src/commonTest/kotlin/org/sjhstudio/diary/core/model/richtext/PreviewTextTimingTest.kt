package org.sjhstudio.diary.core.model.richtext

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource
import org.sjhstudio.diary.core.model.PreviewLength
import org.sjhstudio.diary.core.model.parseRichText

/**
 * 쪽 하나(60편)를 서식 보기로 만드는 시간(스펙 008 research R4)
 * 이 일은 IO 스레드에서 하지만, 쪽을 늦게 주면 스크롤 끝에서 빈 줄이 보임
 * 기기 차이가 커서 넉넉한 상한만 두고 값은 출력해 둠
 */
class PreviewTextTimingTest {
    private fun html(lines: Int) = (0 until lines).chunked(3).joinToString("") { block ->
        block.joinToString("<br>", "<p>", "</p>") { i ->
            if (i % 7 == 0) "<b>퇴근길에 한강까지 걸었다 $i</b>" else "<span style=\"background-color:#fef08a\">바람이 차가워졌다 $i</span> 그래서 좋았다"
        }
    }

    @Test
    fun one_page_of_formatted_previews_is_fast() {
        // 쪽 60편: 보통 일기 57편, 서식 HTML 앞부분을 다 채우는 긴 일기 3편
        val page = List(57) { html(6) } + List(3) { html(400).take(12000) }
        repeat(3) { page.forEach { PreviewText.shape(parseRichText(it), PreviewLength.FULL) } }
        val mark = TimeSource.Monotonic.markNow()
        page.forEach { PreviewText.shape(parseRichText(it), PreviewLength.FULL) }
        val took = mark.elapsedNow()
        println("PreviewTextTiming: page of 60 took $took")
        assertTrue(took < 500.milliseconds, "쪽 하나에 $took")
    }
}
