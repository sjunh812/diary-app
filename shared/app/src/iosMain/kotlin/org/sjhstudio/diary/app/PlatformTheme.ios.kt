package org.sjhstudio.diary.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.uikit.LocalUIViewController
import org.sjhstudio.diary.core.model.ThemeMode
import platform.UIKit.UIUserInterfaceStyle

// 창 전체의 화면 스타일을 바꿔 상태 표시줄과 시스템 창(Face ID 같은)도 앱 화면 모드를 따름
@Composable
internal actual fun ApplyPlatformTheme(mode: ThemeMode) {
    val controller = LocalUIViewController.current
    LaunchedEffect(controller, mode) {
        val style =
            when (mode) {
                ThemeMode.SYSTEM -> UIUserInterfaceStyle.UIUserInterfaceStyleUnspecified
                ThemeMode.LIGHT -> UIUserInterfaceStyle.UIUserInterfaceStyleLight
                ThemeMode.DARK -> UIUserInterfaceStyle.UIUserInterfaceStyleDark
            }
        (controller.view.window ?: controller.view).overrideUserInterfaceStyle = style
    }
}
