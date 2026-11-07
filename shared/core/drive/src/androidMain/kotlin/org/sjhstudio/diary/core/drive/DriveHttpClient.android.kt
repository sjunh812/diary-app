package org.sjhstudio.diary.core.drive

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

actual fun driveHttpClient(): HttpClient = HttpClient(OkHttp) {
    expectSuccess = false
    followRedirects = false
}
