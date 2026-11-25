package org.sjhstudio.diary.feature.write

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.retain.RetainObserver
import androidx.compose.runtime.setValue

/**
 * 재생성된 쓰기 화면에 커서와 키보드를 돌려줄지
 * 본문에 커서가 있는 동안 화면 키보드가 보이다 사라지면 사용자가 내린 것으로 봄
 * 처음부터 화면 키보드가 없던 하드웨어 키보드는 내린 것이 아님
 */
internal class WritingMemory : RetainObserver {
    var writing: Boolean = false
        private set
    private var imeWasVisible = false
    private var dismissed = false

    /**
     * 돌려줄 커서가 대기 중인지
     * 첫 화면은 앞 값을 읽고, 화면에 들어온 뒤 바뀜
     * Compose 상태라 바뀌면 화면을 다시 그림
     */
    var waiting by mutableStateOf(false)
        private set

    /**
     * 화면에 들어올 때마다 불림(처음, 재생성 뒤 돌아올 때)
     * 버려진 컴포지션에서는 불리지 않아 실제로 들어온 때만 대기로 둠
     * 쓰던 중이었으면 돌려줄 커서를 대기로 둠
     * 앞 화면의 키보드 기록은 비워 새 화면에서 내린 것으로 이어지지 않게 함
     */
    override fun onEnteredComposition() {
        imeWasVisible = false
        dismissed = false
        waiting = writing
    }

    override fun onRetained() = Unit

    override fun onExitedComposition() = Unit

    override fun onRetired() = Unit

    override fun onUnused() = Unit

    /** 돌려줄 커서를 줬을 때 */
    fun settle() {
        waiting = false
    }

    /** 시트·권유·창이 덮어 돌려줄 커서를 버릴 때. 닫은 뒤 다시 돌려도 안 돌려줌 */
    fun drop() {
        if (!waiting) return
        waiting = false
        writing = false
    }

    /** 대기 중에는 기록 안 함. 재생성이 잇달아도 커서가 남음 */
    fun update(focused: Boolean, imeVisible: Boolean) {
        if (waiting) return
        dismissed =
            when {
                !focused || imeVisible -> false
                imeWasVisible -> true
                else -> dismissed
            }
        imeWasVisible = imeVisible
        writing = focused && !dismissed
    }
}
