package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.animation.core.animate
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import kotlin.math.roundToInt
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryIconSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.LocalScreenGutter

/**
 * 탭 화면 맨 위 줄(`docs/design.md` 탭 상단 바).
 * 큰 제목이 이 줄 밑으로 들어가는 만큼 왼쪽에 작은 제목이 나타나고, 다 들어가면 아래 선이 생긴다.
 * 바탕은 화면 바탕과 같은 불투명 색이다.
 * Apple HIG(큰 제목은 스크롤하면 보통 제목으로)와 Material 3(큰 바는 작은 바로 접히고 스크롤하면 바탕과 구분)를 따른다.
 *
 * [collapsed]는 0(큰 제목이 다 보임)에서 1(바 밑으로 다 들어감)까지를 돌려준다.
 * 시간이 아니라 스크롤한 만큼 바뀌어서 어중간하게 움직이지 않는다.
 * 스크롤마다 바뀌는 값이라 그리기 단계에서만 읽고, 반을 넘었는지와 다 접혔는지가 바뀔 때만 다시 구성한다.
 * [onTitleClick]이 있으면 작은 제목도 큰 제목처럼 눌러 고를 수 있다(홈의 달 고르기).
 * [accessory]는 접혔을 때만 작은 제목 옆에 붙는다(홈의 백업 경고).
 * [contentMaxWidth]는 넓은 화면에서 글과 단추를 아래 내용 칸에 맞추는 폭이다.
 */
@Composable
fun DiaryTabTopBar(
    title: String,
    collapsed: () -> Float,
    modifier: Modifier = Modifier,
    contentMaxWidth: Dp = Dp.Unspecified,
    onTitleClick: (() -> Unit)? = null,
    accessory: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    val colors = DiaryTheme.colors
    val progress by rememberUpdatedState(collapsed)
    val half by remember { derivedStateOf { progress() >= HALF } }
    val full by remember { derivedStateOf { progress() >= 1f } }
    // 선과 바탕은 창 너비 끝까지, 글과 단추는 아래 내용 칸 너비에 맞춘다.
    Column(modifier.fillMaxWidth().background(colors.canvas), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            Modifier
                .widthIn(max = contentMaxWidth)
                .fillMaxWidth()
                .height(DiarySpacing.tabTitleTop)
                .padding(start = LocalScreenGutter.current, end = DiarySpacing.x1),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                // 반투명은 이 칸 크기의 판에 그려 합친다.
                // 칸을 바 높이 전체로 두어, 넓힌 누르는 곳까지 판 안에 들어가 위아래가 잘리지 않는다(천천히 스크롤할 때 보였다, 2026-11-08 사용자 지적).
                // 색마다 투명도를 곱하는 방식은 iOS에서 먹지 않아 맨 위에서도 작은 제목이 보였다.
                Modifier.weight(1f).fillMaxHeight().graphicsLayer {
                    // 큰 제목이 반 넘게 가려진 뒤부터 나타나 두 제목이 겹쳐 보이는 순간이 없다(iOS 큰 제목과 같다).
                    // 처음부터 따라 나타나면 너무 빨리 생기는 느낌이었다(2026-11-08 사용자 지적).
                    alpha = ((progress() - HALF) / HALF).coerceIn(0f, 1f)
                },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2)
            ) {
                // 반쯤 들어가기 전에는 큰 제목이 읽히고 있으니 화면 읽기와 누르기에서 빠진다.
                if (half) SmallTitle(title, onTitleClick) else SmallTitle(title, null, Modifier.clearAndSetSemantics {})
                if (half) accessory()
            }
            actions()
        }
        Box(Modifier.fillMaxWidth().height(DiarySpacing.hairline).alpha(if (full) 1f else 0f).background(colors.line))
    }
}

@Composable
private fun SmallTitle(title: String, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    val colors = DiaryTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier
            .then(
                if (onClick == null) {
                    Modifier
                } else {
                    // 누르는 면만 글자 양옆 8까지 넓히고 글자는 큰 제목과 같은 왼쪽 선에 둔다.
                    // 넓힌 만큼 안쪽을 다시 채우지 않으면 접히는 순간 글자가 왼쪽으로 밀려 보였다(2026-11-08 사용자 지적).
                    Modifier
                        .diaryTouchOutset(horizontal = DiarySpacing.x2, vertical = DiarySpacing.x2)
                        .diaryPress(interaction, colors.textPrimary, DiaryShapes.r12)
                        .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
                        .padding(horizontal = DiarySpacing.x2)
                }
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x1)
    ) {
        BasicText(
            title,
            modifier = Modifier.weight(1f, fill = false),
            style = DiaryTheme.typography.headline.copy(color = colors.textPrimary),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (onClick !=
            null
        ) {
            DiaryIcon(DiaryIcons.ChevronDown, contentDescription = null, tint = colors.textSecondary, size = DiaryIconSize.i16)
        }
    }
}

