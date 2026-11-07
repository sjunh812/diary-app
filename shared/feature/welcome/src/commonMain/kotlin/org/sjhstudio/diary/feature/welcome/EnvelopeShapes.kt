package org.sjhstudio.diary.feature.welcome

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import org.sjhstudio.diary.core.designsystem.theme.DiaryColors
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing

/** 삼각형 끝에서 곡선이 시작되는 자리. 변 길이에 대한 비율이다. */
private const val TIP_ROUND_FROM = 0.72f

/** 겹 그림자 한 층의 진하기. */
private const val SHADOW_ALPHA = 0.014f

/** 그림자 층 수. 많이 겹쳐야 띠가 보이지 않고 번진다. */
private const val SHADOW_STEPS = 8

/**
 * 끝이 둥근 삼각형.
 * 실제 봉투처럼 뾰족하지 않게 끝을 곡선으로 굴린다.
 * [baseY]에서 시작해 [tipY]까지 내려가거나 올라간다.
 */
internal fun softPoint(width: Float, baseY: Float, tipY: Float) = Path().apply {
    val leftX = width / 2 * TIP_ROUND_FROM
    val shoulderY = baseY + (tipY - baseY) * TIP_ROUND_FROM
    moveTo(0f, baseY)
    lineTo(leftX, shoulderY)
    quadraticTo(width / 2, tipY, width - leftX, shoulderY)
    lineTo(width, baseY)
}

/**
 * 곡선까지 지난 덮개 끝의 실제 높이.
 * 봉인은 이 자리에 앉는다.
 */
internal fun softTip(tip: Float) = tip * (1f + TIP_ROUND_FROM) / 2

/**
 * 종이 한 겹이 아래 겹에 드리우는 옅은 그림자.
 * 선 대신 그림자로 겹을 나눠야 종이처럼 보인다.
 */
internal fun DrawScope.softShadow(path: Path, colors: DiaryColors, direction: Float) {
    repeat(SHADOW_STEPS) { step ->
        val reach = DiarySpacing.x1_5.toPx() * (step + 1) / SHADOW_STEPS
        translate(top = reach * direction) { drawPath(path, colors.overlay.copy(alpha = SHADOW_ALPHA)) }
    }
}
