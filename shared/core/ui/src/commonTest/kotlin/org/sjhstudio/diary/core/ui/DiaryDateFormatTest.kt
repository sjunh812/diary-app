package org.sjhstudio.diary.core.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class DiaryDateFormatTest {
    @Test
    fun deviceRegionIsKeptWhenLanguageMatches() {
        assertEquals("en-GB", formatLocaleTag("en", "en-GB"))
        assertEquals("ko-KR", formatLocaleTag("ko", "ko_KR"))
        assertEquals("ja-JP", formatLocaleTag("ja", "ja_JP@calendar=japanese"))
    }

    @Test
    fun screenLanguageWinsWhenDeviceLanguageIsNotSupported() {
        // 프랑스어 기기는 영어 화면이 보이고 날짜도 영어
        assertEquals("en", formatLocaleTag("en", "fr-FR"))
        assertEquals("en", formatLocaleTag("en", "zh-Hant-TW"))
    }

    // 문장 안에서 "11월 / 12일"처럼 끊기지 않게 함
    @Test
    fun dateKeepsItsPartsOnOneLine() {
        assertEquals("11월 12일 오후 7:48", "11월 12일 오후 7:48".keepTogether())
    }
}
