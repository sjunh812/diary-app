package org.sjhstudio.diary.core.testing

import kotlin.time.Instant
import org.sjhstudio.diary.core.platform.BackupNotifier
import org.sjhstudio.diary.core.platform.BackupScheduler
import org.sjhstudio.diary.core.platform.NetworkState

class FakeNetworkState(override var isConnected: Boolean = true, override var isUnmetered: Boolean = true) : NetworkState

class FakeBackupNotifier : BackupNotifier {
    var at: Instant? = null
        private set
    var body: String? = null
        private set
    var cancels = 0
        private set

    override fun schedule(at: Instant, title: String, body: String) {
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

    var wifiPhotos = 0
        private set

    override fun scheduleWifiPhotos() {
        wifiPhotos++
    }

    var daily = false
        private set
    var startedNow = 0
        private set

    override fun scheduleDaily() {
        daily = true
    }

    override fun cancelDaily() {
        daily = false
    }

    /** true면 시스템 작업이 받은 것처럼 군다. */
    var takesNow = false

    override fun startNow(cellularOnce: Boolean): Boolean {
        startedNow++
        return takesNow
    }
}
