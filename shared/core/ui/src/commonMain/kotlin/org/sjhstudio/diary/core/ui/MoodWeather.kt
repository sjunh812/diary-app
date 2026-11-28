package org.sjhstudio.diary.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.component.DiaryIcon
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.icon.DiaryMoodFace
import org.sjhstudio.diary.core.designsystem.icon.moodFaceLine
import org.sjhstudio.diary.core.designsystem.icon.vector
import org.sjhstudio.diary.core.designsystem.theme.DiaryClaySkin
import org.sjhstudio.diary.core.designsystem.theme.DiaryClayTone
import org.sjhstudio.diary.core.designsystem.theme.DiaryMoodColors
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.ClaySkin
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.MoodTheme
import org.sjhstudio.diary.core.model.Weather
import org.sjhstudio.diary.core.ui.resources.Res
import org.sjhstudio.diary.core.ui.resources.mood_angry
import org.sjhstudio.diary.core.ui.resources.mood_annoyed
import org.sjhstudio.diary.core.ui.resources.mood_anxious
import org.sjhstudio.diary.core.ui.resources.mood_bad
import org.sjhstudio.diary.core.ui.resources.mood_bored
import org.sjhstudio.diary.core.ui.resources.mood_calm
import org.sjhstudio.diary.core.ui.resources.mood_cool
import org.sjhstudio.diary.core.ui.resources.mood_excited
import org.sjhstudio.diary.core.ui.resources.mood_good
import org.sjhstudio.diary.core.ui.resources.mood_laugh
import org.sjhstudio.diary.core.ui.resources.mood_meh
import org.sjhstudio.diary.core.ui.resources.mood_proud
import org.sjhstudio.diary.core.ui.resources.mood_sad
import org.sjhstudio.diary.core.ui.resources.mood_sick
import org.sjhstudio.diary.core.ui.resources.mood_tired
import org.sjhstudio.diary.core.ui.resources.weather_cloudy
import org.sjhstudio.diary.core.ui.resources.weather_mostly_cloudy
import org.sjhstudio.diary.core.ui.resources.weather_partly_cloudy
import org.sjhstudio.diary.core.ui.resources.weather_rain
import org.sjhstudio.diary.core.ui.resources.weather_sleet
import org.sjhstudio.diary.core.ui.resources.weather_snow
import org.sjhstudio.diary.core.ui.resources.weather_sunny

/**
 * 얼굴 선 색
 * 기분 색은 두 테마에서 같은 파스텔이라 선도 어두운 색 하나(Figma 기분 칸)
 */
val MoodFaceLine = Color(0xFF1A1C20)

/**
 * 기분 얼굴
 * 그림만으로 구별하지 않게 이름과 함께 씀
 * [contentDescription]이 없으면 장식
 */
@Composable
fun MoodFace(mood: Mood, size: Dp, modifier: Modifier = Modifier, contentDescription: String? = null) {
    val colors = DiaryTheme.colors
    val dark = colors.canvas.luminance() < DARK_LUMINANCE
    val look = LocalMoodLook.current
    if (look.theme == MoodTheme.CLAY) {
        ClayFace(look, mood, size, contentDescription, modifier)
        return
    }
    val small = smallArt(size * MOOD_ART_SCALE)
    val art = remember(look, mood, dark, small) { look.art(mood, dark, small) }
    if (art != null) {
        // 그림 칸이 얼굴 칸보다 커서 얼굴 칸 밖으로 고르게 넘침. 자리는 얼굴 칸만 차지
        Box(modifier.size(size), contentAlignment = Alignment.Center) {
            Image(painterResource(art), contentDescription, Modifier.requiredSize(size * MOOD_ART_SCALE))
        }
        return
    }
    val face = remember(mood) { mood.face() }
    Box(modifier.size(size).clip(DiaryShapes.full).background(DiaryTheme.colors.mood.of(mood))) {
        Image(
            painter = rememberVectorPainter(remember(face, size) { face.vector(size) }),
            contentDescription = contentDescription,
            // 나쁨만 바탕이 어두워 선을 희게 둠
            colorFilter = ColorFilter.tint(if (mood == Mood.BAD) Color.White else MoodFaceLine)
        )
    }
}

/** 클레이 얼굴은 겹을 쌓아 그림. 머리는 기분 색, 얼굴과 코는 고른 피부톤을 흰 그림에 곱함 */
@Composable
private fun ClayFace(look: ShownMoodLook, mood: Mood, size: Dp, contentDescription: String?, modifier: Modifier = Modifier) {
    val hair = DiaryTheme.colors.mood.of(mood)
    val small = smallArt(size * MOOD_ART_SCALE)
    val layers = remember(look, mood, small) { look.clayLayers(mood, small) }
    val tone = look.skin.tone
    Box(
        modifier.size(size).semantics {
            if (contentDescription !=
                null
            ) {
                this.contentDescription = contentDescription
            }
        },
        contentAlignment = Alignment.Center
    ) {
        for (layer in layers) {
            val tint =
                when (layer.tint) {
                    Tint.HAIR -> hair
                    Tint.SKIN -> tone.face
                    Tint.NOSE -> tone.nose
                    null -> null
                }
            Image(
                painterResource(layer.art),
                contentDescription = null,
                modifier = Modifier.requiredSize(size * MOOD_ART_SCALE),
                colorFilter = tint?.let { ColorFilter.tint(it, BlendMode.Modulate) }
            )
        }
    }
}

