package org.sjhstudio.diary.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

// mailto를 물어보려면 Info.plist의 LSApplicationQueriesSchemes에 있어야 함
@Composable
actual fun rememberOpenLink(): (String) -> Boolean = remember {
    { url ->
        val link = NSURL.URLWithString(url)
        val app = UIApplication.sharedApplication
        if (link == null || !app.canOpenURL(link)) {
            false
        } else {
            app.openURL(link, emptyMap<Any?, Any>(), null)
            true
        }
    }
}
