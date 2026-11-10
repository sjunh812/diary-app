package org.sjhstudio.diary.core.model

import kotlin.coroutines.cancellation.CancellationException

/**
 * 곁일이 실패해도 본래 일을 멈추지 않을 때 씀
 * 실패하면 [fallback]을 돌려주고 코루틴 취소는 그대로 던짐. `runCatching`은 취소까지 삼켜 안 씀
 */
inline fun <T> orElseOnFailure(fallback: T, block: () -> T): T = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (_: Exception) {
    fallback
}
