package org.sjhstudio.diary.core.platform

import kotlin.coroutines.resume
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import org.sjhstudio.diary.core.model.BackupFailure
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSystemFreeSize
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSNumber
import platform.UIKit.UIDevice
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNUserNotificationCenter

/** Swift의 GoogleSignIn을 부르는 다리. iosApp이 앱을 켤 때 건다(research R2). 값이 없으면 빈 문자열을 넘긴다. */
interface IosDriveBridge {
    /** 끝나면 (계정 고유 ID, 이메일, 실패) 중 하나를 넘긴다. 실패는 "cancelled", "offline", "failed"다. */
    fun connect(hint: String?, done: (key: String?, email: String?, error: String?) -> Unit)

    /** 끝나면 (토큰, 계정 고유 ID, 이메일, 실패)를 넘긴다. 실패는 "needsUser", "offline", "failed"다. */
    fun accessToken(done: (token: String?, key: String?, email: String?, error: String?) -> Unit)

    fun disconnect(done: () -> Unit)
}

class IosDriveAuthorizer(private val bridge: () -> IosDriveBridge?) : DriveAuthorizer {
    override suspend fun connect(hint: String?): ConnectResult {
        val bridge = bridge() ?: return ConnectResult.Failed(BackupFailure.UNKNOWN)
        return suspendCancellableCoroutine { continuation ->
            bridge.connect(hint) { key, email, error ->
                continuation.resume(
                    when {
                        key != null && email != null -> ConnectResult.Connected(DriveAccount(key, email))
                        error == "cancelled" -> ConnectResult.Cancelled
                        error == "offline" -> ConnectResult.Failed(BackupFailure.OFFLINE)
                        else -> ConnectResult.Failed(BackupFailure.AUTH)
                    }
                )
            }
        }
    }

    override suspend fun accessToken(): TokenResult {
        val bridge = bridge() ?: return TokenResult.NeedsUser
        return suspendCancellableCoroutine { continuation ->
            bridge.accessToken { token, key, email, error ->
                continuation.resume(
                    when {
                        token != null && key != null && email != null -> TokenResult.Token(token, DriveAccount(key, email))
                        error == "offline" -> TokenResult.Failed(BackupFailure.OFFLINE)
                        error == "needsUser" -> TokenResult.NeedsUser
                        else -> TokenResult.Failed(BackupFailure.AUTH)
                    }
                )
            }
        }
    }

    override suspend fun disconnect() {
        val bridge = bridge() ?: return
        suspendCancellableCoroutine { continuation -> bridge.disconnect { continuation.resume(Unit) } }
    }
}

class IosDeviceName : DeviceName {
    override fun current(): String = UIDevice.currentDevice.model
}

@OptIn(ExperimentalForeignApi::class)
class IosFreeSpace : FreeSpace {
    override fun bytes(): Long {
        val attributes = NSFileManager.defaultManager.attributesOfFileSystemForPath(NSHomeDirectory(), error = null)
        return (attributes?.get(NSFileSystemFreeSize) as? NSNumber)?.longLongValue ?: 0
    }
}

class IosBackupNotifier : BackupNotifier {
    override fun notifyLongFailure(title: String, body: String) {
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(body)
        }
        UNUserNotificationCenter.currentNotificationCenter().addNotificationRequest(
            UNNotificationRequest.requestWithIdentifier(ID, content, null),
            null
        )
    }

    override fun clear() {
        UNUserNotificationCenter.currentNotificationCenter().removeDeliveredNotificationsWithIdentifiers(listOf(ID))
    }

    private companion object {
        const val ID = "backup.long-failure"
    }
}
