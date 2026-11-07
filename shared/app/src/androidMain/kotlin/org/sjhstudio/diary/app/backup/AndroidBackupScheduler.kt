package org.sjhstudio.diary.app.backup

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.sjhstudio.diary.core.backup.BackupController
import org.sjhstudio.diary.core.backup.BackupOutcome
import org.sjhstudio.diary.core.backup.BackupRunner
import org.sjhstudio.diary.core.data.RestoreRepository
import org.sjhstudio.diary.core.model.BackupFailure
import org.sjhstudio.diary.core.model.BackupProgress
import org.sjhstudio.diary.core.platform.BackupAlertPoster
import org.sjhstudio.diary.core.platform.BackupNotifier
import org.sjhstudio.diary.core.platform.BackupProgressNotifications
import org.sjhstudio.diary.core.platform.BackupScheduler
import org.sjhstudio.diary.core.platform.NetworkState
import org.sjhstudio.diary.core.platform.appIsVisible
import org.sjhstudio.diary.feature.backup.backupChannelName
import org.sjhstudio.diary.feature.backup.backupDoneText
import org.sjhstudio.diary.feature.backup.backupProgressText
import org.sjhstudio.diary.feature.backup.backupProgressTitle
import org.sjhstudio.diary.feature.backup.backupStoppedText
import org.sjhstudio.diary.feature.backup.restoreDoneText
import org.sjhstudio.diary.feature.backup.restoreProgressText
import org.sjhstudio.diary.feature.backup.restoreProgressTitle

/**
 * 시스템이 때를 골라 돌리는 백업(research R14).
 * 네트워크가 있을 때만 돌고, 잠깐 실패면 시스템이 다시 부른다.
 */
class BackupWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params),
    KoinComponent {
    override suspend fun doWork(): Result = when (val outcome = get<BackupController>().runAutomatic()) {
        is BackupOutcome.Failed -> if (outcome.kind == BackupFailure.OFFLINE ||
            outcome.kind == BackupFailure.DRIVE_ERROR
        ) {
            Result.retry()
        } else {
            Result.success()
        }

        is BackupOutcome.Done -> {
            // 글은 올렸고 사진은 요금 없는 연결에서 잇는다.
            // 그 연결에서 남은 것은 공간 때문이라 다시 걸지 않는다.
            if (outcome.photosLeft > 0 && !get<NetworkState>().isUnmetered) get<BackupScheduler>().scheduleWifiPhotos()
            Result.success()
        }

        else -> Result.success()
    }
}

/**
 * 사용자가 누른 지금 백업(스펙 003 T043).
 * 앱을 나가도 끊기지 않게 진행 알림과 함께 돈다.
 * 알림 권한이 없어도 백업은 돌고, 진행은 앱 안에서 본다.
 */
class BackupNowWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params),
    KoinComponent {
    private val notifications = BackupProgressNotifications(context)

    override suspend fun getForegroundInfo(): ForegroundInfo = foreground(null)

    override suspend fun doWork(): Result {
        val runner = get<BackupRunner>()
        try {
            setForeground(foreground(null))
        } catch (_: IllegalStateException) {
            // 시스템이 포그라운드 전환을 막으면 앱이 살아 있는 동안만 돈다.
            // 백업은 이어서 할 수 있다.
        }
        return coroutineScope {
            val updates =
                launch {
                    runner.progress.filterNotNull().distinctUntilChangedBy { it.photosDone to it.step }.collect {
                        notifications.update(backupProgressTitle(), backupProgressText(it), it.photosDone, it.photosTotal)
                    }
                }
            val cellular = inputData.getBoolean(KEY_CELLULAR, false)
            var outcome = runner.run(cellular)
            // 연결 직후처럼 자동 백업이 먼저 돌면 끝날 때까지 알림으로 지키고, 남은 것이 있으면 잇는다.
            if (outcome == BackupOutcome.Busy) {
                // 앞선 작업이 막 시작해 아직 진행이 없을 수 있다.
                // 잠깐 기다려 보고, 복원 중이라 진행이 없으면 그냥 끝낸다.
                val started = withTimeoutOrNull(START_WAIT_MILLIS) { runner.progress.first { it != null } }
                if (started != null) {
                    runner.progress.first { it == null }
                    outcome = runner.run(cellular)
                }
            }
            updates.cancel()
            notifications.clear()
            // 올린 사진은 남아 있으니 네트워크가 돌아오면 시스템이 이어서 돌린다.
            val again = outcome as? BackupOutcome.Failed
            if (again != null && again.kind in RESUMABLE && runAttemptCount < MAX_RESUMES) {
                Result.retry()
            } else {
                get<BackupController>().nowFinished(outcome)
                tellIfAway(outcome)
                Result.success()
            }
        }
    }

    /**
     * 앱을 나가 있으면 끝났다고 알린다.
     * 화면을 보고 있으면 화면이 말해 준다.
     */
    private suspend fun tellIfAway(outcome: BackupOutcome) {
        if (appIsVisible()) return
        val text =
            when (outcome) {
                is BackupOutcome.Done -> backupDoneText(outcome.photosLeft)
                is BackupOutcome.Failed -> backupStoppedText(outcome.kind)
                else -> return
            }
        BackupAlertPoster(applicationContext, backupChannelName()).show(text.first, text.second)
    }

    private suspend fun foreground(progress: BackupProgress?): ForegroundInfo {
        notifications.createChannel(backupChannelName())
        val notification = notifications.build(backupProgressTitle(), progress?.let { backupProgressText(it) }, 0, 0)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(BackupProgressNotifications.ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(BackupProgressNotifications.ID, notification)
        }
    }
}

