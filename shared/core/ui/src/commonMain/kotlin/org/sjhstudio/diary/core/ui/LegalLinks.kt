package org.sjhstudio.diary.core.ui

/**
 * 공개된 약관과 개인정보처리방침. 스토어에도 같은 주소를 씀(`docs/release.md`)
 * 한국어는 원문(노션), 그 밖의 언어는 사이트의 영어 번역본
 */
object LegalLinks {
    private const val TERMS_KO = "https://sjunh812.notion.site/411ec02c164a80d3b1f5f1b5bbe1c7c4"
    private const val PRIVACY_KO = "https://sjunh812.notion.site/411ec02c164a805b9bfec5bb88d766a9"
    private const val TERMS_EN = "https://sjunh812.pages.dev/en/1day1diary/terms/"
    private const val PRIVACY_EN = "https://sjunh812.pages.dev/en/1day1diary/privacy/"

    /** [language]는 앱 화면 언어(`diaryLanguage()`) */
    fun terms(language: String): String = if (language == "ko") TERMS_KO else TERMS_EN

    fun privacy(language: String): String = if (language == "ko") PRIVACY_KO else PRIVACY_EN
}
