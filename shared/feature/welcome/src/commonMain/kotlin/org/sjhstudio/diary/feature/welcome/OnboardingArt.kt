package org.sjhstudio.diary.feature.welcome

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
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
import org.sjhstudio.diary.core.designsystem.theme.DiaryArtSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.ui.MoodFaceLine
import org.sjhstudio.diary.core.ui.moodColor
import org.sjhstudio.diary.core.ui.rememberReduceMotion
import org.sjhstudio.diary.feature.welcome.resources.Res

private const val WRITE_FILE = "files/onboarding_write.json"
private const val WRITE_MILLIS = 4_900
private const val BACKUP_FILE = "files/onboarding_backup.json"
private const val BACKUP_MILLIS = 3_200

/** 쓰는 그림. 기분을 고르고, 낱말을 하나씩 쓰고, 저장한다. 글자가 없어 언어를 타지 않는다. */
@Composable
internal fun WriteArt(active: Boolean, modifier: Modifier = Modifier) = LottieArt(WRITE_FILE, WRITE_MILLIS, active, modifier)

/** 백업 그림. 휴대폰의 글과 사진이 내 드라이브로 올라간다. 휴대폰 쪽 기록은 그대로 남는다. */
@Composable
internal fun BackupArt(active: Boolean, modifier: Modifier = Modifier) = LottieArt(BACKUP_FILE, BACKUP_MILLIS, active, modifier)

/**
 * 로띠 파일을 앱 색으로 바꿔 그린다. 파일은 `scripts/lottie/make_onboarding.py`가 만든다.
 * 장이 보일 때 한 번 재생하고 끝 모습에서 멈추며, 되돌아와도 다시 틀지 않는다.
 * 동작 줄이기와 미리보기에서는 처음부터 끝 모습이다.
 */
@Composable
private fun LottieArt(file: String, millis: Int, active: Boolean, modifier: Modifier) {
    val still = rememberReduceMotion() || LocalInspectionMode.current
    val progress = remember { Animatable(if (still) 1f else 0f) }
    val palette = palette()
    val template by produceState<String?>(null, file) { value = Res.readBytes(file).decodeToString() }
    val json = remember(template, palette) { template?.let { painted(it, palette) } }
    val result = rememberLottieComposition(json) {
        LottieCompositionSpec.JsonString(json.orEmpty())
    }
    val composition = if (json == null) null else result.value
    LaunchedEffect(active, still, composition) {
        if (active && !still && composition != null) progress.animateTo(1f, tween(millis, easing = LinearEasing))
    }
    Image(
        painter = rememberLottiePainter(composition = composition, progress = { progress.value }),
        contentDescription = null,
        modifier = modifier.size(DiaryArtSize.width, DiaryArtSize.height)
    )
}

@Composable
private fun palette(): Map<String, Color> {
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
