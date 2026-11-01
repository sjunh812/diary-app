package org.sjhstudio.diary.feature.lock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.sjhstudio.diary.core.platform.DeviceAuthenticator
import org.sjhstudio.diary.core.platform.IosDeviceAuthenticator

@Composable
internal actual fun rememberDeviceAuthenticator(): DeviceAuthenticator = remember { IosDeviceAuthenticator() }
