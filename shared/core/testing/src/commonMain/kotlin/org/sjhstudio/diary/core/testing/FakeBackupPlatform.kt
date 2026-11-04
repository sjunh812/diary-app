package org.sjhstudio.diary.core.testing

import org.sjhstudio.diary.core.platform.BackupNotifier
import org.sjhstudio.diary.core.platform.BackupScheduler
import org.sjhstudio.diary.core.platform.NetworkState

class FakeNetworkState(override var isConnected: Boolean = true, override var isUnmetered: Boolean = true) : NetworkState

class FakeBackupNotifier : BackupNotifier {
    val sent = mutableListOf<Pair<String, String>>()
    var cleared = 0
        private set

    override fun notifyLongFailure(title: String, body: String) {
        sent += title to body
    }

    override fun clear() {
        cleared++
    }
}

class FakeBackupScheduler : BackupScheduler {
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

    override fun startNow() {
        startedNow++
    }
}
