package org.sjhstudio.diary.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 기분 얼굴 눈과 입(Figma "기분 칸", 44 칸 기준 경로). 바탕 원은 부르는 쪽이 기분 색으로 칠한다. */
enum class DiaryMoodFace(internal val paths: List<String>) {
    Angry(listOf("M 15.58 17.42 L 19.25 19.25 M 28.41 17.42 L 24.75 19.25", "M 16.5 29.33 C 20.17 26.89 23.83 26.89 27.5 29.33")),
    Cool(listOf("M 14.67 18.7 L 20.54 18.7 M 23.47 18.7 L 29.34 18.7", "M 16.5 26.58 C 20.17 29.02 23.83 29.02 27.5 26.58")),
    Sad(listOf("M 17.97 18.7 L 17.97 19.8 M 26.04 18.7 L 26.04 19.8", "M 16.5 30.25 C 20.17 27.2 23.83 27.2 27.5 30.25")),
    Sick(listOf("M 17.97 18.7 L 17.97 19.8 M 26.04 18.7 L 26.04 19.8", "M 16.5 28.42 L 27.5 28.42")),
    Laugh(
        listOf(
            "M 16.13 19.43 C 17.35 17.96 18.57 17.96 19.8 19.43 M 24.2 19.43 C 25.42 17.96 26.64 17.96 27.86 19.43",
            "M 15.58 24.75 C 19.86 30.25 24.14 30.25 28.41 24.75 L 15.58 24.75 Z"
        )
    ),
    Meh(listOf("M 17.97 18.7 L 17.97 19.8 M 26.04 18.7 L 26.04 19.8", "M 17.42 27.5 L 26.59 27.5")),
    Bad(
        listOf(
            "M 17.97 18.7 L 17.97 19.8 M 26.04 18.7 L 26.04 19.8",
            "M 16.5 29.34 C 18.33 28.11 20.17 28.11 22 29.34 C 23.83 30.56 25.67 30.56 27.5 29.34"
        )
    ),
    Good(listOf("M 17.97 18.7 L 17.97 19.8 M 26.04 18.7 L 26.04 19.8", "M 16.13 25.67 C 20.04 29.34 23.95 29.34 27.86 25.67")),
    Tired(listOf("M 15.77 19.25 L 19.44 19.25 M 24.57 19.25 L 28.24 19.25", "M 18.33 28.42 C 20.77 29.64 23.22 29.64 25.66 28.42"))
}

/** 얼굴 선은 칸 크기와 상관없이 1.6이다. 그래서 크기마다 따로 만들어 둔다. */
private val cache = HashMap<Pair<DiaryMoodFace, Dp>, ImageVector>()

fun DiaryMoodFace.vector(size: Dp): ImageVector = cache.getOrPut(this to size) {
    val builder =
        ImageVector.Builder(
            name = "mood-$name",
            defaultWidth = size,
            defaultHeight = size,
            viewportWidth = VIEWPORT,
            viewportHeight = VIEWPORT
        )
    for (path in paths) {
        builder.addPath(
            pathData = addPathNodes(path),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = LINE.value * VIEWPORT / size.value,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
    }
    builder.build()
}

private const val VIEWPORT = 44f
private val LINE = 1.6.dp
