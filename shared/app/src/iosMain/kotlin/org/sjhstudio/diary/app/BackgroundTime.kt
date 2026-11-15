package org.sjhstudio.diary.app

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.sjhstudio.diary.core.backup.LongWork
import platform.UIKit.UIApplication
import platform.UIKit.UIBackgroundTaskIdentifier
import platform.UIKit.UIBackgroundTaskInvalid

/**
 * 백업·복원·파일 만들기·가져오기가 도는 동안 시스템 시간을 받아 둠
 * 앱을 나가도 몇 초 만에 멈추지 않게 함. Android는 WorkManager가 맡음
 * 시간이 다 돼도 일은 멈추지 않음. 멈춘 곳부터 다음에 이어 감(docs/backup.md)
 */
internal class BackgroundTime(private val longWork: LongWork) {
    private var task: UIBackgroundTaskIdentifier = UIBackgroundTaskInvalid

    fun start(scope: CoroutineScope) {
        scope.launch(Dispatchers.Main) {
            longWork.running.collect { running -> if (running) begin() else end() }
        }
    }

    private fun begin() {
        task = UIApplication.sharedApplication.beginBackgroundTaskWithName(NAME) { end() }
    }

    /** 일이 끝났거나 시스템 시간이 다 됨. 먼저 온 쪽만 돌려줌 */
    private fun end() {
        if (task == UIBackgroundTaskInvalid) return
        UIApplication.sharedApplication.endBackgroundTask(task)
        task = UIBackgroundTaskInvalid
    }

    private companion object {
        const val NAME = "org.sjhstudio.diary.data"
    }
}
