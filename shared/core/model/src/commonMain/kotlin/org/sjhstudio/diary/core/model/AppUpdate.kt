package org.sjhstudio.diary.core.model

/** 버전("2.0.1", 버전 코드 "45")을 점으로 나눈 숫자 칸끼리 비교. 숫자가 아닌 칸은 0 */
object AppVersion {
    fun compare(a: String, b: String): Int {
        val left = parts(a)
        val right = parts(b)
        for (i in 0 until maxOf(left.size, right.size)) {
            val diff = left.getOrElse(i) { 0 }.compareTo(right.getOrElse(i) { 0 })
            if (diff != 0) return diff
        }
        return 0
    }

    fun isNewer(candidate: String, than: String): Boolean = compare(candidate, than) > 0

    private fun parts(version: String) = version.trim().split('.').map { part -> part.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
}

/**
 * 스토어가 알려 준 새 버전 상태
 * id는 Android 버전 코드, iOS 버전 이름
 */
sealed interface StoreCheck {
    data object None : StoreCheck

    data class Available(val id: String) : StoreCheck

    /** 앱을 다시 열었을 때 이미 받는 중(Android). 취소·실패하면 [Available]로 내림 */
    data class Downloading(val id: String, val progress: Float?) : StoreCheck

    /** 받기는 끝났고 다시 시작하면 바뀜(Android) */
    data class Downloaded(val id: String) : StoreCheck
}

/** 받기 진행(Android). iOS는 App Store가 받아 앱이 모름 */
sealed interface InstallStep {
    data class Downloading(val progress: Float?) : InstallStep

    data object Downloaded : InstallStep

    data object Failed : InstallStep

    /** 사용자가 Play 확인 창이나 받기를 취소 */
    data object Canceled : InstallStep
}

/** 원격 업데이트 요약. 언어 코드("ko")별 문장 */
data class ReleaseNote(val texts: Map<String, String>) {
    /** 앱 언어 문장, 없으면 영어(`docs/design.md` 여러 언어) */
    fun textFor(language: String): String? = texts[language] ?: texts[FALLBACK_LANGUAGE]

    private companion object {
        const val FALLBACK_LANGUAGE = "en"
    }
}

/** 화면이 보는 새 버전 상태(스펙 009) */
sealed interface UpdateNotice {
    data object None : UpdateNotice

    /** [failed]면 받다 실패해 설명이 실패 문구 */
    data class Available(val required: Boolean, val note: ReleaseNote?, val failed: Boolean = false) : UpdateNotice

    data class Downloading(val progress: Float?) : UpdateNotice

    data object Downloaded : UpdateNotice
}
