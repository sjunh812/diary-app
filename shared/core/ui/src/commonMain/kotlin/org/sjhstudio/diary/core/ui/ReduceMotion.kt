package org.sjhstudio.diary.core.ui

import androidx.compose.runtime.Composable

/** 기기의 "동작 줄이기"가 켜졌는지 본다. 켜졌으면 장식 움직임을 건너뛰고 끝 모습만 보인다(`docs/design.md` 동작 줄이기). */
@Composable
expect fun rememberReduceMotion(): Boolean
