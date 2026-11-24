package org.sjhstudio.diary.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.Mood

/**
 * 로띠 파일을 앱 색으로 바꿔 그림
 * 파일은 `scripts/lottie/make_onboarding.py`가 만듦
 * 보일 때 한 번 재생하고 끝 모습에서 멈춤. 되돌아와도 다시 틀지 않음
 * 동작 줄이기와 미리보기에서는 처음부터 끝 모습
 */
@Composable
fun DiaryLottie(read: suspend () -> ByteArray, millis: Int, active: Boolean, modifier: Modifier = Modifier) {
    val still = rememberReduceMotion() || LocalInspectionMode.current
    val progress = remember { Animatable(if (still) 1f else 0f) }
    val palette = lottiePalette()
    // 파일 읽기와 색 바꾸기(수십 KB 문자열)는 화면 스레드 밖에서
    val json by produceState<String?>(null, palette) {
        value = withContext(Dispatchers.Default) { painted(read().decodeToString(), palette) }
    }
    val result = rememberLottieComposition(json) { LottieCompositionSpec.JsonString(json.orEmpty()) }
    val composition = if (json == null) null else result.value
    LaunchedEffect(active, still, composition) {
        // 다크 전환처럼 중간에 다시 불려도 남은 만큼만. 처음 길이로 다시 돌면 느리게 흐름
        val left = ((1f - progress.value) * millis).roundToInt()
        if (active && !still && composition != null) progress.animateTo(1f, tween(left, easing = LinearEasing))
    }
    Image(
        rememberLottiePainter(composition = composition, progress = { progress.value }),
        contentDescription = null,
        modifier = modifier
    )
}

@Composable
private fun lottiePalette(): Map<String, Color> {
    val colors = DiaryTheme.colors
    return mapOf(
        "@surface" to colors.surface,
        "@raised" to colors.raised,
        "@sunken" to colors.sunken,
        "@selected" to colors.selected,
        "@line" to colors.line,
        "@accent" to colors.accent,
        // 바탕 위 선과 그림. 아주 옅은 테마 색에서도 보이게(목록 아이콘과 같은 토큰)
        "@accentText" to colors.accentText,
        "@onAccent" to colors.onAccent,
        "@textPrimary" to colors.textPrimary,
        "@textTertiary" to colors.textTertiary,
        "@face" to moodColor(Mood.GOOD),
        "@moodA" to moodColor(Mood.COOL),
        "@moodB" to moodColor(Mood.LAUGH),
        "@moodC" to moodColor(Mood.TIRED),
        "@faceLine" to MoodFaceLine
    )
}

/**
 * 파일의 색 자리표시를 토큰 값으로 바꿈
 * 그래서 다크 모드 파일을 따로 두지 않음
 */
private fun painted(template: String, palette: Map<String, Color>): String = palette.entries.fold(template) { json, (name, color) ->
    json.replace("\"$name\"", "[${color.red},${color.green},${color.blue},1]")
}
