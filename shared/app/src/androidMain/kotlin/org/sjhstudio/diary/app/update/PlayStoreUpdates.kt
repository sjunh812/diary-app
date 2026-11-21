package org.sjhstudio.diary.app.update

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.google.android.gms.tasks.Task
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.sjhstudio.diary.app.CurrentActivity
import org.sjhstudio.diary.core.data.StoreUpdates
import org.sjhstudio.diary.core.model.InstallStep
import org.sjhstudio.diary.core.model.StoreCheck

/**
 * Play 앱 안 업데이트(유연 흐름, 스펙 009)
 * 동의 창은 Play가 그리고, 받기 진행과 다시 시작 안내는 앱이 그림
 * Play에서 받지 않은 앱이면 Play가 새 버전을 알리지 않아 아무것도 보이지 않음
 */
class PlayStoreUpdates(context: Context, private val current: CurrentActivity, override val installed: String) : StoreUpdates {
    private val app = context.applicationContext
    private val manager = AppUpdateManagerFactory.create(app)
    private val flexible = AppUpdateOptions.defaultOptions(AppUpdateType.FLEXIBLE)

    // 기기 안 Play에 묻는 일이라 돌아올 때마다 봄. 받은 상태도 이때 알아야 함
    override val checkEvery: Duration = Duration.ZERO

    override suspend fun check(): StoreCheck {
        val info = manager.appUpdateInfo.await()
        return when (info.installStatus()) {
            InstallStatus.DOWNLOADED -> StoreCheck.Downloaded

            InstallStatus.PENDING, InstallStatus.DOWNLOADING -> StoreCheck.Downloading(info.progress())

            else ->
                if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                    StoreCheck.Available(info.availableVersionCode().toString())
                } else {
                    StoreCheck.None
                }
        }
    }

    override val installs: Flow<InstallStep> = callbackFlow {
        val listener =
            InstallStateUpdatedListener { state ->
                when (state.installStatus()) {
                    InstallStatus.PENDING, InstallStatus.DOWNLOADING ->
                        trySend(InstallStep.Downloading(progress(state.bytesDownloaded(), state.totalBytesToDownload())))

                    InstallStatus.DOWNLOADED -> trySend(InstallStep.Downloaded)

                    InstallStatus.FAILED -> trySend(InstallStep.Failed)

                    InstallStatus.CANCELED -> trySend(InstallStep.Canceled)

                    else -> Unit
                }
            }
        manager.registerListener(listener)
        awaitClose { manager.unregisterListener(listener) }
    }

    override suspend fun start(): Boolean {
        // 정보는 흐름 한 번에만 쓸 수 있어 누를 때 새로 받음
        val info = manager.appUpdateInfo.await()
        val activity = current.value
        return when {
            activity == null || info.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE -> false

            // 유연 흐름을 못 쓰는 때(Play가 막음)는 스토어 화면으로 보냄
            !info.isUpdateTypeAllowed(flexible) -> {
                openStorePage()
                true
            }

            else -> manager.startUpdateFlow(info, activity, flexible).await() == Activity.RESULT_OK
        }
    }

    override fun restart() {
        manager.completeUpdate()
    }

    override fun openStorePage() {
        val activity = current.value ?: return
        val market = Intent(Intent.ACTION_VIEW, "market://details?id=${app.packageName}".toUri())
        try {
            activity.startActivity(market)
        } catch (_: ActivityNotFoundException) {
            // Play 스토어 앱이 없으면 웹 화면
            activity.startActivity(
                Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=${app.packageName}".toUri())
            )
        }
    }

    private fun AppUpdateInfo.progress() = progress(bytesDownloaded(), totalBytesToDownload())

    private fun progress(done: Long, total: Long): Float? = if (total > 0) done.toFloat() / total else null
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        val error = task.exception
        if (error == null) continuation.resume(task.result) else continuation.resumeWithException(error)
    }
}
