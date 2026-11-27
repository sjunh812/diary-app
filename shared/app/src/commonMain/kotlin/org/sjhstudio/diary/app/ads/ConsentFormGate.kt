package org.sjhstudio.diary.app.ads

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.LifecycleResumeEffect

/**
 * 광고 동의 창을 띄울 때를 정함(`docs/business.md` 광고 동의)
 * 창은 미리 받아 두고, 목록 광고 자리가 화면에 있는 동안만 띄움
 * 받는 사이 상세·작성 화면으로 넘어가면 그 화면 위에 뜨지 않게(헌법 III)
 * 화면 스레드에서만 부름
 *
 * [needed]는 아직 답하지 않았는지, [present]는 받아 둔 창을 띄우고 닫히면 띄웠는지를 알림
 */
class ConsentFormGate(private val needed: () -> Boolean, private val present: (onClosed: (shown: Boolean) -> Unit) -> Unit) {
    private val askers = mutableSetOf<String>()
    private val afterAnswer = mutableListOf<() -> Unit>()
    private var ready = false
    private var showing = false

    /** 창을 받아 둠 */
    fun formReady() {
        ready = true
        showIfWanted()
    }

    /** 창을 받지 못함. 누르고 기다리던 일은 동의 없이 이어 감 */
    fun formFailed() {
        ready = false
        finish()
    }

    /** 목록 광고 자리가 화면에 나옴 */
    fun ask(asker: String) {
        askers += asker
        showIfWanted()
    }

    /** 목록 광고 자리가 화면에서 사라지거나 앱이 뒤로 감 */
    fun stopAsking(asker: String) {
        askers -= asker
    }

    /** 사용자가 눌러 광고를 부를 때(보상형). 답한 뒤 [then], 물을 것이 없으면 바로 */
    fun askNow(then: () -> Unit) {
        if (!needed()) return then()
        afterAnswer += then
        ask(TAPPED)
    }

    private fun showIfWanted() {
        if (!ready || showing || askers.isEmpty()) return
        if (!needed()) {
            ready = false
            return finish()
        }
        showing = true
        present { shown ->
            showing = false
            if (shown) {
                ready = false
                finish()
            }
        }
    }

    private fun finish() {
        askers -= TAPPED
        val works = afterAnswer.toList()
        afterAnswer.clear()
        works.forEach { it() }
    }

    private companion object {
        const val TAPPED = "tapped"
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
