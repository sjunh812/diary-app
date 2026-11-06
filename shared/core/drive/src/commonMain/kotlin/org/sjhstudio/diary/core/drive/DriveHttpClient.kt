package org.sjhstudio.diary.core.drive

import io.ktor.client.HttpClient
import okio.FileSystem
import org.sjhstudio.diary.core.platform.DriveAuthorizer

/**
 * 플랫폼 엔진으로 만든다(Android OkHttp, iOS Darwin).
 * 이어 올리기의 308을 스스로 읽도록 따라가지 않는다.
 */
expect fun driveHttpClient(): HttpClient

/**
 * 앱이 쓰는 드라이브.
 * 쓰는 쪽이 Ktor를 몰라도 되게 여기서 만든다.
 */
fun createDriveApi(authorizer: DriveAuthorizer, fileSystem: FileSystem): DriveApi = KtorDriveApi(driveHttpClient(), authorizer, fileSystem)
