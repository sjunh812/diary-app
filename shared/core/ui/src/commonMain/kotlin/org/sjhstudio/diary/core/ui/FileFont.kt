package org.sjhstudio.diary.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily

/**
 * 받은 글꼴 파일(`AppFiles.fontDirectory/<id>.ttf`)로 만든 글꼴.
 * 경로가 없거나 읽지 못하면 null.
 */
@Composable
fun rememberFileFontFamily(path: String?): FontFamily? = remember(path) { path?.let(::cachedFontFamily) }

/**
 * 마지막으로 읽은 글꼴 하나를 들고 있는다.
 * 글꼴 파일은 10MB 안팎이라 화면을 열 때마다 읽으면 그만큼 끊긴다.
 * 글꼴은 하나만 쓰니 하나면 충분하다.
 * 화면 스레드에서만 부른다.
 */
private var lastFont: Pair<String, FontFamily>? = null

private fun cachedFontFamily(path: String): FontFamily? {
    lastFont?.let { (cachedPath, family) -> if (cachedPath == path) return family }
    return runCatching { fontFamilyFromFile(path) }.getOrNull()?.also { lastFont = path to it }
}

internal expect fun fontFamilyFromFile(path: String): FontFamily
