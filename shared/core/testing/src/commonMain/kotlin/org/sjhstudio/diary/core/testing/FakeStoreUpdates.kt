package org.sjhstudio.diary.core.testing

import kotlin.time.Duration
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import org.sjhstudio.diary.core.data.StoreUpdates
import org.sjhstudio.diary.core.model.InstallStep
import org.sjhstudio.diary.core.model.StoreCheck

/** 스토어 대역. [answer]가 null이면 묻기에 실패 */
class FakeStoreUpdates(override val installed: String = "43", override val checkEvery: Duration = Duration.ZERO) : StoreUpdates {
    var answer: StoreCheck? = StoreCheck.None
    var checks = 0
        private set
    var starts = 0
        private set
    var restarts = 0
        private set
    var storePages = 0
        private set
    var accepts = true

    /** 참이면 묻기가 끝나지 않음(답하지 않는 스토어) */
    var hangs = false

    override val installs = MutableSharedFlow<InstallStep>(extraBufferCapacity = 8)

    override suspend fun check(): StoreCheck {
        checks++
        if (hangs) awaitCancellation()
        return answer ?: throw IllegalStateException("store unavailable")
    }

    override suspend fun start(): Boolean {
        starts++
        return accepts
    }

    override fun restart() {
        restarts++
    }

    override fun openStorePage() {
        storePages++
    }
}
