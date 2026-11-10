package org.sjhstudio.diary.core.ui

import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/*
 * 화면 모델이 앱을 끄지 않게 하는 마지막 안전망.
 * 저장 공간, 네트워크, DB 읽기처럼 예상되는 실패는 저장소가 받아 결과로 돌려준다.
 * 그래도 빠뜨린 예외가 화면 모델까지 오면 여기서 받고 앱은 그대로 쓴다.
 * 화면 모델에서 맨 `viewModelScope.launch`와 `stateIn(viewModelScope, …)`은 `coroutineSafetyCheck`가 막는다.
 */

/**
 * 실패해도 앱을 끄지 않는 `launch`.
 * 실패하면 [onFailure]가 받고, 코루틴 취소는 그대로 던진다.
 */
// 마지막 안전망이라 일부러 모든 예외를 받는다. 예상되는 실패는 저장소가 먼저 구체적으로 받는다.
@Suppress("TooGenericExceptionCaught")
fun CoroutineScope.launchSafely(onFailure: (Exception) -> Unit = {}, block: suspend CoroutineScope.() -> Unit): Job = launch {
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        onFailure(e)
    }
}

/**
 * 실패해도 앱을 끄지 않는 `stateIn`.
 * 원본 흐름이 실패하면 마지막 값을 그대로 둔다.
 */
fun <T> Flow<T>.stateInSafely(scope: CoroutineScope, started: SharingStarted, initialValue: T): StateFlow<T> =
    catch { if (it !is Exception) throw it }.stateIn(scope, started, initialValue)

/**
 * 화면 모델 흐름을 화면이 보는 동안만 돌리는 규칙.
 * 화면을 돌리거나 잠깐 다른 앱에 다녀와도 5초 안이면 다시 읽지 않는다(Now in Android `WhileUiSubscribed`와 같다).
 */
val WhileUiSubscribed: SharingStarted = SharingStarted.WhileSubscribed(5.seconds.inWholeMilliseconds)
