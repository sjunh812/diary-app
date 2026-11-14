package org.sjhstudio.diary.app.lock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.uikit.LocalUIViewController
import kotlinx.coroutines.flow.filter
import org.koin.compose.koinInject
import platform.UIKit.UIActivityViewController

/**
 * 잠기면 일기 글이 보이는 공유 시트를 닫음
 * 잠금 화면은 Compose 안에 그려져 그 위에 뜬 화면을 가리지 못함
 * 광고·로그인·사진 고르기는 닫지 않음. 강제로 닫으면 결과를 기다리는 SDK가 멈출 수 있음
 */
@Composable
internal fun DismissPresentedOnLock() {
    val controller = LocalUIViewController.current
    val lock = koinInject<LockController>()
    LaunchedEffect(controller, lock) {
        lock.state.filter { it == LockState.Locked || it == LockState.NeedsNewPin }.collect {
            (controller.presentedViewController as? UIActivityViewController)?.dismissViewControllerAnimated(false, completion = null)
        }
    }
}
