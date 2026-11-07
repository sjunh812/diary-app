package org.sjhstudio.diary.feature.lock

import androidx.compose.runtime.Composable
import org.sjhstudio.diary.core.platform.DeviceAuthenticator

/**
 * 지금 화면에서 시스템 인증 창을 띄우는 것.
 * Android는 지금 액티비티가 있어야 한다.
 */
@Composable
expect fun rememberDeviceAuthenticator(): DeviceAuthenticator
