package org.sjhstudio.diary.core.testing

import org.sjhstudio.diary.core.data.ContextTips
import org.sjhstudio.diary.core.model.ContextTip

/** 화면 시험용. [open]에 넣은 팁만 보이고, 부른 일을 적어 둠 */
class FakeContextTips(private val open: Set<ContextTip> = emptySet()) : ContextTips {
    val done = mutableListOf<ContextTip>()
    var clippedOpens = 0
        private set

    override fun isOpen(tip: ContextTip): Boolean = tip in open && tip !in done

    override suspend fun load() = Unit

    override fun done(tip: ContextTip) {
        done += tip
    }

    override fun clippedOpened() {
        clippedOpens++
    }
}
