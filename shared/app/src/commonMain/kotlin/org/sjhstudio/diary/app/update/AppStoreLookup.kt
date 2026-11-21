package org.sjhstudio.diary.app.update

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * App Store 공개 조회(`itunes.apple.com/lookup?id=`) 응답에서 버전만 읽음(스펙 009 research R2)
 * 그 나라 스토어에 앱이 없으면 결과가 비어 null
 */
internal object AppStoreLookup {
    const val APP_ID = "6835116731"
    const val URL = "https://itunes.apple.com/lookup"

    private val json = Json { ignoreUnknownKeys = true }

    fun version(body: String): String? =
        json.decodeFromString<Response>(body).results.firstNotNullOfOrNull { result -> result.version?.trim()?.takeIf(String::isNotEmpty) }

    @Serializable
    private data class Response(val results: List<Result> = emptyList())

    @Serializable
    private data class Result(val version: String? = null)
}
