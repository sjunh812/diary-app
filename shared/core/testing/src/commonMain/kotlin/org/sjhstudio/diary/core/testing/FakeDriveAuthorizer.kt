package org.sjhstudio.diary.core.testing

import org.sjhstudio.diary.core.platform.ConnectResult
import org.sjhstudio.diary.core.platform.DriveAccount
import org.sjhstudio.diary.core.platform.DriveAuthorizer
import org.sjhstudio.diary.core.platform.TokenResult

/** 드라이브 권한 대역. 계정을 바꾸거나 권한을 거둔 상태를 시험에서 만든다. */
class FakeDriveAuthorizer(var account: DriveAccount? = DriveAccount("sub-1", "diary@gmail.com")) : DriveAuthorizer {
    var connectResult: ConnectResult? = null
    var tokenResult: TokenResult? = null
    var tokensIssued = 0
        private set
    var disconnected = false
        private set
    var invalidated = 0
        private set

    override suspend fun connect(hint: String?): ConnectResult =
        connectResult ?: account?.let { ConnectResult.Connected(it) } ?: ConnectResult.Cancelled

    override suspend fun accessToken(): TokenResult {
        tokenResult?.let { return it }
        val current = account ?: return TokenResult.NeedsUser
        tokensIssued++
        return TokenResult.Token("token-$tokensIssued", current)
    }

    override fun invalidateToken() {
        invalidated++
    }

    override suspend fun disconnect() {
        disconnected = true
        account = null
    }
}
