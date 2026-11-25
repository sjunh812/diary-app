package org.sjhstudio.diary.feature.write

/**
 * 재생성된 쓰기 화면에 커서와 키보드를 돌려줄지
 * 본문에 커서가 있는 동안 화면 키보드가 보이다 사라지면 사용자가 내린 것으로 봄
 * 처음부터 화면 키보드가 없던 하드웨어 키보드는 내린 것이 아님
 */
internal class WritingMemory {
    var writing: Boolean = false
        private set
    private var imeWasVisible = false
    private var dismissed = false

    fun update(focused: Boolean, imeVisible: Boolean) {
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
