package org.sjhstudio.diary.core.testing

import kotlinx.coroutines.flow.MutableStateFlow
import org.sjhstudio.diary.core.data.RestoreRepository
import org.sjhstudio.diary.core.model.BackupEntry
import org.sjhstudio.diary.core.model.RestoreFailure
import org.sjhstudio.diary.core.model.RestorePreparation
import org.sjhstudio.diary.core.model.RestoreState
import org.sjhstudio.diary.core.model.RollbackInfo

class FakeRestoreRepository(var entries: List<BackupEntry>? = emptyList()) : RestoreRepository {
    override val state = MutableStateFlow<RestoreState>(RestoreState.Idle)
    var preparation: RestorePreparation = RestorePreparation.Failed(RestoreFailure.OTHER)
    var merges = 0
        private set

    override val rollback = MutableStateFlow<RollbackInfo?>(null)
    var replaces = 0
        private set

    override fun replace() {
        replaces++
    }

    override suspend fun rollBack(): Boolean {
        rollback.value = null
        return true
    }

    override suspend fun backups(): List<BackupEntry>? = entries

    override suspend fun prepare(snapshotName: String): RestorePreparation = preparation

    override fun merge() {
        merges++
    }

    override fun dismiss() {
        state.value = RestoreState.Idle
    }

    override suspend fun resumePhotos() = Unit
}