/**
 * 기분 얼굴의 실루엣을 [color] 한 색으로. 기본 테마와 기분 없음은 원, 테마 그림은 그 그림 모양
 * [grow]만큼 둘레를 키워 겹친 얼굴 사이를 띄우는 데 씀(달력 하루 여러 편)
 */
@Composable
fun MoodSilhouette(mood: Mood?, size: Dp, color: Color, modifier: Modifier = Modifier, grow: Dp = Dp.Hairline) {
    val dark = DiaryTheme.colors.canvas.luminance() < DARK_LUMINANCE
    val look = LocalMoodLook.current
    // 앞 얼굴과 같은 그림을 써서 한 칸에 두 벌을 읽지 않게. 둘레는 몇 dp라 화질 차이가 없음
    val small = smallArt(size * MOOD_ART_SCALE)
    val arts =
        remember(look, mood, dark, small) {
            when {
                mood == null -> emptyList()
                look.theme == MoodTheme.CLAY -> look.clayLayers(mood, small).map { it.art }
                else -> listOfNotNull(look.art(mood, dark, small))
            }
        }
    // 겹(클레이)이 겹쳐도 진해지지 않게 투명도는 묶음 전체에 한 번만
    val solid = color.copy(alpha = 1f)
    Box(modifier.size(size).graphicsLayer { alpha = color.alpha }, contentAlignment = Alignment.Center) {
        if (arts.isEmpty()) {
            Box(Modifier.requiredSize(size + grow * 2).clip(DiaryShapes.full).background(solid))
        } else {
            // 반투명 그림(비눗방울)도 속까지 한 색으로 채움
            val filter = remember(solid) { solidFilter(solid) }
            for (art in arts) {
                Image(
                    painterResource(art),
                    contentDescription = null,
                    Modifier.requiredSize(size * MOOD_ART_SCALE + grow * 2),
                    colorFilter = filter
                )
            }
        }
    }
}

/** 그림 알파를 이만큼 키워 가장자리만 부드럽게 남기고 속은 불투명하게 */
private const val SILHOUETTE_ALPHA_BOOST = 8f

/** 색 행렬의 더하는 값은 0~255 */
private const val CHANNEL_MAX = 255f

private fun solidFilter(color: Color): ColorFilter = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            0f, 0f, 0f, 0f, color.red * CHANNEL_MAX,
            0f, 0f, 0f, 0f, color.green * CHANNEL_MAX,
            0f, 0f, 0f, 0f, color.blue * CHANNEL_MAX,
            0f, 0f, 0f, SILHOUETTE_ALPHA_BOOST, 0f
        )
    )
)

private const val DARK_LUMINANCE = 0.5f

/**
 * 기분 없이 글만 쓴 일기
 * 회색 원 안 글줄 둘. 굵기는 기분 얼굴의 선과 같음
 */
@Composable
fun NoMoodFace(size: Dp, modifier: Modifier = Modifier) {
    val colors = DiaryTheme.colors
    val line = colors.textSecondary
    Canvas(modifier.size(size).clip(DiaryShapes.full).background(colors.sunken)) {
        val unit = this.size.width / NO_MOOD_GRID
        val stroke = moodFaceLine(size).toPx()
        // 둥근 끝이 선 밖으로 나오므로 그만큼 안쪽에서 끝냄
        val left = NO_MOOD_LEFT * unit + stroke / 2
        drawLine(
            line,
            Offset(left, NO_MOOD_FIRST_Y * unit),
            Offset(NO_MOOD_FIRST_END * unit - stroke / 2, NO_MOOD_FIRST_Y * unit),
            stroke,
            StrokeCap.Round
        )
        drawLine(
            line,
            Offset(left, NO_MOOD_SECOND_Y * unit),
            Offset(NO_MOOD_SECOND_END * unit - stroke / 2, NO_MOOD_SECOND_Y * unit),
            stroke,
            StrokeCap.Round
        )
    }
}

// 칸 28 기준의 글줄 자리
private const val NO_MOOD_GRID = 28f
private const val NO_MOOD_LEFT = 8f
private const val NO_MOOD_FIRST_END = 20f
private const val NO_MOOD_SECOND_END = 16f
private const val NO_MOOD_FIRST_Y = 12f
private const val NO_MOOD_SECOND_Y = 16f

/** 날씨 아이콘 */
@Composable
fun WeatherIcon(weather: Weather, size: Dp, tint: Color, modifier: Modifier = Modifier, contentDescription: String? = null) {
    DiaryIcon(weather.icon(), contentDescription = contentDescription, tint = tint, size = size, modifier = modifier)
}

