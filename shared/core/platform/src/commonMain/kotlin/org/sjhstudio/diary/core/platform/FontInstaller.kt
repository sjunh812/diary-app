package org.sjhstudio.diary.core.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 고를 때 받는 일기 글꼴(`docs/design.md` 일기 본문 글꼴 10종, research R17)
 * Android는 Play 필요할 때 설치 모듈(`fonts/<id>`), iOS는 On-Demand Resources로 받음
 * 둘 다 스토어가 내려 주어 우리 서버가 없음
 * 받으면 [AppFiles.fontDirectory]에 `<id>.ttf`로 복사해 두고 그 파일로 글꼴을 만듦
 */
interface FontInstaller {
    /** 받아 둔 글꼴 파일. 아직 받지 않았으면 null */
    fun installedPath(id: String): String?

    /**
     * 받아 둔 글꼴 파일이 생기거나 지워질 때마다 늘어남
     * 글꼴 파일로 그리는 곳은 이 값이 바뀌면 [installedPath]를 다시 봄
     */
    val changes: StateFlow<Int> get() = NO_CHANGES

    /** 이미 확인해 둔 경로만. 파일을 보지 않아 화면을 그리는 중에 불러도 됨. 모르면 null */
    fun knownPath(id: String): String? = installedPath(id)

    /** 받음. 이미 받았으면 바로 [FontInstallState.Installed]를 냄 */
    fun install(id: String): Flow<FontInstallState>

    /** [ids] 밖의 받은 글꼴 파일 삭제. 앱에서 뺀 글꼴이 저장 공간을 차지하지 않게 함 */
    fun removeExcept(ids: Set<String>) = Unit
}

private val NO_CHANGES = MutableStateFlow(0).asStateFlow()

sealed interface FontInstallState {
    /** 받는 중. [fraction]은 0~1, 모르면 null */
    data class Downloading(val fraction: Float?) : FontInstallState

    data class Installed(val path: String) : FontInstallState

    /** 받지 못함. [offline]이면 인터넷 연결 없음. 쓰던 글꼴은 그대로 둠 */
    data class Failed(val offline: Boolean) : FontInstallState
}