/**
 * 목록 위 머리(큰 제목과 그 아래 줄)를 스크롤한 만큼 접는 상태.
 * 위로 올리면 머리부터 접히고, 목록이 맨 위에 닿은 뒤 더 내려야 다시 펼쳐진다(Material 3 큰 바와 같다).
 * 손을 떼면 반 넘게 접힌 쪽으로 붙는다.
 * 플랫폼 코드 없이 Compose 중첩 스크롤만 써서 Android와 iOS에서 같게 움직인다.
 */
@Stable
class DiaryCollapsingHeaderState(initialFraction: Float = 0f) {
    /** 머리 전체 높이(px). 그려질 때 정해진다. */
    var height by mutableFloatStateOf(0f)
        internal set

    private var pendingFraction = initialFraction

    /** 0(다 펼침)에서 -[height](다 접힘)까지. */
    var offset by mutableFloatStateOf(0f)
        private set

    val fraction: Float get() = if (height <= 0f) pendingFraction else (-offset / height).coerceIn(0f, 1f)

    internal fun measured(full: Float) {
        if (full == height) return
        val keep = fraction
        height = full
        offset = -keep * full
    }

    /** 탭을 다시 눌러 맨 위로 갈 때처럼 스크롤 없이 목록을 올릴 때 머리도 펼친다. */
    suspend fun expand() {
        animate(offset, 0f) { value, _ -> offset = value }
    }

    val connection: NestedScrollConnection =
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y >= 0f || height <= 0f) return Offset.Zero
                val next = (offset + available.y).coerceAtLeast(-height)
                val used = next - offset
                offset = next
                return Offset(0f, used)
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y <= 0f || height <= 0f) return Offset.Zero
                val next = (offset + available.y).coerceAtMost(0f)
                val used = next - offset
                offset = next
                return Offset(0f, used)
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                // 반쯤 걸쳐 멈추지 않게 가까운 쪽으로 붙인다.
                // 반쯤 접힌 채라면 목록은 맨 위라 펼쳐도 내용이 튀지 않는다.
                val now = fraction
                if (height > 0f && now > 0f && now < 1f) {
                    val target = if (now < HALF) 0f else -height
                    animate(offset, target) { value, _ -> offset = value }
                }
                return Velocity.Zero
            }
        }

    companion object {
        val Saver: Saver<DiaryCollapsingHeaderState, Float> = Saver(save = { it.fraction }, restore = { DiaryCollapsingHeaderState(it) })
    }
}

@Composable
fun rememberDiaryCollapsingHeaderState(): DiaryCollapsingHeaderState =
    rememberSaveable(saver = DiaryCollapsingHeaderState.Saver) { DiaryCollapsingHeaderState() }

/**
 * [state]만큼 위로 밀려 접히는 머리.
 * 머리 높이가 줄어드는 만큼 아래 목록이 따라 올라온다.
 * 내용은 위로 밀리며 바 밑으로 사라지고, 접히는 동안 옅어지지 않는다(큰 제목이 바 밑으로 들어가는 모습).
 */
@Composable
fun DiaryCollapsingHeader(state: DiaryCollapsingHeaderState, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content = content, modifier = modifier.fillMaxWidth().clipToBounds()) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minHeight = 0)) }
        val full = placeables.sumOf { it.height }
        state.measured(full.toFloat())
        val shown = (full + state.offset).roundToInt().coerceIn(0, full)
        layout(constraints.maxWidth, shown) {
            var y = shown - full
            for (p in placeables) {
                p.placeRelative(0, y)
                y += p.height
            }
        }
    }
}

/**
 * 스크롤 안에 큰 제목이 있는 화면(돌아보기, 설정)에서 접힌 정도를 잰다.
 * 제목이 스크롤 내용 맨 위에 있다고 보고, 스크롤한 거리를 제목 높이로 나눈다.
 */
fun titleCollapse(scrolled: Int, titleHeight: Int): Float = if (titleHeight <=
    0
) {
    0f
} else {
    (scrolled.toFloat() / titleHeight).coerceIn(0f, 1f)
}

/**
 * 스크롤이 멈췄을 때 큰 제목이 반쯤 걸쳐 있으면 가까운 쪽으로 붙인다.
 * 홈의 접히는 머리와 같은 규칙이라 탭마다 손맛이 같다.
 */
@Composable
fun SnapTitleOnRest(scroll: ScrollState, titleHeight: Int) {
    LaunchedEffect(scroll, titleHeight) {
        snapshotFlow { scroll.isScrollInProgress }.collect { moving ->
            val now = scroll.value
            if (!moving && titleHeight > 0 && now in 1 until titleHeight) {
                scroll.animateScrollTo(if (now < titleHeight / 2) 0 else titleHeight)
            }
        }
    }
}

private const val HALF = 0.5f
