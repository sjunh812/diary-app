package org.sjhstudio.diary.feature.lock

import androidx.compose.runtime.Composable
import org.sjhstudio.diary.core.platform.DeviceAuthenticator

/**
 * 지금 화면에 시스템 인증 창 띄우기
 * Android는 지금 액티비티가 있어야 함
 */
@Composable
expect fun rememberDeviceAuthenticator(): DeviceAuthenticator
