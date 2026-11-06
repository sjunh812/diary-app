package org.sjhstudio.diary.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily

/**
 * 받은 글꼴 파일(`AppFiles.fontDirectory/<id>.ttf`)로 만든 글꼴.
 * 경로가 없거나 읽지 못하면 null.
 */
@Composable
fun rememberFileFontFamily(path: String?): FontFamily? = remember(path) { path?.let { runCatching { fontFamilyFromFile(it) }.getOrNull() } }

internal expect fun fontFamilyFromFile(path: String): FontFamily
