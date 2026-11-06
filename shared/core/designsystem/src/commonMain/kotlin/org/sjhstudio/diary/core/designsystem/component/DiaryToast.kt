package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.sjhstudio.diary.core.designsystem.theme.DiaryMotion
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme

enum class DiaryToastResult { Dismissed, ActionPerformed }

/**
 * 토스트 하나.
 * 앞의 것이 사라져야 다음 것이 뜬다.
 */
class DiaryToastData internal constructor(
    val message: String,
    val actionLabel: String?,
    private val continuation: CancellableContinuation<DiaryToastResult>
) {
    internal fun finish(result: DiaryToastResult) {
        if (continuation.isActive) continuation.resume(result)
    }
}

/** 화면 모델이 아니라 화면에서 만들어 [DiaryToastHost]에 넘긴다. */
@Stable
class DiaryToastState {
    private val mutex = Mutex()

    var current: DiaryToastData? by mutableStateOf(null)
        private set

    suspend fun show(message: String, actionLabel: String? = null): DiaryToastResult = mutex.withLock {
        try {
            suspendCancellableCoroutine { continuation -> current = DiaryToastData(message, actionLabel, continuation) }
        } finally {
            current = null
        }
    }
}

/**
 * 토스트(`docs/design.md` 토스트, TDS Toast, SEED Snackbar).
 * 폭 화면 - 40, 최대 400 · 높이 48 · 한 줄 full, 여러 줄 20 · bodyStrong.
 * 버튼이 있으면 5초, 없으면 3초 보이고(TDS Toast), 누르는 동안은 사라지지 않는다.
 * 탭과 쓰기 버튼 위에 오도록 부르는 쪽이 자리를 잡는다.
 */
@Composable
fun DiaryToastHost(state: DiaryToastState, modifier: Modifier = Modifier) {
    val data = state.current
    var pressed by remember { mutableStateOf(false) }
    LaunchedEffect(data, pressed) {
        if (data == null || pressed) return@LaunchedEffect
        delay(if (data.actionLabel != null) ACTION_MILLIS else PLAIN_MILLIS)
        data.finish(DiaryToastResult.Dismissed)
    }
    AnimatedVisibility(
        visible = data != null,
        enter = fadeIn(tween(DiaryMotion.QUICK_MILLIS)),
        exit = fadeOut(tween(DiaryMotion.QUICK_MILLIS)),
        modifier = modifier
    ) {
        val shown = data ?: return@AnimatedVisibility
        val colors = DiaryTheme.colors
        var multiline by remember(shown) { mutableStateOf(false) }
        val action = shown.actionLabel
        Row(
            modifier =
                Modifier
                    .padding(horizontal = DiarySpacing.x5)
                    .widthIn(max = MaxWidth)
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(if (multiline) DiaryShapes.r20 else DiaryShapes.full)
                    .background(colors.toast)
                    .pointerInput(shown) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            pressed = true
                            waitForUpOrCancellation()
                            pressed = false
                        }
                    }.semantics { liveRegion = LiveRegionMode.Polite }
                    // 버튼은 토스트 높이를 다 써서 누르는 곳이 48이 된다.
                    // 그래서 위아래 여백은 글에만 주고, 오른쪽은 버튼 안쪽 8만큼 줄인다.
                    .padding(start = DiarySpacing.x5, end = if (action != null) DiarySpacing.x3 else DiarySpacing.x5),
            horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x3),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicText(
                text = shown.message,
                style = DiaryTheme.typography.bodyStrong.copy(color = colors.onToast),
                onTextLayout = { multiline = it.lineCount > 1 },
                modifier = Modifier.weight(1f).padding(vertical = DiarySpacing.x3)
            )
            if (action != null) {
                val interaction = remember { MutableInteractionSource() }
                Box(
                    Modifier
                        .heightIn(min = DiarySpacing.minTouchTarget)
                        .widthIn(min = DiarySpacing.minTouchTarget)
                        .clickable(interactionSource = interaction, indication = null, role = Role.Button) {
                            shown.finish(DiaryToastResult.ActionPerformed)
                        }
                        // 누름 면이 토스트 끝에 닿지 않게 위아래를 조금 안으로 넣는다.
                        .padding(vertical = DiarySpacing.x1_5)
                        .diaryPress(interaction, colors.onToast, DiaryShapes.r10)
                        .padding(horizontal = DiarySpacing.x2),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(text = action, style = DiaryTheme.typography.bodyStrong.copy(color = colors.toastAction))
                }
            }
        }
    }
}

// 태블릿에서도 폰과 같은 크기로 보이게 막는다(M3 스낵바 344~672, 안드로이드 태블릿 스낵바 576 안쪽).
private val MaxWidth = 400.dp
private const val ACTION_MILLIS = 5_000L
private const val PLAIN_MILLIS = 3_000L
