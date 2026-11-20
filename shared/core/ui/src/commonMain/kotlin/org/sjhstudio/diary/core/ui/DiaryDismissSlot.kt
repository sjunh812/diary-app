package org.sjhstudio.diary.core.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.sjhstudio.diary.core.designsystem.theme.DiaryMotion

/**
 * 목록 위 카드·안내 자리(`docs/design.md` 움직임 "카드 닫기")
 * 닫으면 흐려진 뒤 자리가 접혀 아래 목록이 따라 올라옴(M3: 나가는 것은 짧게, 남은 것이 그 뒤 자리를 채움)
 * 새로 생기면 자리가 열린 뒤 나타남. 처음 그릴 때는 움직이지 않음. 동작 줄이기를 켜면 바로 바뀜
 * [key]가 같으면 같은 카드라 안의 글만 바뀌고 움직이지 않음
 */
@Composable
fun <T> DiaryDismissSlot(target: T, modifier: Modifier = Modifier, key: (T) -> Any? = { it }, content: @Composable (T) -> Unit) {
    val reduceMotion = rememberReduceMotion()
    AnimatedContent(
        target,
        modifier,
        transitionSpec = { if (reduceMotion || key(initialState) == key(targetState)) Instant else FadeThenFold },
        contentAlignment = Alignment.TopStart,
        label = "dismiss",
        contentKey = key
    ) { content(it) }
}

private val FadeThenFold =
    ContentTransform(
        targetContentEnter = fadeIn(tween(DiaryMotion.QUICK_MILLIS, delayMillis = DiaryMotion.QUICK_MILLIS)),
        initialContentExit = fadeOut(tween(DiaryMotion.EXIT_MILLIS)),
        sizeTransform =
            SizeTransform(clip = true) { _, _ ->
                tween(DiaryMotion.QUICK_MILLIS, delayMillis = DiaryMotion.EXIT_MILLIS, easing = DiaryMotion.decelerate)
            }
    )

private val Instant =
    ContentTransform(fadeIn(tween(0)), fadeOut(tween(0)), sizeTransform = SizeTransform(clip = true) { _, _ -> tween(0) })
