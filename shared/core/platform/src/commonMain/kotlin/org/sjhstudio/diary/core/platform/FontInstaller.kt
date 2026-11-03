package org.sjhstudio.diary.core.platform

import kotlinx.coroutines.flow.Flow

/**
 * 고를 때 받는 일기 글꼴(`docs/design.md` 일기 본문 글꼴 10종, research R17).
 * Android는 Play 필요할 때 설치 모듈(`fonts/<id>`), iOS는 On-Demand Resources로 받는다. 둘 다 스토어가 내려 주어 우리 서버가 없다.
 * 받으면 [AppFiles.fontDirectory]에 `<id>.ttf`로 복사해 두고 그 파일로 글꼴을 만든다.
 */
interface FontInstaller {
    /** 받아 둔 글꼴 파일. 아직 받지 않았으면 null. */
    fun installedPath(id: String): String?

    /** 받는다. 이미 받았으면 바로 [FontInstallState.Installed]를 낸다. */
    fun install(id: String): Flow<FontInstallState>
}

sealed interface FontInstallState {
    /** 받는 중. [fraction]은 0~1, 모르면 null. */
    data class Downloading(val fraction: Float?) : FontInstallState

    data class Installed(val path: String) : FontInstallState

    /** 받지 못했다. [offline]이면 인터넷에 연결되지 않았다. 쓰던 글꼴은 그대로 둔다. */
    data class Failed(val offline: Boolean) : FontInstallState
}
