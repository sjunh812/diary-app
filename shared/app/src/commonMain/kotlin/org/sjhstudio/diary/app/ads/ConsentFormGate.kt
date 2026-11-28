package org.sjhstudio.diary.app.ads

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * 광고 동의 창을 띄울 때를 정함(`docs/business.md` 광고 동의)
 * 창은 미리 받아 두고, 목록 광고 자리나 보상 영상 시트가 화면에 있는 동안만 띄움
 * 앱이 뒤에 있어 못 띄운 창은 앞으로 돌아와 다시 물을 때 띄움
 * 받는 사이 상세·작성 화면으로 넘어가면 그 화면 위에 뜨지 않게(헌법 III)
 * 화면 스레드에서만 부름
 *
 * [needed]는 아직 답하지 않았는지, [load]는 창 받기, [present]는 받아 둔 창을 띄우고 닫히면 띄웠는지를 알림
 * 창 받기도 여기서만 불러 받는 중에 겹쳐 받거나, 실패한 뒤 누름이 끝없이 기다리지 않게
 */
class ConsentFormGate(
    private val needed: () -> Boolean,
    private val load: () -> Unit,
    private val present: (onClosed: (shown: Boolean) -> Unit) -> Unit
) {
    private enum class Form { NONE, LOADING, READY }

    private val askers = mutableSetOf<String>()
    private val afterAnswer = mutableListOf<() -> Unit>()
    private var form = Form.NONE
    private var showing = false

    /** 받아 둔 창이 없으면 받음. 답할 차례가 아니면 [load]가 [formFailed]로 끝냄 */
    fun prepare() {
        if (form != Form.NONE) return
        form = Form.LOADING
        load()
    }

    /** 창을 받아 둠 */
    fun formReady() {
        form = Form.READY
        showIfWanted()
    }

    /** 창을 받지 못함. 누르고 기다리던 일은 동의 없이 이어 가고, 다음 누름 때 다시 받음 */
    fun formFailed() {
        form = Form.NONE
        finish()
    }

    /** 광고 자리나 보상 영상 시트가 화면에 나옴 */
    fun ask(asker: String) {
        askers += asker
        showIfWanted()
    }

    /** 광고 자리나 시트가 화면에서 사라지거나 앱이 뒤로 감 */
    fun stopAsking(asker: String) {
        askers -= asker
    }

    /**
     * 사용자가 눌러 광고를 부를 때(보상형). 답하거나 창을 못 받을 때까지 기다림
     * 창은 시트가 화면에 있을 때만 뜸. 기다리다 취소되면 기다림만 거둠
     */
    suspend fun answer() {
        if (!needed()) return
        suspendCancellableCoroutine { result ->
            val resume = { if (result.isActive) result.resume(Unit) }
            afterAnswer += resume
            result.invokeOnCancellation { afterAnswer -= resume }
            prepare()
        }
    }

    private fun showIfWanted() {
        if (form != Form.READY || showing || askers.isEmpty()) return
        if (!needed()) {
            form = Form.NONE
            return finish()
        }
        showing = true
        present { shown ->
            showing = false
            if (shown) {
                form = Form.NONE
                finish()
            }
        }
    }

    private fun finish() {
        val works = afterAnswer.toList()
        afterAnswer.clear()
        works.forEach { it() }
    }
}

/** 광고 자리가 화면에 있고 앱이 앞에 있는 동안만 동의 창을 원함 */
@Composable
internal fun ConsentFormGate.AskWhileShown(key: String) {
    LifecycleResumeEffect(this, key) {
        ask(key)
        onPauseOrDispose { stopAsking(key) }
    }
}
