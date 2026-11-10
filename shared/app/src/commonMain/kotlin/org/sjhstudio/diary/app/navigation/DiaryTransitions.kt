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

// 두 플랫폼 모두 iOS식 밀기(docs/design.md "화면 전환").
// 아래 화면은 4분의 1만 비키고, 불투명도를 낮춰 뒤의 검은 바탕으로 어두워짐
// 그래서 화면 바탕은 늘 불투명. 두 화면을 같이 흐리게 하는 전환은 쓰지 않음

private const val SLIDE_MILLIS = 300
private const val PARALLAX_DIVISOR = 4

// 들고 나는 곡선 하나. 천천히 출발하면 나가는 화면이 굼떠 보임
private val SlideEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

// 다크 바탕은 검은색에 가까워 더 어둡게
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

/** 뒤로 가기 버튼으로 뺄 때 */
internal fun <T : Any> popTransition(dimmedAlpha: Float): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
    slideBack(eased = true, dimmedAlpha = dimmedAlpha)
}

/** 뒤로 가기 스와이프 중. 손가락을 따라가도록 곡선 없이 진행도만큼 */
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

/** 탭 이동. 쌓는 화면이 아니라 바로 바꿈 */
internal val TabSwap: ContentTransform get() = ContentTransform(EnterTransition.None, ExitTransition.None)

/**
 * 탭 첫 화면용 전환. 다른 탭 첫 화면에서 홈으로 돌아갈 때는 스와이프 중에도 밀지 않고 바로 바꿈.
 * 밀면 홈이 그 아래 쌓여 있던 것처럼 보임
 */
internal val TabTransitions: Map<String, Any> =
    NavDisplay.transitionSpec { TabSwap } + NavDisplay.popTransitionSpec { TabSwap } + NavDisplay.predictivePopTransitionSpec { TabSwap }
