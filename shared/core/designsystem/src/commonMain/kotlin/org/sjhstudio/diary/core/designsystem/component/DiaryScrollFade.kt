package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.foundation.ScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing

/**
 * 아래에 내용이 더 있으면 끝을 바탕색([surface])으로 흐림
 * 잘린 줄 대신 이어진다는 것을 보임. 끝까지 내리면 사라짐
 * 스크롤 수식어보다 앞에 달아야 보이는 칸에 그려짐
 */
fun Modifier.diaryScrollFade(scroll: ScrollState, surface: Color): Modifier = drawWithContent {
    drawContent()
    if (scroll.canScrollForward) {
        val fade = DiarySpacing.x8.toPx()
        drawRect(
            Brush.verticalGradient(listOf(surface.copy(alpha = 0f), surface), size.height - fade, size.height),
            topLeft = Offset(0f, size.height - fade),
            size = Size(size.width, fade)
        )
    }
}
