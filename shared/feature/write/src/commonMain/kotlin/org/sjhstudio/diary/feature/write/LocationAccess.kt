package org.sjhstudio.diary.feature.write

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

/** 위치 허용을 묻는 시스템 창과 휴대폰 설정 열기(FR-005) */
@Stable
interface LocationAccess {
    /** 아직 묻지 않았으면 묻고, 끝나면 허용 여부와 상관없이 [onDone] 호출 */
    fun request(onDone: () -> Unit)

    /** [locationOff]면 기기 위치 설정, 아니면 이 앱의 설정을 엶 */
    fun openSettings(locationOff: Boolean)
}

@Composable
internal expect fun rememberLocationAccess(): LocationAccess
