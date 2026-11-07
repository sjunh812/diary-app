package org.sjhstudio.diary.core.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

/** 움직임과 상태 오버레이(`docs/design.md` 상태, 움직임, M3 motion·state layer). */
object DiaryMotion {
    /** 누름, 켜고 끄기 같은 반응. */
    const val QUICK_MILLIS = 200

    /** 화면 전환, 시트. */
    const val STANDARD_MILLIS = 300

    /** 감속 곡선. */
    val decelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
}

/** 내용 위에 `textPrimary`를 덮는 비율. */
object DiaryStateAlpha {
    const val PRESSED = 0.10f
    const val HOVERED = 0.08f
    const val DRAGGED = 0.16f
    const val DISABLED_CONTENT = 0.38f
    const val DISABLED_CONTAINER = 0.12f
}