/**
 * 복원 사진을 받는 동안 앱을 지킨다.
 * 받기 자체는 앱 범위에서 돌고, 이 작업은 앱을 나가도 시스템이 멈추지 않게 알림과 함께 떠 있는다.
 * 앱이 끝까지 죽었으면 시스템이 이 작업을 다시 부르고, 그때는 남은 사진을 여기서 이어 받는다.
 */
class RestorePhotosWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params),
    KoinComponent {
    private val notifications = BackupProgressNotifications(context)

    override suspend fun getForegroundInfo(): ForegroundInfo = foreground()

    override suspend fun doWork(): Result {
        val restore = get<RestoreRepository>()
        val waited = inputData.getBoolean(KEY_WAITED, false)
        val earlier = restore.lastPhotoRun
        try {
            setForeground(foreground())
        } catch (_: IllegalStateException) {
            // 시스템이 포그라운드 전환을 막으면 앱이 살아 있는 동안만 받는다.
            // 남은 사진은 다음에 앱을 열 때 잇는다.
        }
        return coroutineScope {
            val updates =
                launch {
                    restore.photoProgress.filterNotNull().collect {
                        notifications.update(restoreProgressTitle(), restoreProgressText(it.done, it.total), it.done, it.total)
                    }
                }
            val again = restore.resumePhotos()
            restore.photoProgress.first { it == null }
            updates.cancel()
            notifications.clear()
            if (waited && again && runAttemptCount < MAX_RESUMES) {
                // 조건을 건 작업이라 네트워크가 돌아오면 시스템이 다시 부른다.
                Result.retry()
            } else {
                // 기다리다 깨어난 작업은 이번에 받은 결과만 알린다.
                val finished = restore.lastPhotoRun?.takeUnless { waited && it === earlier }
                if (finished != null && !appIsVisible()) {
                    val text = restoreDoneText(finished)
                    BackupAlertPoster(applicationContext, backupChannelName()).show(text.first, text.second)
                }
                Result.success()
            }
        }
    }

    private suspend fun foreground(): ForegroundInfo {
        notifications.createChannel(backupChannelName())
        val notification = notifications.build(restoreProgressTitle(), null, 0, 0)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(BackupProgressNotifications.ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(BackupProgressNotifications.ID, notification)
        }
    }
}

/**
 * 7일 실패 알림을 띄운다.
 * 예약한 때까지 백업이 없었다는 뜻이다.
 * 내용은 예약할 때 받은 그대로다.
 */
class BackupAlertWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE) ?: return Result.success()
        val body = inputData.getString(KEY_BODY) ?: return Result.success()
        BackupAlertPoster(applicationContext, backupChannelName()).show(title, body)
        return Result.success()
    }
}

/** 앱이 돌지 못해도 때가 되면 오도록 작업 관리자에 미리 맡긴다(research R16). */
class AndroidBackupNotifier(private val context: Context, private val clock: Clock = Clock.System) : BackupNotifier {
    private val manager get() = WorkManager.getInstance(context)

