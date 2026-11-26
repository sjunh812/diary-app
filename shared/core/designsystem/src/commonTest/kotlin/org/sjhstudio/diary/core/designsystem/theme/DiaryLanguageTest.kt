package org.sjhstudio.diary.core.designsystem.theme

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiaryLanguageTest {
    // Pretendard에 한자가 없어 일본어·중국어 화면만 시스템 글꼴
    @Test
    fun onlyJapaneseAndChineseUseSystemFont() {
        assertTrue(usesSystemFont("ja"))
        assertTrue(usesSystemFont("zh-Hant"))
        listOf("en", "ko", "es", "pt", "de", "fr").forEach { assertFalse(usesSystemFont(it), it) }
    }

    // 한중일은 연·월, 오전·오후가 앞
    @Test
    fun eastAsianLanguagesPutLargerUnitFirst() {
        listOf("ko", "ja", "zh-Hant").forEach { assertTrue(putsLargerUnitFirst(it), it) }
        listOf("en", "es", "pt", "de", "fr").forEach { assertFalse(putsLargerUnitFirst(it), it) }
    }
}
