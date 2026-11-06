package org.sjhstudio.diary.core.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import okio.BufferedSink
import okio.Source
import org.sjhstudio.diary.core.model.BackupEntry
import org.sjhstudio.diary.core.model.BundleEstimate
import org.sjhstudio.diary.core.model.PhotoProgress
import org.sjhstudio.diary.core.model.PhotoRun
import org.sjhstudio.diary.core.model.RESTORE_FROM_FILE
import org.sjhstudio.diary.core.model.RestorePreparation
import org.sjhstudio.diary.core.model.RestoreState
import org.sjhstudio.diary.core.model.RollbackInfo

/**
 * 화면이 쓰는 복원하기 입구.
 * 일기 내용은 여기를 지나지 않고 숫자만 지나간다.
 */
interface RestoreRepository {
    val state: StateFlow<RestoreState>

    /**
     * 드라이브의 백업 목록.
     * 읽지 못하면 null이다.
     */
    suspend fun backups(): List<BackupEntry>?

    /** 스냅샷을 받아 확인하고 쓰기 없이 결과 숫자를 낸다(FR-021, FR-023). */
    suspend fun prepare(snapshotName: String): RestorePreparation

    /**
     * 확인한 백업을 합치고 사진을 받는다.
     * 지금 기기의 일기는 지우지 않는다(FR-022).
     */
    fun merge()

    /**
     * 지금 일기를 이 백업으로 바꾼다.
     * 사본을 만들어 7일 동안 되돌릴 수 있다(FR-027).
     */
    fun replace()

    /** 실패한 복원을 같은 방식으로 다시 한다. */
    fun retry()

    /** 되돌릴 수 있는 동안만 값이 있다. */
    val rollback: Flow<RollbackInfo?>

    /**
     * 바꾸기 전으로 돌아간다.
     * 되돌리지 못하면 false다.
     */
    suspend fun rollBack(): Boolean

    /** 파일로 만들면 담길 수와 어림 크기. */
    suspend fun bundleEstimate(): BundleEstimate

    /** 일기와 사진을 zip 하나로 [sink]에 스트리밍으로 쓴다(FR-031). */
    suspend fun writeBundle(sink: BufferedSink, onProgress: (done: Int, total: Int) -> Unit)

    /**
     * 사용자가 고른 파일을 앱 안으로 옮겨 확인한다.
     * 미리보기는 [RESTORE_FROM_FILE]로 다시 읽는다.
     * [byteSize]를 알면 옮기기 전에 공간이 되는지 먼저 본다.
     */
    suspend fun prepareFile(source: Source, byteSize: Long? = null): RestorePreparation

    /** 결과 화면을 닫을 때 부른다. */
    fun dismiss()

    /**
     * 앱을 열 때 부른다.
     * 전에 다 못 받은 사진을 이어 받는다(FR-024).
     */
    suspend fun resumePhotos()

    /**
     * 사진을 받는 동안의 진행.
     * 받고 있지 않으면 null이다.
     */
    val photoProgress: StateFlow<PhotoProgress?>

    /**
     * 마지막으로 끝난 사진 받기의 결과.
     * 흐름은 빠른 값을 건너뛰므로 끝 값은 여기서 읽는다.
     */
    val lastPhotoRun: PhotoRun?
}
