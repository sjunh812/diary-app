package org.sjhstudio.diary.core.designsystem.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 모서리 단계(`docs/design.md` 간격, 모서리). 이 밖의 값은 쓰지 않는다.
 * SEED radius 단계, TDS 버튼 모서리, M3 시트·대화상자 28을 합쳤다.
 */
object DiaryRadius {
    /** 작은 버튼, 뱃지. */
    val r8: Dp = 8.dp

    /** 중간 버튼. */
    val r10: Dp = 10.dp

    /** 입력칸, 검색, 사진, 날씨 칸. */
    val r12: Dp = 12.dp

    /** 보통 버튼. */
    val r14: Dp = 14.dp

    /** 큰 버튼, 쓰기 버튼, 카드, 기분 칸. */
    val r16: Dp = 16.dp

    /** 여러 줄 토스트, 숫자판 누름. */
    val r20: Dp = 20.dp

    /** 시트, 대화상자. */
    val r28: Dp = 28.dp
}

object DiaryShapes {
    val r8: Shape = RoundedCornerShape(DiaryRadius.r8)
    val r10: Shape = RoundedCornerShape(DiaryRadius.r10)
    val r12: Shape = RoundedCornerShape(DiaryRadius.r12)
    val r14: Shape = RoundedCornerShape(DiaryRadius.r14)
    val r16: Shape = RoundedCornerShape(DiaryRadius.r16)
    val r20: Shape = RoundedCornerShape(DiaryRadius.r20)
    val r28: Shape = RoundedCornerShape(DiaryRadius.r28)

    /** 칩, 한 줄 토스트, 스위치, 진행 막대. */
    val full: Shape = CircleShape
}
