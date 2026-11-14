package org.sjhstudio.diary.core.data

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okio.Buffer
import okio.BufferedSink
import okio.Sink
import okio.Source
import okio.buffer
import okio.use
import org.sjhstudio.diary.core.model.BackupEntry
import org.sjhstudio.diary.core.model.BundleEstimate
import org.sjhstudio.diary.core.model.PhotoProgress
import org.sjhstudio.diary.core.model.PhotoRun
import org.sjhstudio.diary.core.model.RESTORE_FROM_FILE
import org.sjhstudio.diary.core.model.RestoreFailure
import org.sjhstudio.diary.core.model.RestorePreparation
import org.sjhstudio.diary.core.model.RestoreState
import org.sjhstudio.diary.core.model.RollbackInfo
import org.sjhstudio.diary.core.model.RollbackRun
import org.sjhstudio.diary.core.model.WaitingPhotos
import org.sjhstudio.diary.core.model.orElseOnFailure

/** 묶음 파일 만들기 */
interface BundleFiles {
    /** 파일로 만들면 담길 수와 어림 크기 */
    suspend fun bundleEstimate(): BundleEstimate

    /**
     * 일기와 사진을 zip 하나로 [open]에 스트리밍으로 씀(FR-031)
     * 닫기까지 끝나야 다 쓴 것으로 봄
     */
    suspend fun writeBundle(open: () -> Sink, onProgress: (done: Int, total: Int) -> Unit)

    /**
     * 앱이 꺼져 끝나지 못한 묶음 파일 만들기가 있으면 한 번 true
     * 지금 만드는 중이면 false
     */
    suspend fun takeStoppedBundle(): Boolean

    /** 사용자가 파일 만들기를 멈춤. 다음에 알릴 일이 아님 */
    fun forgetStoppedBundle()

    /**
     * 사용자가 고른 자리에 묶음 파일을 만듦
     * [open]으로 열고, 다 쓰면 닫음
     * 끝까지 쓰지 못하면 [discard]로 지움
     * 쓰다 만 파일이 완성본 이름으로 남으면 나중에 그 파일로 복원을 시도하게 됨
     * 멈추라고 해서 끝나도 지우고, 멈춤을 그대로 알림
     */
    suspend fun saveBundle(open: () -> Sink, discard: suspend () -> Unit, onProgress: (done: Int, total: Int) -> Unit): BundleSave =
        withContext(Dispatchers.IO) {
            try {
                var counted: CountingSink? = null
                writeBundle({ CountingSink(open()).also { counted = it } }, onProgress)
                BundleSave.Saved(checkNotNull(counted).written)
            } catch (error: CancellationException) {
                withContext(NonCancellable) { removed(discard) }
                throw error
            } catch (_: Exception) {
                BundleSave.Failed(left = !removed(discard))
            }
        }
}

/** 파일 만들기의 끝 */
sealed interface BundleSave {
    /** [bytes]는 어림값이 아니라 실제로 쓴 크기 */
    data class Saved(val bytes: Long) : BundleSave

    /** [left]면 쓰다 만 파일을 지우지 못함. 사용자가 지워야 함 */
    data class Failed(val left: Boolean) : BundleSave
}

private suspend fun removed(discard: suspend () -> Unit): Boolean = try {
    discard()
    true
} catch (error: CancellationException) {
    throw error
} catch (_: Exception) {
    false
}

/** 실제로 쓴 크기 계산 */
private class CountingSink(private val delegate: Sink) : Sink by delegate {
    var written = 0L
        private set

    override fun write(source: Buffer, byteCount: Long) {
        delegate.write(source, byteCount)
        written += byteCount
    }
}

/**
 * 화면이 쓰는 복원하기 입구
 * 일기 내용은 여기를 지나지 않고 숫자만 지나감
 */
