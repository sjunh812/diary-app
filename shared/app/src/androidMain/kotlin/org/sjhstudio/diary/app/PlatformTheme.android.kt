package org.sjhstudio.diary.app

import android.app.UiModeManager
import android.graphics.Color
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.drawable.toDrawable
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.ThemeMode

/**
 * 상태·내비게이션 막대 글자색과 창 바탕을 앱 화면 모드에 맞춤
 * 창은 화면 모드가 바뀌어도 다시 만들지 않음(매니페스트 `uiMode`). 다시 만들면 화면이 한 번 더 깜빡였음
 */
@Composable
internal actual fun ApplyPlatformTheme(mode: ThemeMode, dark: Boolean) {
    val activity = LocalActivity.current as? ComponentActivity ?: return
    val canvas = DiaryTheme.colors.canvas
    LaunchedEffect(activity, dark) {
        val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        activity.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }
    // 키보드가 오르내리는 틈처럼 앱 화면이 덮지 않는 순간에 보이는 바탕
    LaunchedEffect(activity, canvas) { activity.window.setBackgroundDrawable(canvas.toArgb().toDrawable()) }
    // 시작 화면은 앱 코드보다 먼저 시스템이 그려 앱 모드를 모름. Android 12부터 앱 모드를 넘겨 다음 시작부터 맞춤
    LaunchedEffect(activity, mode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return@LaunchedEffect
        val night =
            when (mode) {
                ThemeMode.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
                ThemeMode.LIGHT -> UiModeManager.MODE_NIGHT_NO
                ThemeMode.DARK -> UiModeManager.MODE_NIGHT_YES
            }
        activity.getSystemService(UiModeManager::class.java)?.setApplicationNightMode(night)
    }
}
