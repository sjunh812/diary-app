package org.sjhstudio.diary.core.backup

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.datastore.BackupStore
import org.sjhstudio.diary.core.model.BackupConnection
import org.sjhstudio.diary.core.model.BackupFailure
import org.sjhstudio.diary.core.model.BackupStatus
import org.sjhstudio.diary.core.platform.BackupScheduler
import org.sjhstudio.diary.core.platform.ConnectResult
import org.sjhstudio.diary.core.platform.DriveAuthorizer

/** 화면이 쓰는 백업 입구. 홈과 설정이 같은 상태를 본다(contracts/backup-flow.md 상태 표시). */
class BackupController(
    private val store: BackupStore,
    private val db: Lazy<DiaryDatabase>,
    private val runner: Lazy<BackupRunner>,
    private val authorizer: DriveAuthorizer,
    private val scheduler: BackupScheduler,
    private val scope: CoroutineScope
) {
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val status: Flow<BackupStatus> =
        store.prefs.flatMapLatest { prefs ->
            val waiting = prefs.accountKey?.let { db.value.backupDao().notBackedUp(it) } ?: flowOf(0)
            combine(waiting, runner.value.progress) { notBackedUp, running ->
                BackupStatus(
                    connection =
                        when {
                            prefs.accountKey == null -> BackupConnection.NotConnected
                            prefs.lastFailure == BackupFailure.AUTH -> BackupConnection.NeedsReconnect
                            else -> BackupConnection.Connected(prefs.accountLabel.orEmpty())
                        },
                    lastSuccessAt = prefs.lastSuccessAt,
                    photosNotBackedUp = notBackedUp,
                    running = running,
                    failingSince = prefs.failingSince,
                    lastFailure = prefs.lastFailure,
                    autoEnabled = prefs.autoEnabled,
                    photosOnCellular = prefs.photosOnCellular
                )
            }
        }

    /** 처음 연결하면 자동 백업을 켜고 바로 백업한다(FR-013). */
    suspend fun connect(hint: String? = null): ConnectResult {
        val result = authorizer.connect(hint)
        if (result is ConnectResult.Connected) {
            store.update {
                it.copy(
                    accountKey = result.account.key,
                    accountLabel = result.account.email,
                    autoEnabled = true,
                    userDisconnected = false,
                    lastFailure = null
                )
            }
            scheduler.scheduleDaily()
            backUpNow()
        }
        return result
    }

    fun backUpNow(cellularOnce: Boolean = false) {
        scope.launch { runner.value.run(cellularOnce) }
    }

    suspend fun setAuto(enabled: Boolean) {
        store.update { it.copy(autoEnabled = enabled) }
        if (enabled) scheduler.scheduleDaily() else scheduler.cancelDaily()
    }

    suspend fun setPhotosOnCellular(enabled: Boolean) = store.update { it.copy(photosOnCellular = enabled) }

    /** 연결을 끊어도 드라이브의 백업은 지우지 않는다(FR-004). */
    suspend fun disconnect() {
        authorizer.disconnect()
        scheduler.cancelDaily()
        store.update {
            it.copy(
                accountKey = null,
                accountLabel = null,
                rootFolderId = null,
                autoEnabled = false,
                userDisconnected = true,
                lastFailure = null,
                failingSince = null
            )
        }
    }
}
