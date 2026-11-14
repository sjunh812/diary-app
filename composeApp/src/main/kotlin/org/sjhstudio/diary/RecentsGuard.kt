package org.sjhstudio.diary

import android.app.Activity
import android.os.Build
import android.view.WindowManager

/**
 * 잠금이 켜져 있으면 최근 앱 화면에 일기가 비치지 않게 함(FR-018, research R13)
 * Android 13부터는 최근 앱 미리보기만 끔
 * 그 아래는 화면을 떠날 때(onPause) `FLAG_SECURE`를 켜고 돌아오면(onResume) 끔
 * 쓰는 동안의 화면 캡처는 막지 않음
 * 잠금이 꺼져 있으면 아무것도 하지 않음
 */
class RecentsGuard(private val activity: Activity) {
    private var enabled = false

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) activity.setRecentsScreenshotEnabled(!enabled)
    }

    fun onPause() {
        if (enabled &&
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        ) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    fun onResume() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}
