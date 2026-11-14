package org.sjhstudio.diary.feature.lock

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import org.sjhstudio.diary.core.platform.AndroidDeviceAuthenticator
import org.sjhstudio.diary.core.platform.DeviceAuthenticator
import org.sjhstudio.diary.core.platform.NoDeviceAuthenticator

// 생체 인증 창(BiometricPrompt)은 FragmentActivity 위에만 뜸
// 미리보기처럼 액티비티가 없으면 창을 띄우지 않음
@Composable
actual fun rememberDeviceAuthenticator(): DeviceAuthenticator {
    val context = LocalContext.current
    return remember(context) { context.fragmentActivity()?.let(::AndroidDeviceAuthenticator) ?: NoDeviceAuthenticator }
}

private fun Context.fragmentActivity(): FragmentActivity? =
    generateSequence(this) { (it as? ContextWrapper)?.baseContext }.filterIsInstance<FragmentActivity>().firstOrNull()
