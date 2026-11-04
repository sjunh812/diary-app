package org.sjhstudio.diary.core.backup

import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.sjhstudio.diary.core.data.BackupRepository
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.datastore.BackupStore
import org.sjhstudio.diary.core.model.BackupConnection
import org.sjhstudio.diary.core.model.BackupFailure
import org.sjhstudio.diary.core.model.BackupStatus
import org.sjhstudio.diary.core.platform.BackupScheduler
import org.sjhstudio.diary.core.platform.ConnectResult
import org.sjhstudio.diary.core.platform.DriveAuthorizer

/** [BackupRepository]의 구현. 드라이브와 일기 DB는 처음 쓸 때 연다. */
class BackupController(
    private val store: BackupStore,
    private val db: Lazy<DiaryDatabase>,
    private val runner: Lazy<BackupRunner>,
    private val authorizer: DriveAuthorizer,
    private val scheduler: BackupScheduler,
    private val scope: CoroutineScope,
    private val clock: Clock = Clock.System
) : BackupRepository {
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override val status: Flow<BackupStatus> =
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

    override suspend fun connect(hint: String?): ConnectResult {
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

    override fun backUpNow(cellularOnce: Boolean) {
        scope.launch { runner.value.run(cellularOnce) }
    }

    override suspend fun backUpIfDue() {
        val prefs = store.current()
        val last = prefs.lastSuccessAt
        if (prefs.accountKey == null || !prefs.autoEnabled || prefs.lastFailure == BackupFailure.AUTH) return
        if (last == null || clock.now() - last >= 24.hours) backUpNow()
    }

    override suspend fun setAuto(enabled: Boolean) {
        store.update { it.copy(autoEnabled = enabled) }
        if (enabled) scheduler.scheduleDaily() else scheduler.cancelDaily()
    }

    override suspend fun setPhotosOnCellular(enabled: Boolean) = store.update { it.copy(photosOnCellular = enabled) }

    override suspend fun disconnect() {
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
