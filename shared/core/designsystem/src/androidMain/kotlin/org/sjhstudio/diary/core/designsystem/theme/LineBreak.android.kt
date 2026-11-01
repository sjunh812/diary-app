package org.sjhstudio.diary.core.designsystem.theme

import androidx.compose.ui.text.style.LineBreak

internal actual val KOREAN_LINE_BREAK: LineBreak =
    LineBreak(strategy = LineBreak.Strategy.HighQuality, strictness = LineBreak.Strictness.Normal, wordBreak = LineBreak.WordBreak.Phrase)
