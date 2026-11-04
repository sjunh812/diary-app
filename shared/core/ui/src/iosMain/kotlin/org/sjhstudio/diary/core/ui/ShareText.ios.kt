package org.sjhstudio.diary.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.uikit.LocalUIViewController
import platform.UIKit.UIActivityViewController
import platform.UIKit.popoverPresentationController

@Composable
actual fun rememberShareText(): (String) -> Unit {
    val controller = LocalUIViewController.current
    return remember(controller) {
        { text ->
            val sheet = UIActivityViewController(activityItems = listOf(text), applicationActivities = null)
            // iPad는 띄울 자리가 있어야 한다.
            sheet.popoverPresentationController?.sourceView = controller.view
            controller.presentViewController(sheet, animated = true, completion = null)
        }
    }
}
