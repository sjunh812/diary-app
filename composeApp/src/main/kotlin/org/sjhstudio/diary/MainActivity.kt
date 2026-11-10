package org.sjhstudio.diary

import android.app.UiModeManager
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalDensity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.sjhstudio.diary.app.DiaryApp
import org.sjhstudio.diary.app.Startup
import org.sjhstudio.diary.app.navigation.AppLaunch
import org.sjhstudio.diary.core.data.SettingsRepository
import org.sjhstudio.diary.core.data.lock.LockRepository
import org.sjhstudio.diary.core.model.ThemeMode
import org.sjhstudio.diary.core.platform.AndroidDriveAuthorizer
import org.sjhstudio.diary.core.platform.DriveConsentLauncher

// 잠금 화면의 생체 인증 창(BiometricPrompt)이 FragmentActivity를 요구함
class MainActivity : FragmentActivity() {
    private val recents = RecentsGuard(this)

    // 구글 드라이브 동의 창
    // 돌아온 결과를 기다리는 쪽에 넘김
    // 창을 보는 동안 화면을 돌리면 결과는 새 화면으로 오니 기다림은 앱과 함께 사는 곳에 둠
    private val consent =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            get<AndroidDriveAuthorizer>().consentResult.finish(result.data.takeIf { result.resultCode == RESULT_OK })
        }

    @Volatile private var insetsKnown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // 설정과 잠금을 읽을 때까지만 시작 화면을 붙잡음(contracts/app-shell.md 시작 순서 2)
        // 일부러 기다리지 않음
        val startup = get<Startup>()
        // 시작 화면은 설정과 잠금을 읽고, 앱 화면이 시스템 막대 높이를 받을 때까지 둠
        // 막대 높이를 받기 전 첫 그림은 화면이 위로 붙었다가 내려앉아 큰 제목이 위에서 떨어지는 것처럼 보임
        // 그동안 앱 화면은 뒤에서 미리 구성되어 따로 기다리는 비용 없음
        installSplashScreen().setKeepOnScreenCondition { !startup.ready || !insetsKnown }
        lifecycleScope.launch {
            // 막대가 없는 창에서도 시작 화면에 갇히지 않게 짧게만 기다림
            delay(INSETS_WAIT_MILLIS)
            insetsKnown = true
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch {
            // 앱 화면 모드(시스템·라이트·다크)에 맞춰 상태·내비게이션 막대 아이콘 색을 바꿈
            // 앱만 다크일 때 시각이 안 보이지 않게
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                get<SettingsRepository>().settings.map { it.themeMode }.distinctUntilChanged().collect { mode ->
                    keepSplashInMode(mode)
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
        val authorizer = get<AndroidDriveAuthorizer>()
        authorizer.consent =
            DriveConsentLauncher { intent ->
                authorizer.consentResult.await { consent.launch(IntentSenderRequest.Builder(intent).build()) }
            }
        setContent {
            DiaryApp()
            InsetsProbe { insetsKnown = true }
        }
        // 화면을 돌려 다시 만들어질 때는 쌓인 화면이 이미 복원됨
        if (savedInstanceState == null) handleIntent(intent)
    }

    /**
     * 스플래시는 앱 코드보다 먼저 시스템이 그려 앱 화면 모드를 모름
     * Android 12부터 앱 모드를 시스템에 넘겨 다음 시작부터 맞춤. 11 이하는 휴대폰 모드를 따름
     * 모드가 지금 화면과 다르면 화면을 한 번 다시 만듦
     */
    private fun keepSplashInMode(mode: ThemeMode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val night =
            when (mode) {
                ThemeMode.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
                ThemeMode.LIGHT -> UiModeManager.MODE_NIGHT_NO
                ThemeMode.DARK -> UiModeManager.MODE_NIGHT_YES
            }
        getSystemService(UiModeManager::class.java)?.setApplicationNightMode(night)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    /**
     * 알림이 보낸 요청만 받음
     * 이 화면은 밖에 열려 있지 않아, 이름을 곧바로 가리킨 인텐트는 앱이 만든 것
     * 런처는 옛 이름(.SplashActivity)으로 들어옴
     * 최근 앱에서 다시 열면 시스템이 처음 인텐트를 또 주니 건너뜀
     */
    private fun handleIntent(intent: Intent) {
        val fromRecents = intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0
        val ours = intent.component?.className == MainActivity::class.java.name
        if (intent.action == ACTION_WRITE && ours && !fromRecents) get<AppLaunch>().requestWrite()
    }

    override fun onDestroy() {
        get<AndroidDriveAuthorizer>().consent = null
        super.onDestroy()
    }

    override fun onPause() {
        recents.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        recents.onResume()
    }

    companion object {
        /** 매일 알림을 눌렀을 때 */
        const val ACTION_WRITE = "org.sjhstudio.diary.action.WRITE"
    }
}

/** 앱 화면이 시스템 막대 높이를 받으면 [onKnow] 호출 */
@Composable
private fun InsetsProbe(onKnow: () -> Unit) {
    val density = LocalDensity.current
    val bars = WindowInsets.systemBars
    val known = bars.getTop(density) > 0 || bars.getBottom(density) > 0
    val latest by rememberUpdatedState(onKnow)
    LaunchedEffect(known) { if (known) latest() }
}

private const val INSETS_WAIT_MILLIS = 300L
