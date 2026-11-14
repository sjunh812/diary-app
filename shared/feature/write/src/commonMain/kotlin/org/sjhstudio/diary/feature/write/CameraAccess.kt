package org.sjhstudio.diary.feature.write

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

/** 촬영 권한 확인과 이 앱의 설정 열기 */
@Stable
interface CameraAccess {
    /**
     * 사용자가 카메라를 막아 둔 상태
     * 아직 묻지 않았으면 거짓. 촬영 화면을 띄울 때 시스템이 직접 물음
     */
    fun blocked(): Boolean

    fun openSettings()
}

@Composable
internal expect fun rememberCameraAccess(): CameraAccess
