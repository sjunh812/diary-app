package org.sjhstudio.diary.feature.welcome

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import kotlin.math.PI
import kotlin.math.sin
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.component.DiaryIcon
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryArtSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.ui.MoodFace
import org.sjhstudio.diary.core.ui.rememberReduceMotion
import org.sjhstudio.diary.feature.welcome.resources.Res
import org.sjhstudio.diary.feature.welcome.resources.onboarding_art_saved
import org.sjhstudio.diary.feature.welcome.resources.onboarding_art_text

private const val WRITE_MILLIS = 3_000
private const val BACKUP_MILLIS = 3_200

/**
 * 장이 보일 때 한 번 재생하고 끝 모습에서 멈춘다. 되돌아와도 다시 틀지 않는다.
 * 동작 줄이기와 미리보기에서는 처음부터 끝 모습이다.
 */
@Composable
private fun rememberArtProgress(active: Boolean, millis: Int): State<Float> {
    val still = rememberReduceMotion() || LocalInspectionMode.current
    val progress = remember { Animatable(if (still) 1f else 0f) }
    LaunchedEffect(active, still) {
        if (active && !still) progress.animateTo(1f, tween(millis, easing = LinearEasing))
    }
    return progress.asState()
}

/** [from]에서 [to]까지를 0에서 1로 편다. */
private fun Float.within(from: Float, to: Float): Float = ((this - from) / (to - from)).coerceIn(0f, 1f)

/** 감속 곡선에 살짝 넘쳤다 돌아오는 값. 기분 얼굴과 확인 표시가 톡 나타날 때만 쓴다. */
private fun pop(value: Float): Float {
    val back = value - 1f
    return 1f + back * back * (2.2f * back + 1.2f)
}

/** 쓰는 그림. 기분을 고르고, 한 줄을 쓰고, 저장한다. */
@Composable
internal fun WriteArt(active: Boolean, modifier: Modifier = Modifier) {
    val colors = DiaryTheme.colors
    val typography = DiaryTheme.typography
    val progress by rememberArtProgress(active, WRITE_MILLIS)
    val text = stringResource(Res.string.onboarding_art_text)
    Column(
        modifier
            .width(DiaryArtSize.width)
            .clip(DiaryShapes.r16)
            .background(colors.surface)
            .border(DiarySpacing.hairline, colors.line, DiaryShapes.r16)
            .padding(DiarySpacing.x5),
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x3)
    ) {
        MoodFace(
            Mood.GOOD,
            DiaryArtSize.face,
            Modifier.graphicsLayer {
                val scale = pop(progress.within(0f, 0.16f))
                scaleX = scale
                scaleY = scale
                alpha = progress.within(0f, 0.08f)
            }
        )
        val style = typography.reading.copy(color = colors.textPrimary)
        Box {
            // 글자가 늘어도 카드 높이가 움직이지 않게 온 글로 자리를 잡아 둔다.
            BasicText(text, style = style.copy(color = Color.Transparent))
            BasicText(text.take((text.length * progress.within(0.2f, 0.78f)).toInt()), style = style)
        }
        Row(
            Modifier.graphicsLayer { alpha = progress.within(0.82f, 0.95f) },
            horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x1),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DiaryIcon(DiaryIcons.Check, contentDescription = null, tint = colors.accentText, size = DiaryArtSize.tileIcon)
            BasicText(stringResource(Res.string.onboarding_art_saved), style = typography.captionStrong.copy(color = colors.accentText))
        }
    }
}

/** 백업 그림. 휴대폰의 글과 사진이 내 드라이브로 올라간다. 휴대폰 쪽 기록은 그대로 남는다. */
@Composable
internal fun BackupArt(active: Boolean, modifier: Modifier = Modifier) {
    val colors = DiaryTheme.colors
    val progress by rememberArtProgress(active, BACKUP_MILLIS)
    val travel = DiaryArtSize.width - DiaryArtSize.phoneWidth / 2 - DiaryArtSize.cloud / 2
    val start = DiaryArtSize.phoneWidth / 2 - DiaryArtSize.tile / 2
    Box(modifier.width(DiaryArtSize.width).height(DiaryArtSize.height)) {
        Phone(Modifier.align(Alignment.CenterStart))
        Box(Modifier.align(Alignment.CenterEnd).size(DiaryArtSize.cloud)) {
            Box(
                Modifier.fillMaxWidth().height(DiaryArtSize.cloud).clip(DiaryShapes.full).background(colors.selected),
                contentAlignment = Alignment.Center
            ) {
                DiaryIcon(DiaryIcons.Cloud, contentDescription = null, tint = colors.accent, size = DiaryArtSize.cloudIcon)
            }
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(DiaryArtSize.check)
                    .graphicsLayer {
                        val scale = pop(progress.within(0.8f, 0.96f))
                        scaleX = scale
                        scaleY = scale
                        alpha = progress.within(0.8f, 0.86f)
                    }
                    .clip(DiaryShapes.full)
                    .background(colors.accent),
                contentAlignment = Alignment.Center
            ) {
                DiaryIcon(DiaryIcons.Check, contentDescription = null, tint = colors.onAccent, size = DiaryArtSize.tileIcon)
            }
        }
        Tile(DiaryIcons.Pen, start, travel, progress.within(0f, 0.34f), Modifier.align(Alignment.CenterStart))
        Tile(DiaryIcons.Image, start, travel, progress.within(0.2f, 0.54f), Modifier.align(Alignment.CenterStart))
        Tile(DiaryIcons.Image, start, travel, progress.within(0.42f, 0.76f), Modifier.align(Alignment.CenterStart))
    }
}

/** 휴대폰 안은 일기 한 편처럼 보이게 얼굴, 글줄, 사진 칸만 그린다. */
@Composable
private fun Phone(modifier: Modifier) {
    val colors = DiaryTheme.colors
    Column(
        modifier
            .size(DiaryArtSize.phoneWidth, DiaryArtSize.phoneHeight)
            .clip(DiaryShapes.r16)
            .background(colors.surface)
            .border(DiarySpacing.hairline, colors.line, DiaryShapes.r16)
            .padding(DiarySpacing.x3),
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x2)
    ) {
        MoodFace(Mood.GOOD, DiarySpacing.x5)
        repeat(2) { Box(Modifier.fillMaxWidth().height(DiaryArtSize.line).clip(DiaryShapes.full).background(colors.sunken)) }
        Box(
            Modifier.fillMaxWidth().weight(1f).clip(DiaryShapes.r8).background(colors.sunken),
            contentAlignment = Alignment.Center
        ) {
            DiaryIcon(DiaryIcons.Image, contentDescription = null, tint = colors.textTertiary, size = DiaryArtSize.tileIcon)
        }
    }
}

/** 호를 그리며 날아가 구름에 닿으면 사라지는 조각. */
@Composable
private fun Tile(icon: ImageVector, start: Dp, travel: Dp, value: Float, modifier: Modifier) {
    val colors = DiaryTheme.colors
    if (value <= 0f || value >= 1f) return
    Box(
        modifier
            .size(DiaryArtSize.tile)
            .graphicsLayer {
                translationX = (start + travel * value).toPx()
                translationY = -(DiaryArtSize.arc.toPx() * sin(PI * value).toFloat())
                alpha = minOf(value / 0.15f, (1f - value) / 0.15f, 1f)
            }
            .clip(DiaryShapes.r8)
            .background(colors.accent),
        contentAlignment = Alignment.Center
    ) {
        DiaryIcon(icon, contentDescription = null, tint = colors.onAccent, size = DiaryArtSize.tileIcon)
    }
}
