package org.sjhstudio.diary.app.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.unit.IntOffset
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay

// 화면 전환은 두 플랫폼 모두 iOS 내비게이션을 따른다(docs/design.md 움직임, 2026-11-10 사용자 결정).
// 새 화면은 오른쪽에서 밀려 들어오고, 아래 화면은 폭의 4분의 1만 비키며 어두워진다.
// 어둡게 하는 것은 아래 화면의 불투명도를 낮춰 화면 뒤에 깐 검은 바탕이 비치게 하는 것이다.
// 그래서 화면마다 바탕이 불투명해야 하고 두 화면을 같이 흐리게 하는 전환은 쓰지 않는다.
// Navigation 3 안드로이드 기본값(0.7초 크로스페이드)은 느려서 사라지는 화면이 오래 남아 눌렸다(사용자 지적).

private const val SLIDE_MILLIS = 300
private const val PARALLAX_DIVISOR = 4

// 들어갈 때와 나갈 때 같은 곡선을 쓴다.
// 천천히 출발하는 곡선은 나가는 화면이 굼떠 보인다(오발있과 같다).
private val SlideEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

// 다크 바탕은 검은색에 가까워 조금 어둡게 해서는 두 화면이 갈리지 않는다.
private const val LIGHT_DIMMED_ALPHA = 0.9f
private const val DARK_DIMMED_ALPHA = 0.6f

internal fun dimmedAlpha(dark: Boolean): Float = if (dark) DARK_DIMMED_ALPHA else LIGHT_DIMMED_ALPHA

internal fun <T : Any> pushTransition(dimmedAlpha: Float): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
    val fade = tween<Float>(SLIDE_MILLIS, easing = SlideEasing)
    val slide = tween<IntOffset>(SLIDE_MILLIS, easing = SlideEasing)
    ContentTransform(
        targetContentEnter = slideInHorizontally(slide) { it },
        initialContentExit = slideOutHorizontally(slide) { -it / PARALLAX_DIVISOR } + fadeOut(fade, targetAlpha = dimmedAlpha)
    )
}

/** 뒤로 가기 버튼이나 화면의 뒤로 단추로 뺄 때. */
internal fun <T : Any> popTransition(dimmedAlpha: Float): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
    slideBack(eased = true, dimmedAlpha = dimmedAlpha)
}

/**
 * 뒤로 가기 스와이프를 하는 동안.
 * 손가락을 따라오게 진행도만큼 그대로 움직이고 속도 곡선은 두지 않는다.
 */
internal fun <T : Any> predictivePopTransition(dimmedAlpha: Float): AnimatedContentTransitionScope<Scene<T>>.(Int) -> ContentTransform = {
    slideBack(eased = false, dimmedAlpha = dimmedAlpha)
}

private fun slideBack(eased: Boolean, dimmedAlpha: Float): ContentTransform {
    val easing = if (eased) SlideEasing else LinearEasing
    val fade = tween<Float>(SLIDE_MILLIS, easing = easing)
    val slide = tween<IntOffset>(SLIDE_MILLIS, easing = easing)
    return ContentTransform(
        targetContentEnter = slideInHorizontally(slide) { -it / PARALLAX_DIVISOR } + fadeIn(fade, initialAlpha = dimmedAlpha),
        initialContentExit = slideOutHorizontally(slide) { it }
    )
}

/** 탭을 옮길 때. 탭은 쌓는 화면이 아니라 밀거나 흐리게 하지 않고 바로 바꾼다. */
internal val TabSwap: ContentTransform get() = ContentTransform(EnterTransition.None, ExitTransition.None)

/**
 * 탭 맨 위 화면에 다는 전환.
 * 다른 탭의 첫 화면에서 뒤로 가 홈으로 갈 때, 뒤로 가기 스와이프를 하는 동안에도 밀지 않고 손을 떼면 바로 바꾼다.
 * 홈과 다른 탭은 쌓인 화면이 아니라서 밀어내면 홈이 그 아래 있던 것처럼 보인다.
 */
internal val TabTransitions: Map<String, Any> =
    NavDisplay.transitionSpec { TabSwap } + NavDisplay.popTransitionSpec { TabSwap } + NavDisplay.predictivePopTransitionSpec { TabSwap }
