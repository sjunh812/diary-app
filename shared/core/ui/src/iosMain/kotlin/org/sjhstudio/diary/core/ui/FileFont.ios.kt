package org.sjhstudio.diary.core.ui

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.dataWithContentsOfFile
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
internal actual fun fontFamilyFromFile(path: String): FontFamily {
    val data = checkNotNull(NSData.dataWithContentsOfFile(path)) { "글꼴 파일을 읽지 못했다" }
    val bytes = ByteArray(data.length.toInt())
    if (bytes.isNotEmpty()) bytes.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
    // 같은 경로면 같은 글꼴로 본다(Skia 글꼴 캐시의 키).
    return FontFamily(Font(identity = path, data = bytes))
}