fun Mood.label(): StringResource = when (this) {
    Mood.ANGRY -> Res.string.mood_angry
    Mood.COOL -> Res.string.mood_cool
    Mood.SAD -> Res.string.mood_sad
    Mood.SICK -> Res.string.mood_sick
    Mood.LAUGH -> Res.string.mood_laugh
    Mood.MEH -> Res.string.mood_meh
    Mood.BAD -> Res.string.mood_bad
    Mood.GOOD -> Res.string.mood_good
    Mood.TIRED -> Res.string.mood_tired
    Mood.EXCITED -> Res.string.mood_excited
    Mood.PROUD -> Res.string.mood_proud
    Mood.CALM -> Res.string.mood_calm
    Mood.BORED -> Res.string.mood_bored
    Mood.ANXIOUS -> Res.string.mood_anxious
    Mood.ANNOYED -> Res.string.mood_annoyed
}

fun Weather.label(): StringResource = when (this) {
    Weather.SUNNY -> Res.string.weather_sunny
    Weather.PARTLY_CLOUDY -> Res.string.weather_partly_cloudy
    Weather.MOSTLY_CLOUDY -> Res.string.weather_mostly_cloudy
    Weather.CLOUDY -> Res.string.weather_cloudy
    Weather.RAIN -> Res.string.weather_rain
    Weather.SLEET -> Res.string.weather_sleet
    Weather.SNOW -> Res.string.weather_snow
}

@Composable
fun moodName(mood: Mood): String = stringResource(mood.label())

@Composable
fun weatherName(weather: Weather): String = stringResource(weather.label())

private fun Mood.face(): DiaryMoodFace = when (this) {
    Mood.ANGRY -> DiaryMoodFace.Angry
    Mood.COOL -> DiaryMoodFace.Cool
    Mood.SAD -> DiaryMoodFace.Sad
    Mood.SICK -> DiaryMoodFace.Sick
    Mood.LAUGH -> DiaryMoodFace.Laugh
    Mood.MEH -> DiaryMoodFace.Meh
    Mood.BAD -> DiaryMoodFace.Bad
    Mood.GOOD -> DiaryMoodFace.Good
    Mood.TIRED -> DiaryMoodFace.Tired
    Mood.EXCITED -> DiaryMoodFace.Excited
    Mood.PROUD -> DiaryMoodFace.Proud
    Mood.CALM -> DiaryMoodFace.Calm
    Mood.BORED -> DiaryMoodFace.Bored
    Mood.ANXIOUS -> DiaryMoodFace.Anxious
    Mood.ANNOYED -> DiaryMoodFace.Annoyed
}

fun Weather.icon(): ImageVector = when (this) {
    Weather.SUNNY -> DiaryIcons.WeatherSunny
    Weather.PARTLY_CLOUDY -> DiaryIcons.WeatherPartlyCloudy
    Weather.MOSTLY_CLOUDY -> DiaryIcons.WeatherMostlyCloudy
    Weather.CLOUDY -> DiaryIcons.WeatherCloudy
    Weather.RAIN -> DiaryIcons.WeatherRain
    Weather.SLEET -> DiaryIcons.WeatherSleet
    Weather.SNOW -> DiaryIcons.WeatherSnow
}

/**
 * 기분 색
 * 그래프처럼 얼굴 없이 색만 쓸 때
 */
@Composable
fun moodColor(mood: Mood): Color = DiaryTheme.colors.mood.of(mood)

private fun DiaryMoodColors.of(mood: Mood): Color = when (mood) {
    Mood.ANGRY -> angry
    Mood.COOL -> cool
    Mood.SAD -> sad
    Mood.SICK -> sick
    Mood.LAUGH -> laugh
    Mood.MEH -> meh
    Mood.BAD -> bad
    Mood.GOOD -> good
    Mood.TIRED -> tired
    Mood.EXCITED -> excited
    Mood.PROUD -> proud
    Mood.CALM -> calm
    Mood.BORED -> bored
    Mood.ANXIOUS -> anxious
    Mood.ANNOYED -> annoyed
}

/** 피부톤마다 얼굴·코 색. 차례가 아니라 단계 이름으로 짝지어 순서가 바뀌어도 어긋나지 않게 */
val ClaySkin.tone: DiaryClayTone
    get() =
        when (this) {
            ClaySkin.SKIN1 -> DiaryClaySkin.veryLight
            ClaySkin.SKIN2 -> DiaryClaySkin.light
            ClaySkin.SKIN3 -> DiaryClaySkin.lightMedium
            ClaySkin.SKIN4 -> DiaryClaySkin.medium
            ClaySkin.SKIN5 -> DiaryClaySkin.mediumDark
            ClaySkin.SKIN6 -> DiaryClaySkin.dark
        }

/** 이 크기의 그림 칸에 작은 그림(120px)을 쓸지 */
@Composable
private fun smallArt(box: Dp): Boolean = with(LocalDensity.current) { box.toPx() } <= SMALL_ART_PX
