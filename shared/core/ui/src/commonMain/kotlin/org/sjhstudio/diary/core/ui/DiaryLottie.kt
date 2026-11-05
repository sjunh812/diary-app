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
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.Mood

/**
 * 로띠 파일을 앱 색으로 바꿔 그린다. 파일은 `scripts/lottie/make_onboarding.py`가 만든다.
 * 보일 때 한 번 재생하고 끝 모습에서 멈추며, 되돌아와도 다시 틀지 않는다.
 * 동작 줄이기와 미리보기에서는 처음부터 끝 모습이다.
 */
@Composable
fun DiaryLottie(read: suspend () -> ByteArray, millis: Int, active: Boolean, modifier: Modifier = Modifier) {
    val still = rememberReduceMotion() || LocalInspectionMode.current
    val progress = remember { Animatable(if (still) 1f else 0f) }
    val palette = lottiePalette()
    val template by produceState<String?>(null) { value = read().decodeToString() }
    val json = remember(template, palette) { template?.let { painted(it, palette) } }
    val result = rememberLottieComposition(json) { LottieCompositionSpec.JsonString(json.orEmpty()) }
    val composition = if (json == null) null else result.value
    LaunchedEffect(active, still, composition) {
        if (active && !still && composition != null) progress.animateTo(1f, tween(millis, easing = LinearEasing))
    }
    Image(rememberLottiePainter(composition = composition, progress = { progress.value }), contentDescription = null, modifier = modifier)
}

@Composable
private fun lottiePalette(): Map<String, Color> {
    val colors = DiaryTheme.colors
    return mapOf(
        "@surface" to colors.surface,
        "@sunken" to colors.sunken,
        "@selected" to colors.selected,
        "@line" to colors.line,
        "@accent" to colors.accent,
        "@onAccent" to colors.onAccent,
        "@textTertiary" to colors.textTertiary,
        "@face" to moodColor(Mood.GOOD),
        "@faceLine" to MoodFaceLine
    )
}

/** 파일의 색 자리표시를 토큰 값으로 바꾼다. 그래서 다크 모드 파일을 따로 두지 않는다. */
private fun painted(template: String, palette: Map<String, Color>): String = palette.entries.fold(template) { json, (name, color) ->
    json.replace("\"$name\"", "[${color.red},${color.green},${color.blue},1]")
}