    override suspend fun schedule(at: Instant, title: String, body: String) {
        val delay = (at - clock.now()).inWholeMilliseconds.coerceAtLeast(0)
        val work =
            OneTimeWorkRequestBuilder<BackupAlertWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(KEY_TITLE to title, KEY_BODY to body))
                .build()
        manager.enqueueUniqueWork(ALERT, ExistingWorkPolicy.REPLACE, work)
    }

    // 7일 알림과 자리를 나눈다.
    // 같은 작업 이름을 쓰면 예약해 둔 알림이 사라진다.
    override fun show(title: String, body: String) {
        val work = OneTimeWorkRequestBuilder<BackupAlertWorker>().setInputData(workDataOf(KEY_TITLE to title, KEY_BODY to body)).build()
        manager.enqueueUniqueWork(NOTICE, ExistingWorkPolicy.REPLACE, work)
    }

    override fun cancel() {
        manager.cancelUniqueWork(ALERT)
        BackupAlertPoster(context, "").clear()
    }
}

/**
 * 작업 관리자의 하루 주기 작업 하나.
 * 조건은 네트워크 연결됨과 배터리 부족 아님이다.
 */
class AndroidBackupScheduler(private val context: Context) : BackupScheduler {
    private val manager get() = WorkManager.getInstance(context)
    private val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).setRequiresBatteryNotLow(true).build()

    override fun scheduleDaily() {
        // 주기 작업은 맡기자마자 한 번 돈다.
        // 첫 백업은 연결하는 쪽이 정하므로 하루 뒤부터 돌린다.
        val work = PeriodicWorkRequestBuilder<BackupWorker>(
            1,
            TimeUnit.DAYS
        ).setConstraints(constraints).setInitialDelay(1, TimeUnit.DAYS).build()
        manager.enqueueUniquePeriodicWork(DAILY, ExistingPeriodicWorkPolicy.KEEP, work)
    }

    override fun cancelDaily() {
        manager.cancelUniqueWork(DAILY)
    }

    // 조건을 걸지 않는다.
    // 인터넷이 없으면 조용히 기다리지 않고 바로 실패를 보여 준다.
    // 실패 뒤 재시도를 기다리는 작업이 있어도 사용자가 누른 요청이 대신한다.
    // 대기가 끝날 때까지 기다리지 않고, "이번만 모바일 데이터" 선택도 버리지 않는다.
    // 올리는 중인 백업은 BackupController가 여기까지 넘기지 않는다.
    override fun startNow(cellularOnce: Boolean): Boolean {
        val work =
            OneTimeWorkRequestBuilder<BackupNowWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setBackoffCriteria(BackoffPolicy.LINEAR, RETRY_MINUTES, TimeUnit.MINUTES)
                .setInputData(workDataOf(KEY_CELLULAR to cellularOnce))
                .build()
        manager.enqueueUniqueWork(NOW, ExistingWorkPolicy.REPLACE, work)
        return true
    }

    override fun scheduleWifiPhotos() {
        val unmetered = Constraints.Builder().setRequiredNetworkType(NetworkType.UNMETERED).build()
        manager.enqueueUniqueWork(
            PHOTOS,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<BackupWorker>().setConstraints(unmetered).build()
        )
    }

    override fun keepRestoreRunning() {
        val work = OneTimeWorkRequestBuilder<RestorePhotosWorker>().setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST).build()
        manager.enqueueUniqueWork(RESTORE, ExistingWorkPolicy.KEEP, work)
    }

    // 사용자가 모바일 데이터를 허용하면 앱을 열 때 바로 받으니 기다리는 작업을 바꾸지 않는다.
    override fun waitForPhotoNetwork(wifiOnly: Boolean) {
        val network = if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
        val work =
            OneTimeWorkRequestBuilder<RestorePhotosWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(network).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, RETRY_MINUTES, TimeUnit.MINUTES)
                .setInputData(workDataOf(KEY_WAITED to true))
                .build()
        manager.enqueueUniqueWork(RESTORE_WAIT, ExistingWorkPolicy.KEEP, work)
    }

    private companion object {
        const val RESTORE = "backup.restore"
        const val RESTORE_WAIT = "backup.restore.wait"
        const val DAILY = "backup.daily"
        const val NOW = "backup.now"
        const val PHOTOS = "backup.photos"
    }
}

private const val ALERT = "backup.alert"
private const val NOTICE = "backup.notice"
private const val KEY_TITLE = "title"
private const val KEY_BODY = "body"
private const val KEY_CELLULAR = "cellular"
private const val KEY_WAITED = "waited"
private const val START_WAIT_MILLIS = 3_000L
private const val MAX_RESUMES = 8
private const val RETRY_MINUTES = 1L
private val RESUMABLE = setOf(BackupFailure.OFFLINE, BackupFailure.DRIVE_ERROR)
