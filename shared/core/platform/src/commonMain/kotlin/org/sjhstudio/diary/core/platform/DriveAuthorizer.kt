package org.sjhstudio.diary.core.platform

import org.sjhstudio.diary.core.model.BackupFailure

/**
 * 연결한 구글 계정. [key]는 구글 계정 고유 ID(sub), [email]은 화면에만 보임.
 * 둘 다 로그와 통계에 넣지 않음.
 */
data class DriveAccount(val key: String, val email: String) {
    override fun toString(): String = "DriveAccount"
}

sealed interface ConnectResult {
    data class Connected(val account: DriveAccount) : ConnectResult

    data object Cancelled : ConnectResult

    data class Failed(val kind: BackupFailure) : ConnectResult
}

sealed interface TokenResult {
    data class Token(val value: String, val account: DriveAccount) : TokenResult {
        override fun toString(): String = "Token"
    }

    /** 권한이 없거나 거둬짐. 상태를 "다시 연결이 필요해요"로 둠 */
    data object NeedsUser : TokenResult

    data class Failed(val kind: BackupFailure) : TokenResult
}

/** 구글 드라이브 권한 받기(contracts/drive-gateway.md). 범위는 `drive.file`뿐이고 서버는 없음 */
interface DriveAuthorizer {
    /** 화면이 필요한 연결. [hint]는 먼저 보일 계정 이메일 */
    suspend fun connect(hint: String?): ConnectResult

    /** 화면 없이 토큰을 받음. 백그라운드 작업에서도 부름 */
    suspend fun accessToken(): TokenResult

    /** 드라이브가 토큰을 거절함. 다음 [accessToken]은 새로 받음 */
    fun invalidateToken() {}

    /** 이 기기의 연결을 끊음. 드라이브의 백업 파일은 지우지 않음(FR-004) */
    suspend fun disconnect()
}
