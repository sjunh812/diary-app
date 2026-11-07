package org.sjhstudio.diary.core.backup

import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.sjhstudio.diary.core.data.BackupRepository
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.datastore.BackupPrefs
import org.sjhstudio.diary.core.datastore.BackupStore
import org.sjhstudio.diary.core.model.BackupConnection
import org.sjhstudio.diary.core.model.BackupEntry
import org.sjhstudio.diary.core.model.BackupEntryState
import org.sjhstudio.diary.core.model.BackupFailure
import org.sjhstudio.diary.core.model.BackupNudge
import org.sjhstudio.diary.core.model.BackupSkip
import org.sjhstudio.diary.core.model.BackupStatus
import org.sjhstudio.diary.core.model.PLATFORM_LEGACY
import org.sjhstudio.diary.core.platform.BackupNotifier
import org.sjhstudio.diary.core.platform.BackupScheduler
import org.sjhstudio.diary.core.platform.ConnectResult
import org.sjhstudio.diary.core.platform.DriveAuthorizer

/**
 * 연결한 드라이브에서 권할 백업.
 * 새 백업을 먼저 고르고, 없으면 예전 앱이 올린 백업을 고른다.
 * 예전 백업은 편수를 몰라 0편으로 온다.
 * [migrated]면 이 폰에서 이미 옮겨 온 일기와 같은 백업이라 권하지 않는다.
 */
fun backupToOffer(backups: List<BackupEntry>?, migrated: Boolean): BackupEntry? {
    val ready = backups.orEmpty().filter { it.state == BackupEntryState.Ready }
    return ready.firstOrNull { it.entryCount > 0 } ?: ready.firstOrNull { it.platform == PLATFORM_LEGACY && !migrated }
}

/**
 * [BackupRepository]의 구현.
 * 드라이브와 일기 DB는 처음 쓸 때 연다.
 */
