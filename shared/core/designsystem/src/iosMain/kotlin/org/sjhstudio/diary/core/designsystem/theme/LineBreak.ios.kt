package org.sjhstudio.diary.core.designsystem.theme

import androidx.compose.ui.text.style.LineBreak

// iOS Compose에는 구절 단위 설정이 없어 문단 기본값을 쓴다. 한국어·일본어 줄바꿈은 iOS 시뮬레이터에서 따로 본다.
internal actual val PHRASE_LINE_BREAK: LineBreak = LineBreak.Paragraph
