package org.sjhstudio.diary.core.drive

import io.ktor.client.HttpClient

/** 플랫폼 엔진으로 만든다(Android OkHttp, iOS Darwin). 이어 올리기의 308을 스스로 읽도록 따라가지 않는다. */
expect fun driveHttpClient(): HttpClient