interface RestoreRepository :
    BundleFiles,
    SavedBackups {
    val state: StateFlow<RestoreState>

    /**
     * 드라이브의 백업 목록
     * 읽지 못하면 null
     */
    suspend fun backups(): List<BackupEntry>?

    /** 스냅샷을 받아 확인하고 쓰기 없이 결과 숫자를 냄(FR-021, FR-023) */
    suspend fun prepare(snapshotName: String): RestorePreparation

    /**
     * 확인한 백업을 합치고 사진을 받음
     * 지금 기기의 일기는 지우지 않음(FR-022)
     */
    fun merge()

    /**
     * 지금 일기를 이 백업으로 바꿈
     * 사본을 만들어 7일 동안 되돌릴 수 있음(FR-027)
     */
    fun replace()

    /** 실패한 복원을 같은 방식으로 다시 시도 */
    fun retry()

    /** 되돌릴 수 있는 동안만 값이 있음 */
    val rollback: Flow<RollbackInfo?>

    /**
     * 바꾸기 전으로 돌아감
     * 복원이나 백업이 돌고 있으면 끝날 때까지 기다림
     * 되돌리지 못하면 false
     */
    suspend fun rollBack(): Boolean

    /** 되돌리기 진행. 화면을 떠났다 돌아와도 결과가 보임 */
    val rollbackRun: StateFlow<RollbackRun>

    /**
     * 드라이브에서 지워진 백업 폴더를 새로 만듦
     * 알림을 끈 사람도 볼 수 있게 화면에서도 알림
     */
    val folderRecreated: Flow<Boolean>

    fun dismissFolderRecreated()

    /**
     * 사용자가 고른 파일을 앱 안으로 옮겨 확인
     * 미리보기는 [RESTORE_FROM_FILE]로 다시 읽음
     * [byteSize]를 알면 옮기기 전에 공간이 되는지 먼저 봄
     */
    suspend fun prepareFile(source: Source, byteSize: Long? = null): RestorePreparation

    /** 결과 화면을 닫을 때 호출 */
    fun dismiss()

    /**
     * 앱을 열 때와 네트워크가 돌아올 때 호출
     * 전에 다 못 받은 사진을 이어 받음(FR-024)
     * 지금은 받을 수 없어 나중에 다시 불러야 하면 true
     */
    suspend fun resumePhotos(): Boolean

    /** 사용자가 누른 지금 받기. 모바일 데이터여도 이번만 받음 */
    fun fetchPhotosNow()

    /**
     * 받다가 멈췄거나 받는 중인 사진
     * 받을 수 있는 사진이 없으면 null
     * 백업에 없어 받을 수 없는 사진은 세지 않음
     */
    val waitingPhotos: Flow<WaitingPhotos?>

    /**
     * 사진을 받는 동안의 진행
     * 받고 있지 않으면 null
     */
    val photoProgress: StateFlow<PhotoProgress?>

    /**
     * 마지막으로 끝난 사진 받기의 결과
     * 흐름은 빠른 값을 건너뛰므로 끝 값은 여기서 읽음
     */
    val lastPhotoRun: PhotoRun?
}

/** 지난번에 읽어 둔 백업 목록. 드라이브에 묻기 전에 먼저 보여 줌 */
interface SavedBackups {
    /** 없거나 다른 계정의 것이면 빈 목록 */
    suspend fun savedBackups(): List<BackupEntry> = emptyList()
}

/**
 * 고른 파일을 [open]으로 열어 [prepareFile]에 넘기고 닫음
 * 크기를 묻고 여는 일은 입출력 스레드에서 함
 * 열지 못하면 고른 파일을 받지 못한 것으로 알림
 */
suspend fun RestoreRepository.prepareFile(open: () -> Source, byteSize: () -> Long?): RestorePreparation = withContext(Dispatchers.IO) {
    val source =
        try {
            open()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            return@withContext RestorePreparation.Failed(RestoreFailure.OTHER)
        }
    try {
        source.use { prepareFile(it, orElseOnFailure(null) { byteSize() }) }
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        RestorePreparation.Failed(RestoreFailure.MISMATCH)
    }
}
