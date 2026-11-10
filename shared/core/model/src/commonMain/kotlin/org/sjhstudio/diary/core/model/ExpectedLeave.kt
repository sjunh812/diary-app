package org.sjhstudio.diary.core.model

/**
 * 사진 고르기·카메라처럼 앱이 잠깐 시스템 화면으로 나갔다 돌아오는 표시.
 * 잠금을 곧바로 다시 걸면 앱 화면이 치워져 고른 결과를 받을 곳이 없어짐.
 * 앱을 켠 채 자리를 비운 경우까지 풀리지 않게 잠금 쪽에서 시간 제한을 둠.
 */
object ExpectedLeave {
    var marked: Boolean = false
        private set

    fun mark() {
        marked = true
    }

    fun clear() {
        marked = false
    }
}
