package org.sjhstudio.diary.app

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import okio.Path.Companion.toPath
import org.koin.compose.koinInject
import org.sjhstudio.diary.app.gate.LockGate
import org.sjhstudio.diary.app.gate.MigrationGate
import org.sjhstudio.diary.app.scaffold.AppScaffold
import org.sjhstudio.diary.core.data.SettingsRepository
import org.sjhstudio.diary.core.data.photo.ThumbnailRepository
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.AppSettings
import org.sjhstudio.diary.core.model.ThemeMode
import org.sjhstudio.diary.core.ui.ProvideDiaryImageLoader

/**
 * 안드로이드와 iOS가 같이 띄우는 앱의 뿌리(contracts/app-shell.md 시작 순서 3).
 * `DiaryTheme` → 잠금 관문 → 이전 관문 → 앱 틀 순서다.
 * 홈·달력·돌아보기 탭의 화면은 스펙 002가 채운다.
 */
@Composable
fun DiaryApp() {
    val thumbnails = koinInject<ThumbnailRepository>()
    ProvideDiaryImageLoader { thumbnails.thumbnail(it.toPath())?.toString() }
    val startup = koinInject<Startup>()
    val repository = koinInject<SettingsRepository>()
    // 시작 단계에서 읽은 설정으로 첫 프레임부터 화면 모드를 맞춘다.
    val initial = (startup.state.value as? StartupState.Ready)?.settings ?: AppSettings()
    val settings by repository.settings.collectAsStateWithLifecycle(initialValue = initial)
    val darkTheme =
        when (settings.themeMode) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
    DiaryTheme(darkTheme = darkTheme) {
        LockGate {
            MigrationGate { AppScaffold() }
        }
    }
}
