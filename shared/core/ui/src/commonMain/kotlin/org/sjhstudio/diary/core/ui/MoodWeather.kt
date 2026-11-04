package org.sjhstudio.diary.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.Dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.component.DiaryIcon
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.icon.DiaryMoodFace
import org.sjhstudio.diary.core.designsystem.icon.vector
import org.sjhstudio.diary.core.designsystem.theme.DiaryMoodColors
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.Weather
import org.sjhstudio.diary.core.ui.resources.Res
import org.sjhstudio.diary.core.ui.resources.mood_angry
import org.sjhstudio.diary.core.ui.resources.mood_bad
import org.sjhstudio.diary.core.ui.resources.mood_cool
import org.sjhstudio.diary.core.ui.resources.mood_good
import org.sjhstudio.diary.core.ui.resources.mood_laugh
import org.sjhstudio.diary.core.ui.resources.mood_meh
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

/** 얼굴 선 색. 기분 색은 두 테마에서 같은 파스텔이라 선도 어두운 색 하나다(Figma 기분 칸). */
private val FACE_LINE = Color(0xFF1A1C20)

/** 기분 얼굴. 그림만으로 구별하지 않게 이름과 함께 쓴다. [contentDescription]이 없으면 장식이다. */
@Composable
fun MoodFace(mood: Mood, size: Dp, modifier: Modifier = Modifier, contentDescription: String? = null) {
    val face = remember(mood) { mood.face() }
    Box(modifier.size(size).clip(DiaryShapes.full).background(DiaryTheme.colors.mood.of(mood))) {
        Image(
            painter = rememberVectorPainter(remember(face, size) { face.vector(size) }),
            contentDescription = contentDescription,
            // 나쁨만 바탕이 어두워 선을 희게 둔다.
            colorFilter = ColorFilter.tint(if (mood == Mood.BAD) Color.White else FACE_LINE)
        )
    }
}

/** 날씨 아이콘. */
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

/** 기분 색. 그래프처럼 얼굴 없이 색만 쓸 때 부른다. */
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
}
