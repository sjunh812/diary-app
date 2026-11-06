package org.sjhstudio.diary.core.testing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okio.BufferedSink
import okio.Source
import org.sjhstudio.diary.core.data.RestoreRepository
import org.sjhstudio.diary.core.model.BackupEntry
import org.sjhstudio.diary.core.model.BundleEstimate
import org.sjhstudio.diary.core.model.PhotoProgress
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

    override suspend fun bundleEstimate() = BundleEstimate(0, 0, 0)

    override suspend fun writeBundle(sink: BufferedSink, onProgress: (done: Int, total: Int) -> Unit) = Unit

    override suspend fun prepareFile(source: Source): RestorePreparation = preparation

    override suspend fun backups(): List<BackupEntry>? = entries

    override suspend fun prepare(snapshotName: String): RestorePreparation = preparation

    override fun merge() {
        merges++
    }

    var dismisses = 0
        private set

    override fun dismiss() {
        dismisses++
        state.value = RestoreState.Idle
    }

    override suspend fun resumePhotos() = Unit

    override val photoProgress: StateFlow<PhotoProgress?> = MutableStateFlow(null)
}
