package org.sjhstudio.diary.core.backup

import org.sjhstudio.diary.core.datastore.BackupStore
import org.sjhstudio.diary.core.platform.DriveAuthorizer
import org.sjhstudio.diary.core.platform.TokenResult

/**
 * 연결해 둔 계정의 토큰만 내줌
 * 로그인이 다른 계정이면 다시 연결로 보내 백업·복원·정리가 화면의 계정과 다른 드라이브를 쓰지 않게 함
 * 연결 전에는 그대로 내줌
 */
class ConnectedAccountAuthorizer(private val real: DriveAuthorizer, private val store: BackupStore) : DriveAuthorizer by real {
    override suspend fun accessToken(): TokenResult {
        val token = real.accessToken()
        if (token !is TokenResult.Token) return token
        val connected = store.current().accountKey
        return if (connected == null || connected == token.account.key) token else TokenResult.NeedsUser
    }
}
