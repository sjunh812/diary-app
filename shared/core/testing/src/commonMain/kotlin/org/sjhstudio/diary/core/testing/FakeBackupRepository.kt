package org.sjhstudio.diary.core.testing

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.sjhstudio.diary.core.data.BackupRepository
import org.sjhstudio.diary.core.model.BackupEntry
import org.sjhstudio.diary.core.model.BackupFailure
import org.sjhstudio.diary.core.model.BackupNudge
import org.sjhstudio.diary.core.model.BackupSkip
import org.sjhstudio.diary.core.model.BackupStatus
import org.sjhstudio.diary.core.platform.ConnectResult

class FakeBackupRepository(initial: BackupStatus = BackupStatus()) : BackupRepository {
    override val status = MutableStateFlow(initial)
    override val nudge = MutableStateFlow<BackupNudge?>(null)
    override val foundBackup = MutableStateFlow<BackupEntry?>(null)
    override val skipped = MutableSharedFlow<BackupSkip>(extraBufferCapacity = 1)
    var backups = 0
        private set

    override suspend fun dismissNudge() {
        nudge.value = null
    }

    /** 다음 [connect]의 결과 */
    var connectResult: ConnectResult = ConnectResult.Cancelled

    override val connectFailure = MutableStateFlow<BackupFailure?>(null)

    override suspend fun connect(hint: String?): ConnectResult {
        connectFailure.value = (connectResult as? ConnectResult.Failed)?.kind
        return connectResult
    }

    override fun connectFailureShown() {
        connectFailure.value = null
    }

    override fun skipFoundBackup() {
        foundBackup.value = null
        backups++
    }

    override fun takeFoundBackup() {
        foundBackup.value = null
    }

    override fun backUpNow(cellularOnce: Boolean) {
        backups++
    }

    override suspend fun backUpIfDue() = Unit

    override suspend fun runIfDue() = Unit

    override suspend fun setAuto(enabled: Boolean) {
        status.value = status.value.copy(autoEnabled = enabled)
    }

    override suspend fun setPhotosOnCellular(enabled: Boolean) {
        status.value = status.value.copy(photosOnCellular = enabled)
    }

    override suspend fun disconnect() = Unit
}
