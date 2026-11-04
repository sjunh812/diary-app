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
import java.util.concurrent.TimeUnit
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.sjhstudio.diary.core.backup.BackupOutcome
import org.sjhstudio.diary.core.backup.BackupRunner
import org.sjhstudio.diary.core.model.BackupFailure
import org.sjhstudio.diary.core.platform.BackupScheduler

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
