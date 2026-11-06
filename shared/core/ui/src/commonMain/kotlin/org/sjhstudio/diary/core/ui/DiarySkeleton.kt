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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
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
private const val PULSE_MILLIS = 900
private const val PULSE_LOW = 0.6f

/**
 * 스켈레톤(`docs/design.md` 스켈레톤).
 * 자리 모양을 아는 목록이 네트워크나 많은 기록 때문에 늦을 때만 쓴다.
 * 짧은 로딩에서 깜빡이지 않게 0.3초 뒤에 나타나고, 끝없는 움직임을 줄이려 빛이 지나가는 대신 천천히 숨 쉰다.
 * 동작 줄이기를 켜면 멈춘 칸으로 보인다.
 * 그림일 뿐이라 화면 읽기에는 읽히지 않는다.
 */
@Composable
fun DiarySkeleton(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    // 미리보기에는 시간이 흐르지 않아서 바로 보인다.
    val inspecting = LocalInspectionMode.current
    var visible by remember { mutableStateOf(inspecting) }
    LaunchedEffect(Unit) {
        delay(SHOW_AFTER_MILLIS)
        visible = true
    }
    if (!visible) return
    val alpha =
        if (rememberReduceMotion()) {
            1f
        } else {
            val transition = rememberInfiniteTransition()
            val pulse by transition.animateFloat(1f, PULSE_LOW, infiniteRepeatable(tween(PULSE_MILLIS), RepeatMode.Reverse))
            pulse
        }
    Box(modifier.alpha(alpha).clearAndSetSemantics {}) { content() }
}

/** 글 한 줄이나 사진 자리 하나. */
@Composable
fun SkeletonBlock(modifier: Modifier = Modifier, shape: Shape = DiaryShapes.r8) {
    Box(modifier.clip(shape).background(DiaryTheme.colors.sunken))
}

/**
 * 글줄 하나.
 * 높이는 글자 높이쯤이다.
 */
@Composable
fun SkeletonLine(modifier: Modifier = Modifier, height: Dp = DiarySpacing.x3) {
    SkeletonBlock(modifier.height(height), DiaryShapes.full)
}

/** 홈 피드 일기 줄 자리(날짜, 시각과 기분, 글 두 줄). 날짜 열, 사이, 얼굴은 실제 일기 줄과 같다. */
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

/** 홈 카드 보기 자리. 카드 면과 여백은 실제 카드와 같다(`docs/design.md` 카드). */
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

/** 홈 사진 격자 자리. 칸 수와 틈은 실제 격자와 같다. */
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

/**
 * 설정·복원 목록의 줄 자리.
 * [rows]만큼 왼쪽 글줄과 오른쪽 짧은 값을 그린다.
 * 이미 여백이 있는 칸에 넣을 때는 [gutter]를 0으로 준다.
 */
@Composable
fun SkeletonRows(modifier: Modifier = Modifier, rows: Int = 3, gutter: Dp = LocalScreenGutter.current) {
    DiarySkeleton(modifier) {
        Column(Modifier.fillMaxWidth().padding(horizontal = gutter), verticalArrangement = Arrangement.spacedBy(DiarySpacing.x6)) {
            repeat(rows) { index ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SkeletonLine(Modifier.fillMaxWidth(if (index % 2 == 0) 0.5f else 0.38f))
                    Spacer(Modifier.weight(1f))
                    SkeletonLine(Modifier.width(DiarySpacing.x10))
                }
            }
        }
    }
}
