package org.sjhstudio.diary.core.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri

@Composable
actual fun rememberOpenLink(): (String) -> Boolean {
    val context = LocalContext.current
    return remember(context) {
        { url ->
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                true
            } catch (_: ActivityNotFoundException) {
                false
            }
        }
    }
}
