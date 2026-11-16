package org.sjhstudio.diary.app.billing

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.sjhstudio.diary.core.platform.AppForeground

/** 앱이 앞으로 올 때마다 알림. 메인 스레드에서 모아야 함 */
internal fun appForegrounds(): Flow<Unit> = callbackFlow {
    val stop = AppForeground.observe(onBackground = {}, onForeground = { trySend(Unit) })
    awaitClose(stop)
}
