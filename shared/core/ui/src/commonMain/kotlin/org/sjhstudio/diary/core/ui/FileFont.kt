package org.sjhstudio.diary.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import org.sjhstudio.diary.core.model.orElseOnFailure

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
 * 시작할 때 뒤에서 미리 읽는 쪽(FontWarmup)과 화면이 같은 글꼴 객체를 써야 미리 읽은 것이 쓰여서 함께 나눈다.
 */
@OptIn(ExperimentalAtomicApi::class)
private val lastFont = AtomicReference<Pair<String, FontFamily>?>(null)

/** [path]의 받은 글꼴. 읽지 못하면 null이다. */
@OptIn(ExperimentalAtomicApi::class)
fun cachedFontFamily(path: String): FontFamily? {
    lastFont.load()?.let { (cachedPath, family) -> if (cachedPath == path) return family }
    return orElseOnFailure(null) { fontFamilyFromFile(path) }?.also { lastFont.store(path to it) }
}

internal expect fun fontFamilyFromFile(path: String): FontFamily
