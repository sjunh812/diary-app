package org.sjhstudio.diary.feature.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import org.sjhstudio.diary.core.data.BackupRepository
import org.sjhstudio.diary.core.data.RestoreRepository
import org.sjhstudio.diary.core.model.AccountChange
import org.sjhstudio.diary.core.model.BackupEntry
import org.sjhstudio.diary.core.model.BackupFailure
import org.sjhstudio.diary.core.model.BackupSkip
import org.sjhstudio.diary.core.model.BackupStatus
import org.sjhstudio.diary.core.model.RollbackInfo
import org.sjhstudio.diary.core.model.quietly
import org.sjhstudio.diary.core.ui.WhileUiSubscribed
import org.sjhstudio.diary.core.ui.launchSafely
import org.sjhstudio.diary.core.ui.stateInSafely

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("TooManyFunctions") // 홈 줄, 백업 화면, 백업 설정이 같은 상태를 함께 봄
class BackupViewModel(private val controller: BackupRepository, private val restore: RestoreRepository) : ViewModel() {
    /** [다시 시도]를 누를 때마다 옴 */
    private val statusRetries = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    // 화면에 돌아와 다시 읽을 때는 읽는 중으로 돌리지 않음. 줄이 사라졌다 나오지 않게
    // [다시 시도]로 읽을 때만 읽는 중을 보여 누른 것이 보이게
    private val statusRead: StateFlow<StatusRead> =
        statusRetries.map { true }.onStart { emit(false) }.flatMapLatest { retried ->
            controller.status
                .map<BackupStatus, StatusRead> { StatusRead.Loaded(it) }
                .onStart { if (retried) emit(StatusRead.Loading) }
                .catch { if (it is Exception) emit(StatusRead.Failed) else throw it }
        }.stateInSafely(viewModelScope, WhileUiSubscribed, StatusRead.Loading)

    /**
     * 저장해 둔 상태를 읽기 전이나 읽지 못하면 null.
     * 기본값을 그리면 켜 둔 백업이 잠깐 또는 계속 꺼짐으로 보여 홈 줄과 백업 화면은 이 값을 봄
     */
    val loadedStatus: StateFlow<BackupStatus?> =
        statusRead.map { (it as? StatusRead.Loaded)?.status }.stateInSafely(viewModelScope, WhileUiSubscribed, null)

    /** 상태를 읽지 못함. 홈 줄과 백업 화면이 다시 시도를 보임 */
    val statusFailed: StateFlow<Boolean> =
        statusRead.map { it == StatusRead.Failed }.stateInSafely(viewModelScope, WhileUiSubscribed, false)

    /** 백업 설정(B13)처럼 연결한 뒤에만 여는 화면이 봄 */
    val status: StateFlow<BackupStatus> =
        statusRead.map {
            (it as? StatusRead.Loaded)?.status ?: BackupStatus()
        }.stateInSafely(viewModelScope, WhileUiSubscribed, BackupStatus())

    fun retryStatus() {
        statusRetries.tryEmit(Unit)
    }

    /** 지금 백업이 올리지 않고 끝났을 때 한 번 옴 */
    val skipped: Flow<BackupSkip> = controller.skipped

    private val _connecting = MutableStateFlow(false)
    val connecting: StateFlow<Boolean> = _connecting.asStateFlow()

    /** 바꾸기 뒤 되돌릴 수 있는 동안만 값이 있음 */
    val rollback: StateFlow<RollbackInfo?> = restore.rollback.stateInSafely(
        viewModelScope,
        WhileUiSubscribed,
        null
    )

    fun rollBack() {
        viewModelScope.launchSafely { restore.rollBack() }
    }

    /** 복원한 사진을 받는 동안만 값이 있음 */
    val photoProgress = restore.photoProgress

    /** 연결한 계정의 드라이브에 있던 가장 최근 백업 */
    val found: StateFlow<BackupEntry?> = controller.foundBackup

    /** 방금 연결에 실패한 까닭 */
    val connectFailure: StateFlow<BackupFailure?> = controller.connectFailure

    fun dismissNudge() {
        viewModelScope.launchSafely { quietly { controller.dismissNudge() } }
    }

    /**
     * [hint]는 예전 앱이 쓰던 구글 계정
     * Android에서 먼저 보임. 계정 선택 창이 떠 있는 동안 다시 눌러도 한 번만 물음
     */
    fun connect(hint: String? = null) {
        if (!_connecting.compareAndSet(expect = false, update = true)) return
        viewModelScope.launchSafely {
            try {
                // 연결 실패 까닭은 [connectFailure]가 말함. 예상하지 못한 예외에도 앱을 끄지 않음
                quietly { controller.connect(hint) }
            } finally {
                _connecting.value = false
            }
        }
    }

    /**
     * [나중에]
     * 이때 이 기기의 첫 백업을 함
     */
    fun skipFound() = controller.skipFoundBackup()

    fun takeFound() = controller.takeFoundBackup()

    fun connectFailureShown() = controller.connectFailureShown()

    fun backUpNow(cellularOnce: Boolean = false) = controller.backUpNow(cellularOnce)

    fun setAuto(enabled: Boolean) {
        viewModelScope.launchSafely { quietly { controller.setAuto(enabled) } }
    }

    fun setPhotosOnCellular(enabled: Boolean) {
        viewModelScope.launchSafely { quietly { controller.setPhotosOnCellular(enabled) } }
    }

    private val _accountChanges = Channel<AccountChange>(Channel.BUFFERED)

    /**
     * 계정 변경이 끝날 때 한 번 옴. 백업 설정이 바뀌면 닫고, 백업 중이면 까닭을 알림
     * 계정 선택 창이 떠 있는 동안 화면을 돌려도 놓치지 않게 쌓아 둠
     */
    val accountChanges: Flow<AccountChange> = _accountChanges.receiveAsFlow()

    /** 백업 설정의 구글 계정 줄. 계정 선택 창이 떠 있는 동안 다시 눌러도 한 번만 물음 */
    fun changeAccount() {
        if (!_connecting.compareAndSet(expect = false, update = true)) return
        viewModelScope.launchSafely {
            try {
                _accountChanges.send(controller.changeAccount())
            } finally {
                _connecting.value = false
            }
        }
    }

    fun disconnect() {
        viewModelScope.launchSafely { quietly { controller.disconnect() } }
    }
}

private sealed interface StatusRead {
    data object Loading : StatusRead

    data class Loaded(val status: BackupStatus) : StatusRead

    data object Failed : StatusRead
}
