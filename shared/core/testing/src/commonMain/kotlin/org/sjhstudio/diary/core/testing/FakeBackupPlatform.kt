package org.sjhstudio.diary.core.testing

import kotlin.time.Instant
import org.sjhstudio.diary.core.platform.BackupNotifier
import org.sjhstudio.diary.core.platform.BackupScheduler
import org.sjhstudio.diary.core.platform.NetworkState

class FakeNetworkState(override var isConnected: Boolean = true, override var isUnmetered: Boolean = true) : NetworkState

class FakeBackupNotifier : BackupNotifier {
    /** 바로 띄운 알림의 본문 */
    val shown = mutableListOf<String>()

    /** 값을 넣으면 알림을 띄우다 실패함 */
    var showFailure: Exception? = null

    override fun show(title: String, body: String) {
        showFailure?.let { throw it }
        shown += body
    }

    var at: Instant? = null
        private set
    var body: String? = null
        private set
    var cancels = 0
        private set

    override suspend fun schedule(at: Instant, title: String, body: String) {
        this.at = at
        this.body = body
    }

    override fun cancel() {
        at = null
        body = null
        cancels++
    }
}

class FakeBackupScheduler : BackupScheduler {
    var restoreKept = 0
        private set

    override fun keepRestoreRunning() {
        restoreKept++
    }

    /** 복원 사진을 기다리는 작업을 맡긴 기록. 값은 Wi-Fi에서만 받는지 */
    val photoWaits = mutableListOf<Boolean>()

    override fun waitForPhotoNetwork(wifiOnly: Boolean) {
        photoWaits += wifiOnly
    }

    var wifiPhotos = 0
        private set

    override fun scheduleWifiPhotos() {
        wifiPhotos++
    }

    var daily = false
        private set
    var startedNow = 0
        private set

    /** 마지막 지금 백업이 이번만 모바일 데이터를 골랐는지 */
    var lastCellular = false
        private set

    override fun scheduleDaily() {
        daily = true
    }

    override fun cancelDaily() {
        daily = false
    }

    /** true면 시스템 작업이 받은 것으로 침 */
    var takesNow = false

    override fun startNow(cellularOnce: Boolean): Boolean {
        startedNow++
        lastCellular = cellularOnce
        return takesNow
    }
}
