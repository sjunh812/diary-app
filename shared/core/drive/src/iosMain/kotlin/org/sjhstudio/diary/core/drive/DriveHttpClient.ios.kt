package org.sjhstudio.diary.core.drive

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

actual fun driveHttpClient(): HttpClient = HttpClient(Darwin) {
    expectSuccess = false
    followRedirects = false
}
