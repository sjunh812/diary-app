package org.sjhstudio.diary.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.uikit.LocalUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIActivityViewController
import platform.UIKit.popoverPresentationController

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberShareText(): (String) -> Unit {
    val controller = LocalUIViewController.current
    return remember(controller) {
        { text ->
            val sheet = UIActivityViewController(activityItems = listOf(text), applicationActivities = null)
            // iPad는 띄울 자리가 있어야 한다.
            // 자리를 주지 않으면 왼쪽 위 구석에 붙는다.
            sheet.popoverPresentationController?.sourceView = controller.view
            sheet.popoverPresentationController?.sourceRect =
                controller.view.bounds.useContents { CGRectMake(size.width / 2, size.height / 2, 0.0, 0.0) }
            controller.presentViewController(sheet, animated = true, completion = null)
        }
    }
}
