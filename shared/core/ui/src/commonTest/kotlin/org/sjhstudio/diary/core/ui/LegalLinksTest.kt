package org.sjhstudio.diary.core.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// 한국어는 노션 원문, 그 밖의 언어는 영어 번역본
class LegalLinksTest {
    @Test
    fun korean_opens_the_korean_original() {
        assertTrue(LegalLinks.privacy("ko").startsWith("https://sjunh812.notion.site/"))
        assertTrue(LegalLinks.terms("ko").startsWith("https://sjunh812.notion.site/"))
    }

    @Test
    fun other_languages_open_the_english_translation() {
        listOf("en", "ja", "zh-Hant", "es", "pt", "de", "fr").forEach { language ->
            assertEquals("https://sjunh812.pages.dev/en/1day1diary/privacy/", LegalLinks.privacy(language))
            assertEquals("https://sjunh812.pages.dev/en/1day1diary/terms/", LegalLinks.terms(language))
        }
    }
}
