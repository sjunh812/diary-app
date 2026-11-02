package org.sjhstudio.diary

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.sjhstudio.diary.app.DiaryApp
import org.sjhstudio.diary.app.Startup
import org.sjhstudio.diary.core.data.SettingsRepository
import org.sjhstudio.diary.core.data.lock.LockRepository
import org.sjhstudio.diary.core.model.ThemeMode

// 잠금 화면의 생체 인증 창(BiometricPrompt)이 FragmentActivity를 요구한다.
class MainActivity : FragmentActivity() {
    private val recents = RecentsGuard(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        // 설정과 잠금을 읽을 때까지만 시작 화면을 붙잡는다(contracts/app-shell.md 시작 순서 2). 일부러 기다리지 않는다.
        val startup = get<Startup>()
        installSplashScreen().setKeepOnScreenCondition { !startup.ready }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch {
            // 앱 화면 모드(시스템·라이트·다크)에 맞춰 상태·내비게이션 막대 아이콘 색을 바꾼다. 앱만 다크일 때 시각이 안 보이지 않게.
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                get<SettingsRepository>().settings.map { it.themeMode }.distinctUntilChanged().collect { mode ->
                    val dark =
                        when (mode) {
                            ThemeMode.SYSTEM -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                                Configuration.UI_MODE_NIGHT_YES

                            ThemeMode.LIGHT -> false

                            ThemeMode.DARK -> true
                        }
                    val style = if (dark) {
                        SystemBarStyle.dark(
                            Color.TRANSPARENT
                        )
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    }
                    enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                get<LockRepository>().settings.map { it.enabled }.distinctUntilChanged().collect(recents::setEnabled)
            }
        }
        setContent { DiaryApp() }
    }

    override fun onPause() {
        recents.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        recents.onResume()
    }
}
