package org.sjhstudio.diary.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * 앱 틀 아이콘. 경로는 Lucide(ISC 라이선스, lucide.dev)를 옮겼다.
 * 선 굵기는 1.75다(`docs/design.md` 아이콘과 그림). 아이콘 세트를 확정하면 이 파일을 바꾼다.
 */
object DiaryIcons {
    val ChevronLeft: ImageVector by lazy {
        lucide("chevron-left") {
            moveTo(15f, 18f)
            lineTo(9f, 12f)
            lineTo(15f, 6f)
        }
    }

    val ChevronRight: ImageVector by lazy {
        lucide("chevron-right") {
            moveTo(9f, 18f)
            lineTo(15f, 12f)
            lineTo(9f, 6f)
        }
    }

    val Check: ImageVector by lazy {
        lucide("check") {
            moveTo(20f, 6f)
            lineTo(9f, 17f)
            lineTo(4f, 12f)
        }
    }

    val Close: ImageVector by lazy {
        lucide("x") {
            moveTo(18f, 6f)
            lineTo(6f, 18f)
            moveTo(6f, 6f)
            lineTo(18f, 18f)
        }
    }

    private fun lucide(name: String, block: PathBuilder.() -> Unit): ImageVector = ImageVector
        .Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.75f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = block
        ).build()
}
