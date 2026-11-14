package org.sjhstudio.diary.core.ui

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** 안드로이드에는 따로 된 설정이 없어 접근성 "애니메이션 삭제"가 켜는 움직임 배율 0을 봄 */
@Composable
actual fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}
