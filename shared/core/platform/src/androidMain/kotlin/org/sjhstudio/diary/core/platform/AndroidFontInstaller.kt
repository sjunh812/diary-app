package org.sjhstudio.diary.core.platform

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import com.google.android.play.core.splitcompat.SplitCompat
import com.google.android.play.core.splitinstall.SplitInstallException
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener
import com.google.android.play.core.splitinstall.model.SplitInstallErrorCode
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import java.io.File
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * 글꼴마다 하나인 Play 필요할 때 설치 모듈(`fonts/<id>`, 모듈 이름이 글꼴 ID)을 받아 자산 `fonts/<id>.ttf`를 앱 파일로 복사
 * 받은 모듈은 앱을 다시 켜지 않고 [SplitCompat]으로 읽음(`DiaryApplication.attachBaseContext`)
 */
class AndroidFontInstaller(context: Context, private val files: AppFiles) : FontInstaller {
    private val context = context.applicationContext
    private val manager = SplitInstallManagerFactory.create(this.context)

    // 화면을 그릴 때도 불러 파일 확인은 글꼴마다 한 번만. 시작 화면 동안 뒤에서 먼저 채움(Startup)
    private val known = ConcurrentHashMap<String, String>()

    override fun installedPath(id: String): String? = known.getOrPut(id) {
        target(id).takeIf { it.isFile && it.length() > 0 }?.absolutePath.orEmpty()
    }
        .ifEmpty { null }

    override fun knownPath(id: String): String? = known[id]?.ifEmpty { null }

    private val _changes = MutableStateFlow(0)
    override val changes: StateFlow<Int> = _changes.asStateFlow()

    /** Play는 진행 중인 받기가 있으면 새 요청을 거절함. 글꼴 둘을 연달아 눌러도 차례로 받음 */
    private val oneAtATime = Mutex()

    /** 큰 모듈은 Play가 사용자 확인을 요구함. 확인 창을 띄울 화면 */
    private var activity: Activity? = null

    init {
        (this.context as? Application)?.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityResumed(activity: Activity) {
                    this@AndroidFontInstaller.activity = activity
                }

                override fun onActivityPaused(activity: Activity) {
                    if (this@AndroidFontInstaller.activity === activity) this@AndroidFontInstaller.activity = null
                }

                override fun onActivityCreated(activity: Activity, state: Bundle?) = Unit

                override fun onActivityStarted(activity: Activity) = Unit

                override fun onActivityStopped(activity: Activity) = Unit

                override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit

                override fun onActivityDestroyed(activity: Activity) = Unit
            }
        )
    }

    override fun install(id: String): Flow<FontInstallState> = flow { oneAtATime.withLock { emitAll(installNow(id)) } }

    private fun installNow(id: String): Flow<FontInstallState> = callbackFlow {
        installedPath(id)?.let {
            send(FontInstallState.Installed(it))
            close()
            return@callbackFlow
        }
        // 개발 빌드는 자산을 앱 안에 담음. 출시 빌드는 받은 모듈이 있을 때만 여기로 옴
        if (id in manager.installedModules || hasAsset(id)) {
            send(copied(id))
            close()
            return@callbackFlow
        }
        // 받기가 빨리 끝나면 시작 결과보다 상태가 먼저 올 수 있어 세션 번호가 아니라 모듈 이름으로 고름
        val listener =
            SplitInstallStateUpdatedListener { state ->
                if (id !in state.moduleNames()) return@SplitInstallStateUpdatedListener
                when (state.status()) {
                    SplitInstallSessionStatus.PENDING, SplitInstallSessionStatus.DOWNLOADING -> {
                        val total = state.totalBytesToDownload()
                        trySend(FontInstallState.Downloading(if (total > 0) state.bytesDownloaded().toFloat() / total else null))
                    }

                    SplitInstallSessionStatus.INSTALLED -> launch {
                        send(copied(id))
                        close()
                    }

                    SplitInstallSessionStatus.FAILED, SplitInstallSessionStatus.CANCELED -> {
                        trySend(FontInstallState.Failed(offline = state.errorCode() == SplitInstallErrorCode.NETWORK_ERROR))
                        close()
                    }

                    // 큰 모듈은 받기 전에 사용자에게 물음. 묻지 않으면 받는 중에서 멈춤
                    SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION -> {
                        val screen = activity
                        if (screen == null) {
                            trySend(FontInstallState.Failed(offline = false))
                            close()
                        } else {
                            manager.startConfirmationDialogForResult(state, screen, CONFIRM_REQUEST)
                        }
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

    /** 저장 공간이 없으면 복사가 실패함. 받기 실패로 알리고 다음에 앱을 열 때 다시 함 */
    private suspend fun copied(id: String): FontInstallState = try {
        FontInstallState.Installed(copy(id))
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        FontInstallState.Failed(offline = false)
    }

    private fun hasAsset(id: String): Boolean = runCatching { context.assets.open("fonts/$id.ttf").close() }.isSuccess

    /** 받은 모듈의 자산을 앱 파일 자리로 복사. 반쯤 쓴 파일이 남지 않게 임시 파일에 쓴 뒤 이름을 바꿈 */
    private suspend fun copy(id: String): String = withContext(Dispatchers.IO) {
        val target = target(id)
        target.parentFile?.mkdirs()
        val partial = File(target.parentFile, "$id.ttf.part")
        openFont(id).use { input -> partial.outputStream().use { input.copyTo(it) } }
        check(partial.renameTo(target)) { "글꼴 파일 이름을 바꾸지 못했다" }
        target.absolutePath.also {
            known[id] = it
            _changes.update { count -> count + 1 }
        }
    }

    /**
     * 앱을 켠 채 받은 모듈은 SplitCompat을 다시 입힌 애플리케이션 컨텍스트로만 보임(Play 문서 "다른 모듈에서 리소스 및 애셋 액세스")
     * Play가 나중에 실제로 설치한 뒤에는 새 패키지 컨텍스트로 읽음
     */
    private fun openFont(id: String): InputStream {
        val path = "fonts/$id.ttf"
        SplitCompat.install(context)
        return runCatching {
            context.assets.open(path)
        }.getOrElse { context.createPackageContext(context.packageName, 0).assets.open(path) }
    }

    override fun removeExcept(ids: Set<String>) {
        File(files.fontDirectory).listFiles()?.filter { it.name.substringBefore('.') !in ids }?.forEach { file ->
            if (file.delete()) {
                known.remove(file.name.substringBefore('.'))
                _changes.update { it + 1 }
            }
        }
    }

    private fun target(id: String) = File(files.fontDirectory, "$id.ttf")
}

private const val CONFIRM_REQUEST = 4_201
