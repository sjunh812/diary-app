package org.sjhstudio.diary.feature.settings

import kotlin.test.Test
import kotlin.test.assertEquals

/** 메일 주소 뒤의 글자는 어느 메일 앱에서나 깨지지 않게 낱낱이 바꿈. */
class FeedbackLinkTest {
    @Test
    fun encodesKoreanAndEmojiByteByByte() {
        assertEquals("%ED%94%BC%EB%93%9C%EB%B0%B1", "피드백".percentEncoded())
        assertEquals("%F0%9F%98%8A", "😊".percentEncoded())
    }

    @Test
    fun encodesLineBreaksSpacesAndSymbols() {
        assertEquals("OS%3A%20Android%2016%0A", "OS: Android 16\n".percentEncoded())
    }
}
