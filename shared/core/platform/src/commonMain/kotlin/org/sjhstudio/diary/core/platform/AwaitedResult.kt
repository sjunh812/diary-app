package org.sjhstudio.diary.core.platform

import kotlinx.coroutines.CompletableDeferred

/**
 * 다른 화면을 다녀온 결과를 기다리는 자리. 앱과 함께 살고 메인 스레드에서만 쓴다.
 * 기다리는 동안 화면을 돌리면 화면이 새로 만들어져 결과는 새 화면으로 온다.
 * 기다림을 화면에 두면 결과가 갈 곳을 잃어 부른 쪽이 영영 멈춘다.
 */
class AwaitedResult<T> {
    private var waiting: CompletableDeferred<T?>? = null

    /** [launch]로 화면을 띄우고 [finish]가 줄 때까지 기다린다. */
    suspend fun await(launch: () -> Unit): T? {
        // 앞선 기다림은 결과가 올 길이 없다. 멈춘 채로 두지 않는다.
        waiting?.complete(null)
        val result = CompletableDeferred<T?>()
        waiting = result
        launch()
        return result.await()
    }

    fun finish(value: T?) {
        waiting?.complete(value)
        waiting = null
    }
}
