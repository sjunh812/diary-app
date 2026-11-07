package org.sjhstudio.diary.core.model

import kotlin.coroutines.cancellation.CancellationException

/**
 * 곁일이 실패해도 본래 일을 멈추지 않을 때 쓴다.
 * 실패하면 [fallback]을 돌려주고, 코루틴 취소는 그대로 던진다.
 * `runCatching`은 취소까지 삼켜서 쓰지 않는다.
 */
inline fun <T> orElseOnFailure(fallback: T, block: () -> T): T = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (_: Exception) {
    fallback
}
