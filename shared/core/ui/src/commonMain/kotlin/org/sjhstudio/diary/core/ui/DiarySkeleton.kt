package org.sjhstudio.diary.core.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.delay
import org.sjhstudio.diary.core.designsystem.component.DiaryEntryDateColumn
import org.sjhstudio.diary.core.designsystem.component.DiaryEntryDateGap
import org.sjhstudio.diary.core.designsystem.theme.DiaryHomeSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.LocalScreenGutter

private const val SHOW_AFTER_MILLIS = 300L
private const val PULSE_MILLIS = 650
private const val PULSE_LOW = 0.35f

/**
 * 스켈레톤(`docs/design.md` 스켈레톤)
 * 자리 모양을 아는 목록이 네트워크나 많은 기록 때문에 늦을 때만 씀
 * 짧은 로딩에서 깜빡이지 않게 0.3초 뒤에 나타남. 끝없는 움직임을 줄이려 빛이 지나가는 대신 천천히 숨 쉼
 * 동작 줄이기를 켜면 멈춘 칸으로 보임
 * 그림일 뿐이라 화면 낭독기가 읽지 않음
 */
@Composable
fun DiarySkeleton(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    // 미리보기에는 시간이 흐르지 않아서 바로 보임
    val inspecting = LocalInspectionMode.current
    var visible by remember { mutableStateOf(inspecting) }
    LaunchedEffect(Unit) {
        delay(SHOW_AFTER_MILLIS)
        visible = true
    }
    if (!visible) return
    val pulse: () -> Float =
        if (rememberReduceMotion()) {
            { 1f }
        } else {
            val transition = rememberInfiniteTransition()
            val value = transition.animateFloat(1f, PULSE_LOW, infiniteRepeatable(tween(PULSE_MILLIS), RepeatMode.Reverse))
            remember(value) { { value.value } }
        }
    // 숨 쉬는 것은 뼈대 칸뿐
    // 카드 바탕처럼 실제 화면에도 있는 면은 그대로 둬서 내용이 들어올 때 카드가 깜빡이지 않음
    CompositionLocalProvider(LocalSkeletonPulse provides pulse) {
        Box(modifier.clearAndSetSemantics {}) { content() }
    }
}

/**
 * 뼈대 칸의 숨 쉬는 정도
 * 매 프레임 값을 그리기 단계에서만 읽어서 숨 쉬는 동안 화면을 다시 짜지 않음
 */
@Suppress("CompositionLocalAllowlist")
private val LocalSkeletonPulse = staticCompositionLocalOf<() -> Float> { { 1f } }

/** 글 한 줄이나 사진 자리 하나 */
@Composable
fun SkeletonBlock(modifier: Modifier = Modifier, shape: Shape = DiaryShapes.r8) {
    val pulse = LocalSkeletonPulse.current
    Box(modifier.graphicsLayer { alpha = pulse() }.clip(shape).background(DiaryTheme.colors.sunken))
}

/**
 * 글줄 하나
 * 높이는 글자 높이쯤
 */
@Composable
fun SkeletonLine(modifier: Modifier = Modifier, height: Dp = DiarySpacing.x3) {
    SkeletonBlock(modifier.height(height), DiaryShapes.full)
}

/** 홈 피드 일기 줄 자리(날짜, 시각과 기분, 글 두 줄). 날짜 열, 사이, 얼굴은 실제 일기 줄과 같음 */
@Composable
fun SkeletonEntryRows(modifier: Modifier = Modifier, count: Int = 6) {
    DiarySkeleton(modifier) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = LocalScreenGutter.current),
            verticalArrangement = Arrangement.spacedBy(DiarySpacing.x8)
        ) {
            repeat(count) { index ->
                Row(horizontalArrangement = Arrangement.spacedBy(DiaryEntryDateGap)) {
                    Column(
                        Modifier.width(DiaryEntryDateColumn),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x2)
                    ) {
                        SkeletonLine(Modifier.width(DiarySpacing.x4))
                        SkeletonLine(Modifier.width(DiarySpacing.x6), DiarySpacing.x5)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(DiarySpacing.x2)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2), verticalAlignment = Alignment.CenterVertically) {
                            SkeletonBlock(Modifier.size(DiaryEntryFaceSize), DiaryShapes.full)
                            SkeletonLine(Modifier.widthIn(max = DiarySpacing.x14).fillMaxWidth())
                        }
                        SkeletonLine(Modifier.fillMaxWidth())
                        SkeletonLine(Modifier.fillMaxWidth(if (index % 2 == 0) 0.7f else 0.5f))
                    }
                }
            }
        }
    }
}

/** 홈 카드 보기 자리. 카드 면과 여백은 실제 카드와 같음(`docs/design.md` 카드) */
@Composable
fun SkeletonEntryCards(modifier: Modifier = Modifier, count: Int = 4) {
    DiarySkeleton(modifier) {
        Column(Modifier.fillMaxWidth().padding(horizontal = DiarySpacing.x4), verticalArrangement = Arrangement.spacedBy(DiarySpacing.x3)) {
            repeat(count) { index ->
                Column(
                    Modifier.fillMaxWidth().clip(DiaryShapes.r16).background(DiaryTheme.colors.surfaceCard).padding(DiarySpacing.x4),
                    verticalArrangement = Arrangement.spacedBy(DiarySpacing.x2)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(DiarySpacing.x2)) {
                            SkeletonLine(Modifier.width(DiarySpacing.x10), DiarySpacing.x5)
                            SkeletonLine(Modifier.width(DiarySpacing.x14))
                        }
                        SkeletonBlock(Modifier.size(DiaryHomeSize.cardFace), DiaryShapes.full)
                    }
                    SkeletonLine(Modifier.fillMaxWidth())
                    SkeletonLine(Modifier.fillMaxWidth(if (index % 2 == 0) 0.7f else 0.5f))
                }
            }
        }
    }
}

/** 홈 사진 격자 자리. 칸 수와 틈은 실제 격자와 같음 */
@Composable
fun SkeletonPhotoGrid(modifier: Modifier = Modifier, count: Int = 18) {
    DiarySkeleton(modifier) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(DiaryHomeSize.gridCell),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = DiarySpacing.x0_5),
            horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x0_5),
            verticalArrangement = Arrangement.spacedBy(DiarySpacing.x0_5),
            userScrollEnabled = false
        ) {
            items(count) { SkeletonBlock(Modifier.aspectRatio(1f), RectangleShape) }
        }
    }
}
