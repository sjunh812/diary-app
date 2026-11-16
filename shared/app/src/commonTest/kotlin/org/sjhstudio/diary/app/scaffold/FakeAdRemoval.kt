package org.sjhstudio.diary.app.scaffold

import kotlinx.coroutines.flow.MutableStateFlow
import org.sjhstudio.diary.app.ads.AdRemovalFlow
import org.sjhstudio.diary.app.ads.AdRemovalSheet
import org.sjhstudio.diary.app.ads.AdRemovalState

/** 시험용 광고 제거 시트. [close]가 사용자가 시트를 닫은 것 */
internal class FakeAdRemoval : AdRemovalFlow {
    override val state = MutableStateFlow(AdRemovalState())
    var opened = 0
        private set

    override fun open() {
        opened++
        state.value = AdRemovalState(AdRemovalSheet.Remove)
    }

    fun close() {
        state.value = AdRemovalState()
    }
}