class BackupController(
    private val store: BackupStore,
    private val db: Lazy<DiaryDatabase>,
    private val runner: Lazy<BackupRunner>,
    private val authorizer: DriveAuthorizer,
    private val scheduler: BackupScheduler,
    private val notifier: BackupNotifier,
    private val scope: CoroutineScope,
    private val clock: Clock = Clock.System,
    private val migrated: () -> Boolean = { false },
    private val legacyAccount: suspend () -> String? = { null },
    /**
     * 드라이브에 있는 가장 최근 백업.
     * 못 읽으면 null이다.
     */
    private val findBackup: suspend () -> BackupEntry? = { null },
    /** 7일 실패 알림을 다시 걸 때 쓴다. */
    private val alerts: BackupAlerts? = null,
    /** 지금 백업이나 자동 백업이 올리고 있는가. */
    private val running: () -> Boolean = { runner.value.progress.value != null }
) : BackupRepository {
    private val _foundBackup = MutableStateFlow<BackupEntry?>(null)
    override val foundBackup: StateFlow<BackupEntry?> = _foundBackup.asStateFlow()

    /** 연결한 뒤 드라이브를 찾는 동안 켜진다. 몇 초 걸려서 화면이 알린다. */
    private val finding = MutableStateFlow(false)

    private val _connectFailure = MutableStateFlow<BackupFailure?>(null)
    override val connectFailure: StateFlow<BackupFailure?> = _connectFailure.asStateFlow()

    private val _skipped = MutableSharedFlow<BackupSkip>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val skipped: SharedFlow<BackupSkip> = _skipped.asSharedFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override val status: Flow<BackupStatus> =
        store.prefs.flatMapLatest { prefs ->
            val waiting = prefs.accountKey?.let { db.value.backupDao().notBackedUp(it) } ?: flowOf(0)
            combine(waiting, runner.value.progress, finding) { notBackedUp, running, findingBackup ->
                BackupStatus(
                    connection =
                        when {
                            prefs.accountKey == null -> BackupConnection.NotConnected
                            prefs.lastFailure == BackupFailure.AUTH -> BackupConnection.NeedsReconnect
                            else -> BackupConnection.Connected(prefs.accountLabel.orEmpty())
                        },
                    lastSuccessAt = prefs.lastSuccessAt,
                    photosNotBackedUp = (notBackedUp - prefs.unusablePhotos.size).coerceAtLeast(0),
                    photosUnusable = prefs.unusablePhotos.size,
                    running = running,
                    failingSince = prefs.failingSince,
                    lastFailure = prefs.lastFailure,
                    autoEnabled = prefs.autoEnabled,
                    photosOnCellular = prefs.photosOnCellular,
                    bytesShort = prefs.bytesShort,
                    restoredBackupAt = prefs.restoredBackupAt,
                    findingBackup = findingBackup
                )
            }
        }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override val nudge: Flow<BackupNudge?> =
        store.prefs.flatMapLatest { prefs ->
            if (!mayNudge(prefs)) {
                flowOf(null)
            } else {
                val dao = db.value.backupDao()
                combine(dao.visibleEntries(), dao.visiblePhotos()) { entries, photos ->
                    if (migrated() || entries >= NUDGE_ENTRIES || photos > 0) BackupNudge(entries, photos, legacyAccount()) else null
                }
            }
        }

    private fun mayNudge(prefs: BackupPrefs): Boolean {
        val hidden = prefs.nudgeHiddenUntil?.let { it > clock.now() } == true
        return prefs.accountKey == null && !prefs.userDisconnected && prefs.nudgeDismissCount < NUDGE_LIMIT && !hidden
    }

    override suspend fun dismissNudge() {
        store.update { it.copy(nudgeDismissCount = it.nudgeDismissCount + 1, nudgeHiddenUntil = clock.now() + NUDGE_HIDE.days) }
    }

    override suspend fun connect(hint: String?): ConnectResult {
        val result =
            try {
                authorizer.connect(hint)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                ConnectResult.Failed(BackupFailure.UNKNOWN)
            }
        // 취소는 사용자가 고른 일이라 말없이 넘어간다.
        _connectFailure.value = (result as? ConnectResult.Failed)?.kind
        if (result !is ConnectResult.Connected) return result
        return try {
            start(result)
            result
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            // 공간이 차서 연결을 적지 못했다.
            // 연결되지 않은 것이라 실패로 알린다.
            _connectFailure.value = BackupFailure.UNKNOWN
            ConnectResult.Failed(BackupFailure.UNKNOWN)
        }
    }

    private suspend fun start(result: ConnectResult.Connected) {
        // 자동 백업은 처음 연결할 때만 켠다.
        // 끊긴 연결을 다시 이을 때는 사용자가 정해 둔 값을 지킨다.
        // 다른 계정을 고르면 앞 계정의 백업 기록은 이 계정의 것이 아니다.
        store.update {
            val kept = if (it.accountKey == null || it.accountKey == result.account.key) it else it.withoutBackupRecord()
            kept.copy(
                accountKey = result.account.key,
                accountLabel = result.account.email,
                autoEnabled = it.accountKey == null || it.autoEnabled,
                userDisconnected = false,
                lastFailure = null
            )
        }
        if (store.current().autoEnabled) scheduler.scheduleDaily()
        // 다른 기기의 백업이 있으면 사용자가 고르기 전에 올리지 않는다(US2 시나리오 7).
        finding.value = true
        val found =
            try {
                findBackup()
            } finally {
                finding.value = false
            }
        if (found != null) {
            store.update { it.copy(foundBackupWaiting = true) }
            _foundBackup.value = found
        } else {
            backUpNow()
        }
    }

    override fun connectFailureShown() {
        _connectFailure.value = null
    }

    override fun skipFoundBackup() {
        if (_foundBackup.value == null) return
        _foundBackup.value = null
        scope.launch { store.update { it.copy(foundBackupWaiting = false) } }
        backUpNow()
    }

    override fun takeFoundBackup() {
        _foundBackup.value = null
        scope.launch { store.update { it.copy(foundBackupWaiting = false) } }
    }

    override fun backUpNow(cellularOnce: Boolean) {
        // 시스템 작업은 새 요청이 앞선 작업을 대신하므로, 올리는 중이면 끊지 않게 넘기지 않는다.
        // 도는 동안은 화면에 버튼도 없다.
        if (running()) return
        if (scheduler.startNow(cellularOnce)) return
        scope.launch { nowFinished(runner.value.run(cellularOnce)) }
    }

    /**
     * 지금 백업이 끝났다.
     * 올리지 않고 끝난 일은 상태에 남지 않아 따로 알린다.
     * 실패는 상태가 말한다.
     */
    fun nowFinished(outcome: BackupOutcome) {
        when (outcome) {
            BackupOutcome.Empty -> _skipped.tryEmit(BackupSkip.NO_ENTRIES)
            BackupOutcome.Busy -> _skipped.tryEmit(BackupSkip.BUSY)
            else -> Unit
        }
    }

    override suspend fun backUpIfDue() {
        // 찾은 백업 안내는 메모리에만 있어 앱을 다시 열면 다시 찾아 보여 준다.
        if (store.current().foundBackupWaiting) {
            if (_foundBackup.value == null) findBackupAgain()
            return
        }
        if (isDue()) scope.launch { runAutomatic() }
    }

    private suspend fun findBackupAgain() {
        val found =
            try {
                findBackup()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // 오프라인이면 다음에 앱을 열 때 다시 본다.
                return
            }
        if (found != null) _foundBackup.value = found else store.update { it.copy(foundBackupWaiting = false) }
    }

    override suspend fun runIfDue() {
        if (isDue()) runAutomatic()
    }

    /**
     * 자동 백업 한 번.
     * 바뀐 것이 없어 올리지 않은 날도 백업은 잘 되는 중이라 7일 알림을 뒤로 민다.
     */
    suspend fun runAutomatic(): BackupOutcome {
        // 끄거나 끊은 뒤에 남아 있던 시스템 작업이 깨워도 올리지 않는다.
        val prefs = store.current()
        if (prefs.accountKey == null || !prefs.autoEnabled || prefs.foundBackupWaiting) return BackupOutcome.Unchanged
        val outcome = runner.value.runIfChanged()
        if (outcome == BackupOutcome.Unchanged) stillBackedUp()
        return outcome
    }

    /**
     * 일기가 마지막 백업 그대로라 지난 실패는 더 이을 것이 없다.
     * 연결이 끊긴 것만은 사용자가 풀어야 해서 남긴다.
     * 공간이 차서 적지 못해도 다음 자동 백업이 다시 한다.
     */
    private suspend fun stillBackedUp() {
        try {
            store.update { prefs ->
                if (prefs.lastFailure == BackupFailure.AUTH) {
                    prefs
                } else {
                    prefs.copy(lastFailure = null, failingSince = null, failureNotifiedAt = null, bytesShort = 0)
                }
            }
            alerts?.schedule(clock.now() + ALERT_AFTER, null)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            return
        }
    }

    private suspend fun isDue(): Boolean {
        val prefs = store.current()
        val last = prefs.lastSuccessAt
        if (prefs.foundBackupWaiting) return false
        if (prefs.accountKey == null || !prefs.autoEnabled || prefs.lastFailure == BackupFailure.AUTH) return false
        return last == null || clock.now() - last >= 24.hours
    }

    override suspend fun setAuto(enabled: Boolean) {
        store.update { it.copy(autoEnabled = enabled) }
        if (enabled) {
            scheduler.scheduleDaily()
            // 끌 때 지운 알림을 되살린다.
            // 없으면 자동 백업이 멈춰도 아무도 모른다.
            alerts?.schedule(clock.now() + ALERT_AFTER, null)
        } else {
            scheduler.cancelDaily()
            notifier.cancel()
        }
    }

    override suspend fun setPhotosOnCellular(enabled: Boolean) = store.update { it.copy(photosOnCellular = enabled) }

    override suspend fun disconnect() {
        authorizer.disconnect()
        scheduler.cancelDaily()
        // 마지막 백업 기록은 끊은 계정의 것이다.
        // 남겨 두면 다른 계정을 연결했을 때 그 계정의 백업처럼 보인다.
        store.update {
            it.withoutBackupRecord().copy(
                accountKey = null,
                accountLabel = null,
                autoEnabled = false,
                userDisconnected = true,
                foundBackupWaiting = false
            )
        }
        _foundBackup.value = null
        notifier.cancel()
    }

    private fun BackupPrefs.withoutBackupRecord() = copy(
        rootFolderId = null,
        lastSuccessAt = null,
        lastSuccessSnapshot = null,
        lastSuccessFingerprint = null,
        lastAttemptAt = null,
        failingSince = null,
        lastFailure = null,
        failureNotifiedAt = null,
        photosNotBackedUp = 0,
        bytesShort = 0,
        restoredBackupAt = null
    )
}

/** 실패 알림까지의 시간(FR-016). 백업이 성공할 때 거는 값과 같다. */
private val ALERT_AFTER = 7.days

private const val NUDGE_ENTRIES = 3
private const val NUDGE_LIMIT = 3
private const val NUDGE_HIDE = 14
