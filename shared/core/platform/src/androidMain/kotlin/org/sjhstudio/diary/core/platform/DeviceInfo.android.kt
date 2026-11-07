package org.sjhstudio.diary.core.platform

import android.os.Build

actual fun deviceInfo(): DeviceInfo = DeviceInfo("Android ${Build.VERSION.RELEASE}", Build.MODEL)
