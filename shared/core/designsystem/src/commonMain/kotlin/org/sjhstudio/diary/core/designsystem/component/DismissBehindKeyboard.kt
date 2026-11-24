package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

/**
 * 시트·대화상자가 뜰 때 뒤 화면의 초점을 풀고 키보드를 내림
 * 시트·대화상자 안은 따로 그려지는 창이라 이 함수는 그 창 밖에서 부름
 * 닫은 뒤 초점은 돌려주지 않음(사용자 결정 2026-11-25)
 */
@Composable
internal fun DismissBehindKeyboard() {
    val behindFocus = LocalFocusManager.current
    val behindKeyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        behindFocus.clearFocus()
        behindKeyboard?.hide()
    }
}
