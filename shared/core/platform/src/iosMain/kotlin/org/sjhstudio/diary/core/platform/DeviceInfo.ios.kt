@file:OptIn(ExperimentalForeignApi::class)

package org.sjhstudio.diary.core.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import platform.UIKit.UIDevice
import platform.posix.uname
import platform.posix.utsname

/** `UIDevice.model`은 "iPhone"까지만 알려 줘 기종 이름은 시스템에서 읽음 */
actual fun deviceInfo(): DeviceInfo {
    val device = UIDevice.currentDevice
    val machine = memScoped {
        val name = alloc<utsname>()
        if (uname(name.ptr) == 0) name.machine.toKString() else null
    }
    return DeviceInfo("${device.systemName} ${device.systemVersion}", machine ?: device.model)
}
