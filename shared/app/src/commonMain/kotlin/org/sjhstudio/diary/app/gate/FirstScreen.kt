package org.sjhstudio.diary.app.gate

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import org.koin.compose.koinInject
import org.sjhstudio.diary.app.Startup

/** 사용자가 볼 첫 화면임을 알림. Android 시작 화면이 이 화면이 그려질 때까지 덮음 */
@Composable
internal fun FirstScreenShown() {
    val startup = koinInject<Startup>()
    SideEffect { startup.screenShown() }
}
