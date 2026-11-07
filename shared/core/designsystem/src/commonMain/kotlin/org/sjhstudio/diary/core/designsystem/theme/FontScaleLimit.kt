package org.sjhstudio.diary.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle

/** 칸 크기가 정해진 글자(탭·레일 이름, 시각 휠)가 따라가는 기기 글자 크기의 끝(`docs/design.md`, DECISIONS 2026-11-03). */
internal const val FIXED_CELL_MAX_FONT_SCALE = 1.3f

/**
 * 기기 글자 크기를 [max]배까지만 따르게 줄인 글자 스타일.
 * 칸이 정해진 곳에서 더 키우면 글자가 잘린다.
 * Apple 탭 막대와 휠 피커도 글자를 키우지 않는다.
 * 앱의 다른 글자는 이 함수를 쓰지 않고 기기 글자 크기를 그대로 따른다.
 */
@Composable
internal fun TextStyle.limitFontScale(max: Float = FIXED_CELL_MAX_FONT_SCALE): TextStyle {
    val fontScale = LocalDensity.current.fontScale
    if (fontScale <= max) return this
    val shrink = max / fontScale
    return copy(fontSize = fontSize * shrink, lineHeight = lineHeight * shrink)
}
