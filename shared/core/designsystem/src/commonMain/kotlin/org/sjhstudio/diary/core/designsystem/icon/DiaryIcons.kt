package org.sjhstudio.diary.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * 앱 틀 아이콘. 경로는 Lucide(`lucide-static` 1.66.0, ISC 라이선스)의 SVG를 그대로 옮겼다.
 * `circle`, `line`, `rect`는 같은 모양의 경로로 바꿨다.
 * 선 굵기는 1.75다(`docs/design.md` 아이콘과 그림). 아이콘 세트를 확정하면 이 파일을 바꾼다.
 */
object DiaryIcons {
    private const val CIRCLE = "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0"

    val ChevronLeft: ImageVector by lazy { lucide("chevron-left", "m15 18-6-6 6-6") }
    val ChevronRight: ImageVector by lazy { lucide("chevron-right", "m9 18 6-6-6-6") }
    val Check: ImageVector by lazy { lucide("check", "M20 6 9 17l-5-5") }
    val Close: ImageVector by lazy { lucide("x", "M18 6 6 18", "m6 6 12 12") }
    val CircleCheck: ImageVector by lazy { lucide("circle-check", CIRCLE, "m16 9-5.5 5.5L8 12") }
    val CircleAlert: ImageVector by lazy { lucide("circle-alert", CIRCLE, "M12 8L12 12", "M12 16L12.01 16") }
    val Info: ImageVector by lazy { lucide("info", CIRCLE, "M12 16v-4", "M12 8h.01") }
    val BookOpen: ImageVector by lazy {
        lucide(
            "book-open",
            "M12 5v16",
            "M20.001 19A2 2 0 0022 17V5a2 2 0 00-1.999-2L16 3.002A5 5 0 0012 5a5 5 0 00-4-2H4a2 2 0 00-2 2v12a2 2 0 001.999 2H8a5 5 0 014 2 5 5 0 014-2z"
        )
    }
    val ImageOff: ImageVector by lazy {
        lucide(
            "image-off",
            "M2 2L22 22",
            "M10.41 10.41a2 2 0 1 1-2.83-2.83",
            "M13.5 13.5L6 21",
            "M18 12L21 15",
            "M3.59 3.59A1.99 1.99 0 0 0 3 5v14a2 2 0 0 0 2 2h14c.55 0 1.052-.22 1.41-.59",
            "M21 15V5a2 2 0 0 0-2-2H9"
        )
    }
    val CalendarCheck: ImageVector by lazy {
        lucide(
            "calendar-check",
            "M8 2v3",
            "M16 2v3",
            "M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z",
            "M3 9h18",
            "m9 15 2 2 4-4"
        )
    }
    val Download: ImageVector by lazy { lucide("download", "M12 15V3", "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4", "m7 10 5 5 5-5") }
    val Star: ImageVector by lazy {
        lucide(
            "star",
            "M11.525 2.295a.53.53 0 0 1 .95 0l2.31 4.679a2.123 2.123 0 0 0 1.595 1.16l5.166.756a.53.53 0 0 1 .294.904l-3.736 3.638a2.123 2.123 0 0 0-.611 1.878l.882 5.14a.53.53 0 0 1-.771.56l-4.618-2.428a2.122 2.122 0 0 0-1.973 0L6.396 21.01a.53.53 0 0 1-.77-.56l.881-5.139a2.122 2.122 0 0 0-.611-1.879L2.16 9.795a.53.53 0 0 1 .294-.906l5.165-.755a2.122 2.122 0 0 0 1.597-1.16z",
            filled = true
        )
    }
    val Mail: ImageVector by lazy {
        lucide(
            "mail",
            "m22 7-8.991 5.727a2 2 0 0 1-2.009 0L2 7",
            "M4 4h16a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z"
        )
    }

    private fun lucide(name: String, vararg paths: String, filled: Boolean = false): ImageVector {
        val builder = ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        for (path in paths) {
            builder.addPath(
                pathData = addPathNodes(path),
                fill = if (filled) SolidColor(Color.Black) else null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }
        return builder.build()
    }
}
