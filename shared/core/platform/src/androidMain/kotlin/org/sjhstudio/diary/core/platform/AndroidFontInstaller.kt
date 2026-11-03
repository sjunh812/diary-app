package org.sjhstudio.diary.core.platform

import android.content.Context
import com.google.android.play.core.splitcompat.SplitCompat
import com.google.android.play.core.splitinstall.SplitInstallException
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener
import com.google.android.play.core.splitinstall.model.SplitInstallErrorCode
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 글꼴마다 하나인 Play 필요할 때 설치 모듈(`fonts/<id>`, 모듈 이름이 글꼴 ID다)을 받아 자산 `fonts/<id>.ttf`를 앱 파일로 복사한다.
 * 받은 모듈은 앱을 다시 켜지 않고 [SplitCompat]으로 읽는다(`DiaryApplication.attachBaseContext`).
 */
class AndroidFontInstaller(context: Context, private val files: AppFiles) : FontInstaller {
    private val context = context.applicationContext
    private val manager = SplitInstallManagerFactory.create(this.context)

    override fun installedPath(id: String): String? = target(id).takeIf { it.isFile && it.length() > 0 }?.absolutePath

    override fun install(id: String): Flow<FontInstallState> = callbackFlow {
        installedPath(id)?.let {
            send(FontInstallState.Installed(it))
            close()
            return@callbackFlow
        }
        if (id in manager.installedModules) {
            send(FontInstallState.Installed(copy(id)))
            close()
            return@callbackFlow
        }
        // 받기가 빨리 끝나면 시작 결과보다 상태가 먼저 올 수 있어 세션 번호가 아니라 모듈 이름으로 고른다.
        val listener =
            SplitInstallStateUpdatedListener { state ->
                if (id !in state.moduleNames()) return@SplitInstallStateUpdatedListener
                when (state.status()) {
                    SplitInstallSessionStatus.PENDING, SplitInstallSessionStatus.DOWNLOADING -> {
                        val total = state.totalBytesToDownload()
                        trySend(FontInstallState.Downloading(if (total > 0) state.bytesDownloaded().toFloat() / total else null))
                    }

                    SplitInstallSessionStatus.INSTALLED -> launch {
                        send(runCatching { FontInstallState.Installed(copy(id)) }.getOrElse { FontInstallState.Failed(offline = false) })
                        close()
                    }

                    SplitInstallSessionStatus.FAILED, SplitInstallSessionStatus.CANCELED -> {
                        trySend(FontInstallState.Failed(offline = state.errorCode() == SplitInstallErrorCode.NETWORK_ERROR))
                        close()
                    }

                    else -> Unit
                }
            }
        manager.registerListener(listener)
        trySend(FontInstallState.Downloading(null))
        manager.startInstall(SplitInstallRequest.newBuilder().addModule(id).build())
            .addOnFailureListener { error ->
                val code = (error as? SplitInstallException)?.errorCode
                trySend(FontInstallState.Failed(offline = code == SplitInstallErrorCode.NETWORK_ERROR))
                close()
            }
        awaitClose { manager.unregisterListener(listener) }
    }.flowOn(Dispatchers.Main)

    /** 받은 모듈의 자산을 앱 파일 자리로 복사한다. 반쯤 쓴 파일을 남기지 않게 임시 파일에 쓴 뒤 이름을 바꾼다. */
    private suspend fun copy(id: String): String = withContext(Dispatchers.IO) {
        SplitCompat.install(context)
        val assets = context.createPackageContext(context.packageName, 0).assets
        val target = target(id)
        target.parentFile?.mkdirs()
        val partial = File(target.parentFile, "$id.ttf.part")
        assets.open("fonts/$id.ttf").use { input -> partial.outputStream().use { input.copyTo(it) } }
        check(partial.renameTo(target)) { "글꼴 파일 이름을 바꾸지 못했다" }
        target.absolutePath
    }

    private fun target(id: String) = File(files.fontDirectory, "$id.ttf")
}
