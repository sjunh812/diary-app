package org.sjhstudio.diary.core.designsystem.component

import kotlin.test.Test
import kotlin.test.assertEquals

class KeepWordsTest {
    private fun breakable(text: String): List<String> = keepWords(text).split(' ').map { it.replace("⁠", "") }

    @Test
    fun breaksOnlyAtSpaces() {
        assertEquals(listOf("이", "휴대폰에만", "있어요"), breakable("이 휴대폰에만 있어요"))
        assertEquals("휴⁠대⁠폰", keepWords("휴대폰"))
    }

    // "사진 5,002 / 장이"처럼 숫자와 단위가 끊겼음
    @Test
    fun keepsNumberWithItsUnit() {
        assertEquals("5,002⁠장⁠이", keepWords("5,002장이"))
        assertEquals("요⁠.", keepWords("요."))
    }

    // "사진 / 5,003장이"처럼 낱말과 숫자 사이에서 끊겼음
    @Test
    fun keepsWordWithTheCountAfterIt() {
        assertEquals(listOf("일기\u00A010,004편과", "사진\u00A05,003장이"), breakable("일기 10,004편과 사진 5,003장이"))
    }

    @Test
    fun leavesTextWithoutHangulAlone() {
        assertEquals("1,204 photos", keepWords("1,204 photos"))
    }
}
