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

/**
 * 결과가 없는 곁일(알림, 뒷정리, 표시용 설정 쓰기)
 * 실패해도 본래 일을 멈추거나 실패로 만들지 않음
 */
inline fun quietly(block: () -> Unit) = orElseOnFailure(Unit, block)
