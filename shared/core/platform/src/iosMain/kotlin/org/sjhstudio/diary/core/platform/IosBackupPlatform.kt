package org.sjhstudio.diary.core.platform

import kotlin.coroutines.resume
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import org.sjhstudio.diary.core.model.BackupFailure
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSystemFreeSize
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSNumber
import platform.Network.nw_path_get_status
import platform.Network.nw_path_is_expensive
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.UIKit.UIDevice
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.dispatch_get_global_queue

/**
 * Swift의 GoogleSignIn을 부르는 다리.
 * iosApp이 앱을 켤 때 건다(research R2).
 * 값이 없으면 빈 문자열을 넘긴다.
 */
interface IosDriveBridge {
    /**
     * 끝나면 (계정 고유 ID, 이메일, 실패) 중 하나를 넘긴다.
     * 실패는 "cancelled", "offline", "failed"다.
     */
    fun connect(hint: String?, done: (key: String?, email: String?, error: String?) -> Unit)

    /**
     * 끝나면 (토큰, 계정 고유 ID, 이메일, 실패)를 넘긴다.
     * 실패는 "needsUser", "offline", "failed"다.
     */
    fun accessToken(done: (token: String?, key: String?, email: String?, error: String?) -> Unit)

    fun disconnect(done: () -> Unit)
}

class IosDriveAuthorizer(private val bridge: () -> IosDriveBridge?) : DriveAuthorizer {
    private val tokens = DriveTokenCache(fetch = ::fetchToken)

    override suspend fun connect(hint: String?): ConnectResult {
        tokens.clear()
        val bridge = bridge() ?: return ConnectResult.Failed(BackupFailure.UNKNOWN)
        val result = suspendCancellableCoroutine { continuation ->
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
        // 연결하는 사이에 옛 계정으로 받은 토큰이 남아 있을 수 있다.
        tokens.clear()
        return result
    }

    override suspend fun accessToken(): TokenResult = tokens.get()

    private suspend fun fetchToken(): TokenResult {
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

    override fun invalidateToken() = tokens.clear()

    override suspend fun disconnect() {
        tokens.clear()
        val bridge = bridge() ?: return
        suspendCancellableCoroutine { continuation -> bridge.disconnect { continuation.resume(Unit) } }
    }
}

/**
 * 경로 감시로 지금 연결을 안다.
 * 데이터 요금이 있으면 `expensive`다.
 */
class IosNetworkState : NetworkState {
    override var isConnected: Boolean = true
        private set
    override var isUnmetered: Boolean = true
        private set

    private val monitor = nw_path_monitor_create()

    init {
        nw_path_monitor_set_update_handler(monitor) { path ->
            isConnected = nw_path_get_status(path) == nw_path_status_satisfied
            isUnmetered = !nw_path_is_expensive(path)
        }
        nw_path_monitor_set_queue(monitor, dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u))
        nw_path_monitor_start(monitor)
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

class IosBackupNotifier(private val clock: Clock = Clock.System) : BackupNotifier {
    private val center get() = UNUserNotificationCenter.currentNotificationCenter()

    override suspend fun schedule(at: Instant, title: String, body: String) {
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(body)
        }
        val seconds = (at - clock.now()).inWholeSeconds.coerceAtLeast(1).toDouble()
        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(seconds, repeats = false)
        center.addNotificationRequest(UNNotificationRequest.requestWithIdentifier(ID, content, trigger), null)
    }

    override fun show(title: String, body: String) {
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(body)
        }
        center.addNotificationRequest(UNNotificationRequest.requestWithIdentifier(NOTICE_ID, content, null), null)
    }

    override fun cancel() {
        center.removePendingNotificationRequestsWithIdentifiers(listOf(ID))
        center.removeDeliveredNotificationsWithIdentifiers(listOf(ID))
    }

    private companion object {
        const val ID = "backup.long-failure"
        const val NOTICE_ID = "backup.notice"
    }
}
