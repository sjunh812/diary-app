package org.sjhstudio.diary.drivecheck

import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.common.api.Scope

// 스펙 003 research R2: 로그인 없이 AuthorizationClient로 drive.file만 받고, 계정은 openid·email로 확인
object DriveAuth {
    val request: AuthorizationRequest = AuthorizationRequest.builder()
        .setRequestedScopes(
            listOf(
                Scope("https://www.googleapis.com/auth/drive.file"),
                Scope("openid"),
                Scope("email"),
            ),
        )
        .build()

    // 다른 클라이언트가 만든 파일도 찾도록 Android·iOS 확인 빌드가 같은 질의 사용
    const val PROBE_FILES = "appProperties has { key='diary.kind' and value='probe-file' } and trashed = false"
    const val PROBE_ROOTS = "appProperties has { key='diary.kind' and value='probe-root' } and trashed = false"
    const val LEGACY_BACKUP = "name = 'note.db' and trashed = false"
}
