package org.sjhstudio.diary.core.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 즐겨찾기, 휴지통, 설정처럼 화면이 바로 끝내는 쓰기의 실패 소식
 * 앱 틀이 받아 한 줄로 알림
 */
object WriteFailures {
    private val failures = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    val events: SharedFlow<Unit> = failures.asSharedFlow()

    fun report() {
        failures.tryEmit(Unit)
    }
}

/**
 * DB나 설정에 쓰는 일 시작
 * 저장 공간이 찬 기기에서는 쓰기가 예외를 던짐
 * 잡지 않으면 앱이 꺼지고 다시 눌러도 또 꺼짐
 * 사용자가 누른 쓰기라 실패하면 한 줄로 알림. 읽기와 곁일은 [launchSafely]
 */
fun CoroutineScope.launchWrite(block: suspend CoroutineScope.() -> Unit): Job =
    launchSafely(onFailure = { WriteFailures.report() }, block = block)
