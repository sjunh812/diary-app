package org.sjhstudio.diary.core.testing

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okio.BufferedSink
import okio.Source
import org.sjhstudio.diary.core.data.BundleFiles
import org.sjhstudio.diary.core.data.RestoreRepository
import org.sjhstudio.diary.core.model.BackupEntry
import org.sjhstudio.diary.core.model.BundleEstimate
import org.sjhstudio.diary.core.model.PhotoProgress
import org.sjhstudio.diary.core.model.PhotoRun
import org.sjhstudio.diary.core.model.RestoreFailure
import org.sjhstudio.diary.core.model.RestorePreparation
import org.sjhstudio.diary.core.model.RestoreState
import org.sjhstudio.diary.core.model.RollbackInfo
import org.sjhstudio.diary.core.model.RollbackRun
import org.sjhstudio.diary.core.model.WaitingPhotos

class FakeBundleFiles : BundleFiles {
    override suspend fun bundleEstimate() = BundleEstimate(0, 0, 0)

    /** 값을 넣으면 파일을 쓰다 실패한다. */
    var writeFailure: Exception? = null

    /** 참이면 멈추라고 할 때까지 파일을 쓰는 중에 머문다. */
    var holdWrite = false
    var writing = false
        private set

    /** 파일에 쓸 바이트 수. */
    var bundleBytes = 0

    override suspend fun writeBundle(sink: BufferedSink, onProgress: (done: Int, total: Int) -> Unit) {
        sink.write(ByteArray(bundleBytes))
        writeFailure?.let { throw it }
        if (holdWrite) {
            writing = true
            awaitCancellation()
        }
    }

    var stoppedBundle = false

    override suspend fun takeStoppedBundle(): Boolean = stoppedBundle.also { stoppedBundle = false }

    var bundleForgets = 0
        private set

    override fun forgetStoppedBundle() {
        bundleForgets++
    }
}

@Suppress("TooManyFunctions") // 저장소 약속을 그대로 흉내 내는 시험 대역이다.
class FakeRestoreRepository(var entries: List<BackupEntry>? = emptyList(), val bundle: FakeBundleFiles = FakeBundleFiles()) :
    RestoreRepository,
    BundleFiles by bundle {
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

    var retries = 0
        private set

    override fun retry() {
        retries++
    }

    override suspend fun rollBack(): Boolean {
        rollback.value = null
        return true
    }

    override val rollbackRun = MutableStateFlow(RollbackRun.IDLE)

    override val folderRecreated = MutableStateFlow(false)

    override fun dismissFolderRecreated() {
        folderRecreated.value = false
    }

    var preparedFileSize: Long? = null
        private set

    override suspend fun prepareFile(source: Source, byteSize: Long?): RestorePreparation {
        preparedFileSize = byteSize
        return preparation
    }

    /** 지난번에 읽어 둔 목록. */
    var saved: List<BackupEntry> = emptyList()

    /** 있으면 드라이브 목록 답을 이것이 끝날 때까지 미룬다. */
    var backupsGate: CompletableDeferred<Unit>? = null

    override suspend fun savedBackups(): List<BackupEntry> = saved

    override suspend fun backups(): List<BackupEntry>? {
        backupsGate?.await()
        return entries
    }

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

    override suspend fun resumePhotos() = false

    var fetchesNow = 0
        private set

    override fun fetchPhotosNow() {
        fetchesNow++
    }

    override val waitingPhotos = MutableStateFlow<WaitingPhotos?>(null)

    override val photoProgress: StateFlow<PhotoProgress?> = MutableStateFlow(null)
    override val lastPhotoRun: PhotoRun? = null
}
