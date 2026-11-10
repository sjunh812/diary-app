package org.sjhstudio.diary.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val DOT_EYES = "M 17.97 18.7 L 17.97 19.8 M 26.04 18.7 L 26.04 19.8"

/**
 * 기분 얼굴(Figma "기분 칸", 44 칸 기준 경로).
 * 바탕 원은 부르는 쪽이 기분 색으로 칠함.
 * 이름을 읽지 않아도 알 수 있게 기분마다 입 모양 말고도 구별되는 표시를 하나씩 둠.
 * 화남은 찌푸린 눈썹, 쿨은 선글라스, 슬픔은 눈물, 아픔은 질끈 감은 눈과 일그러진 입, 피곤은 감은 눈과 하품, z.
 * [lines]는 선으로, [solids]는 면으로 그림.
 */
enum class DiaryMoodFace(internal val lines: List<String>, internal val solids: List<String> = emptyList()) {
    Angry(
        listOf(
            "M 14.8 15.8 L 20 18.6 M 29.2 15.8 L 24 18.6",
            "M 18.2 21.3 L 18.2 21.9 M 25.8 21.3 L 25.8 21.9",
            "M 16.5 30.5 C 19.5 26.8 24.5 26.8 27.5 30.5"
        )
    ),
    Cool(
        lines = listOf("M 21 18.2 L 23 18.2", "M 17 27.8 C 20 29.8 24 29.6 27.5 26.6"),
        solids =
            listOf(
                "M 13 17 L 21 17 L 21 19 C 21 21.8 19.4 23 17 23 C 14.6 23 13 21.8 13 19 Z",
                "M 23 17 L 31 17 L 31 19 C 31 21.8 29.4 23 27 23 C 24.6 23 23 21.8 23 19 Z"
            )
    ),
    Sad(
        lines = listOf("M 17.97 18 L 17.97 19 M 26.04 18 L 26.04 19", "M 17.6 31 C 20 28.2 24 28.2 26.4 31"),
        solids = listOf(
            "M 27.6 21 C 26.3 23.1 25.4 24.4 25.4 25.6 C 25.4 26.9 26.4 27.8 27.6 27.8 C 28.8 27.8 29.8 26.9 29.8 25.6 C 29.8 24.4 28.9 23.1 27.6 21 Z"
        )
    ),
    Sick(
        listOf(
            "M 15.4 17.4 L 19.6 19.2 L 15.4 21 M 28.6 17.4 L 24.4 19.2 L 28.6 21",
            "M 15 28.6 C 16.6 26.4 18.4 26.4 20 28.4 C 21 29.7 23 29.7 24 28.4 C 25.6 26.4 27.4 26.4 29 28.6"
        )
    ),
    Laugh(
        lines = listOf("M 16.13 19.43 C 17.35 17.96 18.57 17.96 19.8 19.43 M 24.2 19.43 C 25.42 17.96 26.64 17.96 27.86 19.43"),
        solids = listOf("M 14.8 24.4 C 15.4 32 28.6 32 29.2 24.4 Z")
    ),
    Meh(listOf(DOT_EYES, "M 17.42 27.5 L 26.59 27.5")),
    Bad(listOf("M 15.6 20 L 19.8 18 M 28.4 20 L 24.2 18", "M 16 30 C 19.4 26 24.6 26 28 30")),
    Good(listOf(DOT_EYES, "M 15.6 25.4 C 19.6 30 24.4 30 28.4 25.4")),
    Tired(
        listOf(
            "M 15.2 19.2 C 16.6 20.9 18.6 20.9 20 19.2 M 24 19.2 C 25.4 20.9 27.4 20.9 28.8 19.2",
            "M 22 26.4 C 23.4 26.4 24.3 27.7 24.3 29.2 C 24.3 30.7 23.4 31.8 22 31.8 C 20.6 31.8 19.7 30.7 19.7 29.2 C 19.7 27.7 20.6 26.4 22 26.4 Z",
            "M 29.2 8.8 L 33.2 8.8 L 29.2 13 L 33.2 13"
        )
    )
}

/** 얼굴 선 굵기가 크기마다 달라 벡터를 크기별로 만들어 둠. */
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
    for (path in solids) builder.addPath(pathData = addPathNodes(path), fill = SolidColor(Color.Black))
    for (path in lines) {
        builder.addPath(
            pathData = addPathNodes(path),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = moodFaceLine(size).value * VIEWPORT / size.value,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
    }
    builder.build()
}

private const val VIEWPORT = 44f

/** 얼굴 선 굵기를 정하는 값. 44 칸에서 1.6. */
private const val LINE_BASE = 0.8f
private const val LINE_PER_SIZE = 55f

/**
 * 얼굴 선 굵기.
 * 작은 얼굴에서 선이 뭉치지 않게 작을수록 가늘게. 18 칸은 약 1.1, 44 칸은 1.6.
 * 얼굴 옆에 놓이는 표시도 이 굵기를 씀.
 */
fun moodFaceLine(size: Dp): Dp = (LINE_BASE + size.value / LINE_PER_SIZE).dp
