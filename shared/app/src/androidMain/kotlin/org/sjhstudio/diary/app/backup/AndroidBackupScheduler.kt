package org.sjhstudio.diary.app.backup

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
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
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.sjhstudio.diary.core.backup.BackupOutcome
import org.sjhstudio.diary.core.backup.BackupRunner
import org.sjhstudio.diary.core.model.BackupFailure
import org.sjhstudio.diary.core.model.BackupProgress
import org.sjhstudio.diary.core.platform.BackupAlertPoster
import org.sjhstudio.diary.core.platform.BackupNotifier
import org.sjhstudio.diary.core.platform.BackupProgressNotifications
import org.sjhstudio.diary.core.platform.BackupScheduler
import org.sjhstudio.diary.core.platform.NetworkState
import org.sjhstudio.diary.feature.backup.backupChannelName
import org.sjhstudio.diary.feature.backup.backupProgressText
import org.sjhstudio.diary.feature.backup.backupProgressTitle

/** 시스템이 때를 골라 돌리는 백업(research R14). 네트워크가 있을 때만 돌고, 잠깐 실패면 시스템이 다시 부른다. */
class BackupWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params),
    KoinComponent {
    override suspend fun doWork(): Result = when (val outcome = get<BackupRunner>().runIfChanged()) {
        is BackupOutcome.Failed -> if (outcome.kind == BackupFailure.OFFLINE ||
            outcome.kind == BackupFailure.DRIVE_ERROR
        ) {
            Result.retry()
        } else {
            Result.success()
        }

        is BackupOutcome.Done -> {
            // 글은 올렸고 사진은 요금 없는 연결에서 잇는다. 그 연결에서 남은 것은 공간 때문이라 다시 걸지 않는다.
            if (outcome.photosLeft > 0 && !get<NetworkState>().isUnmetered) get<BackupScheduler>().scheduleWifiPhotos()
            Result.success()
        }

        else -> Result.success()
    }
}

/**
 * 사용자가 누른 지금 백업(스펙 003 T043). 앱을 나가도 끊기지 않게 진행 알림과 함께 돈다.
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
            // 시스템이 앞으로 올리기를 막으면 앱이 살아 있는 동안만 돈다. 백업은 이어서 할 수 있다.
        }
        coroutineScope {
            val updates =
                launch {
                    runner.progress.filterNotNull().distinctUntilChangedBy { it.photosDone to it.step }.collect {
                        notifications.update(backupProgressTitle(), backupProgressText(it), it.photosDone, it.photosTotal)
                    }
                }
            runner.run(inputData.getBoolean(KEY_CELLULAR, false))
            updates.cancel()
        }
        return Result.success()
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

/** 7일 실패 알림을 띄운다. 예약한 때까지 백업이 없었다는 뜻이다. 내용은 예약할 때 받은 그대로다. */
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

    override fun schedule(at: Instant, title: String, body: String) {
        val delay = (at - clock.now()).inWholeMilliseconds.coerceAtLeast(0)
        val work =
            OneTimeWorkRequestBuilder<BackupAlertWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(KEY_TITLE to title, KEY_BODY to body))
                .build()
        manager.enqueueUniqueWork(ALERT, ExistingWorkPolicy.REPLACE, work)
    }

    override fun cancel() {
        manager.cancelUniqueWork(ALERT)
        BackupAlertPoster(context, "").clear()
    }
}

/** 작업 관리자의 하루 주기 작업 하나. 조건은 네트워크 연결됨과 배터리 부족 아님이다. */
class AndroidBackupScheduler(private val context: Context) : BackupScheduler {
    private val manager get() = WorkManager.getInstance(context)
    private val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).setRequiresBatteryNotLow(true).build()

    override fun scheduleDaily() {
        val work = PeriodicWorkRequestBuilder<BackupWorker>(1, TimeUnit.DAYS).setConstraints(constraints).build()
        manager.enqueueUniquePeriodicWork(DAILY, ExistingPeriodicWorkPolicy.KEEP, work)
    }

    override fun cancelDaily() {
        manager.cancelUniqueWork(DAILY)
    }

    // 조건을 걸지 않는다. 인터넷이 없으면 조용히 기다리지 않고 바로 실패를 보여 준다.
    override fun startNow(cellularOnce: Boolean): Boolean {
        val work =
            OneTimeWorkRequestBuilder<BackupNowWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setInputData(workDataOf(KEY_CELLULAR to cellularOnce))
                .build()
        manager.enqueueUniqueWork(NOW, ExistingWorkPolicy.KEEP, work)
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

    private companion object {
        const val DAILY = "backup.daily"
        const val NOW = "backup.now"
        const val PHOTOS = "backup.photos"
    }
}

private const val ALERT = "backup.alert"
private const val KEY_TITLE = "title"
private const val KEY_BODY = "body"
private const val KEY_CELLULAR = "cellular"
