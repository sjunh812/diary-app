package org.sjhstudio.diary.app.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlin.time.Instant
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.sjhstudio.diary.core.backup.BackupOutcome
import org.sjhstudio.diary.core.backup.BackupRunner
import org.sjhstudio.diary.core.model.BackupFailure
import org.sjhstudio.diary.core.platform.BackupAlertPoster
import org.sjhstudio.diary.core.platform.BackupNotifier
import org.sjhstudio.diary.core.platform.BackupScheduler
import org.sjhstudio.diary.feature.backup.backupChannelName

/** 시스템이 때를 골라 돌리는 백업(research R14). 네트워크가 있을 때만 돌고, 잠깐 실패면 시스템이 다시 부른다. */
class BackupWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params),
    KoinComponent {
    override suspend fun doWork(): Result = when (val outcome = get<BackupRunner>().run()) {
        is BackupOutcome.Failed -> if (outcome.kind == BackupFailure.OFFLINE ||
            outcome.kind == BackupFailure.DRIVE_ERROR
        ) {
            Result.retry()
        } else {
            Result.success()
        }

        else -> Result.success()
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

    override fun startNow() {
        val work = OneTimeWorkRequestBuilder<BackupWorker>().setConstraints(
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        ).build()
        manager.enqueueUniqueWork(NOW, ExistingWorkPolicy.KEEP, work)
    }

    private companion object {
        const val DAILY = "backup.daily"
        const val NOW = "backup.now"
    }
}

private const val ALERT = "backup.alert"
private const val KEY_TITLE = "title"
private const val KEY_BODY = "body"
