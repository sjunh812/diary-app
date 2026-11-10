package org.sjhstudio.diary.core.platform

/**
 * 앱 전체가 화면 뒤로 가고 돌아오는 때(contracts/lock.md 다시 잠그기).
 * 화면 회전이나 창 크기 변경으로 화면이 다시 만들어질 때는 알리지 않음.
 * Android는 `ProcessLifecycleOwner`, iOS는 앱 상태 알림을 씀.
 */
expect object AppForeground {
    /** 지금부터 바뀔 때마다 알림. 돌려준 함수를 부르면 그만 들음. 메인 스레드에서 부름 */
    fun observe(onBackground: () -> Unit, onForeground: () -> Unit): () -> Unit
}
