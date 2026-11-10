package org.sjhstudio.diary.core.platform

import kotlinx.coroutines.flow.Flow

/**
 * 고를 때 받는 일기 글꼴(`docs/design.md` 일기 본문 글꼴 10종, research R17).
 * Android는 Play 필요할 때 설치 모듈(`fonts/<id>`), iOS는 On-Demand Resources로 받음.
 * 둘 다 스토어가 내려 주어 우리 서버가 없음.
 * 받으면 [AppFiles.fontDirectory]에 `<id>.ttf`로 복사해 두고 그 파일로 글꼴을 만듦.
 */
interface FontInstaller {
    /** 받아 둔 글꼴 파일. 아직 받지 않았으면 null */
    fun installedPath(id: String): String?

    /** 받음. 이미 받았으면 바로 [FontInstallState.Installed]를 냄 */
    fun install(id: String): Flow<FontInstallState>
}

sealed interface FontInstallState {
    /** 받는 중. [fraction]은 0~1, 모르면 null */
    data class Downloading(val fraction: Float?) : FontInstallState

    data class Installed(val path: String) : FontInstallState

    /** 받지 못함. [offline]이면 인터넷 연결 없음. 쓰던 글꼴은 그대로 둠 */
    data class Failed(val offline: Boolean) : FontInstallState
}
