package org.sjhstudio.diary.core.testing

import kotlinx.coroutines.flow.MutableStateFlow
import org.sjhstudio.diary.core.data.BackupRepository
import org.sjhstudio.diary.core.model.BackupStatus
import org.sjhstudio.diary.core.platform.ConnectResult

class FakeBackupRepository(initial: BackupStatus = BackupStatus()) : BackupRepository {
    override val status = MutableStateFlow(initial)
    var backups = 0
        private set

    override suspend fun connect(hint: String?): ConnectResult = ConnectResult.Cancelled

    override fun backUpNow(cellularOnce: Boolean) {
        backups++
    }

    override suspend fun backUpIfDue() = Unit

    override suspend fun setAuto(enabled: Boolean) {
        status.value = status.value.copy(autoEnabled = enabled)
    }

    override suspend fun setPhotosOnCellular(enabled: Boolean) {
        status.value = status.value.copy(photosOnCellular = enabled)
    }

    override suspend fun disconnect() = Unit
}
