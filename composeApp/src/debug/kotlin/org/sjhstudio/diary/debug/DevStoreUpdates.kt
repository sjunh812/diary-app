package org.sjhstudio.diary.debug

import android.content.Context
import android.content.Intent
import android.os.Process
import androidx.core.content.edit
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import org.sjhstudio.diary.core.data.AppUpdateRepository
import org.sjhstudio.diary.core.data.RemoteValues
import org.sjhstudio.diary.core.data.StoreUpdates
import org.sjhstudio.diary.core.model.InstallStep
import org.sjhstudio.diary.core.model.StoreCheck

/** 개발자 모드 "새 버전". 앱을 다시 켜도 남음 */
enum class DevUpdate(val label: String, val summary: String) {
    REAL("실제", "Play 응답 그대로"),
    AVAILABLE("있음", "기본 문구"),
    NOTE("요약 있음", "업데이트 요약 표시"),
    REQUIRED("강제 업데이트", "닫을 수 없는 카드"),
    FAIL("받기 실패", "40%에서 실패")
}

/**
 * 개발 빌드에서 Play 없이 새 버전 흐름을 끝까지 돌아 봄(스펙 009)
 * 받기는 3초 동안 진행을 흉내 내고, 다시 시작하면 흉내를 끄고 앱을 다시 켬
 */
class DevStoreUpdates(private val context: Context, private val real: StoreUpdates, private val scope: CoroutineScope) : StoreUpdates {
    private val prefs = context.getSharedPreferences("dev_mode", Context.MODE_PRIVATE)
    private val chosen = MutableStateFlow(prefs.getString(KEY, null)?.let(DevUpdate::valueOf) ?: DevUpdate.REAL)
    val mode: StateFlow<DevUpdate> = chosen.asStateFlow()
    private val steps = MutableSharedFlow<InstallStep>(extraBufferCapacity = 16)
    private var downloaded = false

    fun choose(mode: DevUpdate) {
        // 다시 시작하며 프로세스를 끝내도 남게 바로 씀
        prefs.edit(commit = true) { putString(KEY, mode.name) }
        chosen.value = mode
        downloaded = false
    }

    override val installed: String get() = real.installed

    override val checkEvery: Duration get() = real.checkEvery

    override suspend fun check(): StoreCheck = when {
        chosen.value == DevUpdate.REAL -> real.check()
        downloaded -> StoreCheck.Downloaded
        else -> StoreCheck.Available(FAKE_VERSION_CODE)
    }

    override val installs: Flow<InstallStep> = merge(real.installs, steps)

    override suspend fun start(): Boolean {
        val mode = chosen.value
        if (mode == DevUpdate.REAL) return real.start()
        scope.launch {
            for (percent in 0..100 step STEP_PERCENT) {
                if (mode == DevUpdate.FAIL && percent > FAIL_PERCENT) {
                    steps.emit(InstallStep.Failed)
                    return@launch
                }
                steps.emit(InstallStep.Downloading(percent / 100f))
                delay(STEP_MILLIS)
            }
            downloaded = true
            steps.emit(InstallStep.Downloaded)
        }
        return true
    }

    override fun restart() {
        if (chosen.value == DevUpdate.REAL) return real.restart()
        // 새 버전으로 바뀐 것처럼 흉내를 끄고 다시 켬
        choose(DevUpdate.REAL)
        context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
            context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        }
        Process.killProcess(Process.myPid())
    }

    override fun openStorePage() = real.openStorePage()

    /** 흉내에 맞춘 원격 값. 실제면 그대로 */
    fun remote(real: RemoteValues) = RemoteValues { key ->
        when (chosen.value) {
            DevUpdate.REQUIRED -> if (key == AppUpdateRepository.KEY_MIN_VERSION) FAKE_VERSION_NAME else real.string(key)

            DevUpdate.NOTE ->
                when (key) {
                    AppUpdateRepository.KEY_NOTE_VERSION -> FAKE_VERSION_NAME
                    AppUpdateRepository.KEY_NOTE_PREFIX + "ko" -> "결제 뒤에도 광고가 보이던 문제를 고쳤어요."
                    AppUpdateRepository.KEY_NOTE_PREFIX + "en" -> "Fixed ads showing after purchase."
                    AppUpdateRepository.KEY_NOTE_PREFIX + "ja" -> "購入後も広告が表示される問題を修正しました。"
                    else -> real.string(key)
                }

            else -> real.string(key)
        }
    }

    private companion object {
        const val KEY = "update"
        const val FAKE_VERSION_CODE = "9999"
        const val FAKE_VERSION_NAME = "99.0"
        const val STEP_PERCENT = 10
        const val STEP_MILLIS = 300L
        const val FAIL_PERCENT = 40
    }
}
