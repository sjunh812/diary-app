package org.sjhstudio.diary.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.Path.Companion.toPath
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.sjhstudio.diary.app.gate.FirstScreenShown
import org.sjhstudio.diary.app.gate.LockGate
import org.sjhstudio.diary.app.gate.MigrationGate
import org.sjhstudio.diary.app.gate.WelcomeGate
import org.sjhstudio.diary.app.navigation.ProvideTabViewModelStores
import org.sjhstudio.diary.app.scaffold.AppScaffold
import org.sjhstudio.diary.core.data.PlanRepository
import org.sjhstudio.diary.core.data.SettingsRepository
import org.sjhstudio.diary.core.data.photo.ThumbnailRepository
import org.sjhstudio.diary.core.designsystem.component.ProvideScreenGutter
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.AppSettings
import org.sjhstudio.diary.core.model.DiaryFont
import org.sjhstudio.diary.core.model.ThemeMode
import org.sjhstudio.diary.core.platform.FontInstaller
import org.sjhstudio.diary.core.ui.ProvideDiaryImageLoader
import org.sjhstudio.diary.core.ui.diaryBodyFontFamily
import org.sjhstudio.diary.core.ui.rememberFileFontFamily
import org.sjhstudio.diary.core.ui.rememberThemeBrand

/** 고른 글꼴은 앱 전체에 씀(헌법 V). 기본 글꼴이면 null이라 테마가 언어에 맞는 글꼴을 고름 */
@Composable
private fun chosenFontFamily(font: DiaryFont): FontFamily? {
    val installer = koinInject<FontInstaller>()
    val changes by installer.changes.collectAsStateWithLifecycle()
    // 화면 스레드에서 파일을 보지 않음. 시작 때 확인해 둔 값을 먼저 쓰고, 글꼴이나 받은 파일이 바뀌면 뒤에서 확인
    val path by produceState(installer.knownPath(font.id), font, changes) {
        value = withContext(Dispatchers.IO) { installer.installedPath(font.id) }
    }
    val installed = rememberFileFontFamily(path)
    val family = diaryBodyFontFamily(font) { installed }
    return family.takeUnless { font == DiaryFont.PRETENDARD }
}

/**
 * 안드로이드와 iOS가 같이 띄우는 앱의 뿌리(contracts/app-shell.md 시작 순서 3)
 * `DiaryTheme` → 잠금 관문 → 이전 관문 → 앱 틀 순
 * 홈·달력·돌아보기 탭의 화면은 스펙 002가 채움
 */
@Composable
fun DiaryApp() {
    val thumbnails = koinInject<ThumbnailRepository>()
    ProvideDiaryImageLoader { thumbnails.thumbnail(it.toPath())?.toString() }
    val startup = koinInject<Startup>()
    // 첫 화면 표시는 이 앱 화면의 것. 화면이 사라지면 되돌려 다음 화면도 첫 화면을 기다림
    DisposableEffect(startup) { onDispose(startup::screenGone) }
    val repository = koinInject<SettingsRepository>()
    // 시작 단계에서 읽은 설정으로 첫 프레임부터 화면 모드를 맞춤
    val initial = (startup.state.value as? StartupState.Ready)?.settings ?: AppSettings()
    val settings by repository.settings.collectAsStateWithLifecycle(initialValue = initial)
    val darkTheme =
        when (settings.themeMode) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
    // 요금제는 기기에 남긴 값이라 첫 프레임부터 Plus의 직접 고른 색이 보임
    val plan by koinInject<PlanRepository>().plan.collectAsStateWithLifecycle()
    val brand = rememberThemeBrand(settings.theme, customAllowed = plan.customTheme)
    DiaryTheme(darkTheme = darkTheme, fontFamily = chosenFontFamily(settings.font), brand = brand) {
        ApplyPlatformTheme(settings.themeMode, darkTheme)
        CompositionLocalProvider(LocalChosenFont provides settings.font) {
            ProvideScreenGutter {
                ProvideTabViewModelStores {
                    LockGate {
                        MigrationGate {
                            // 일기 DB는 이전 관문을 지난 뒤에만 열어 정리와 백업 확인도 여기서 시작
                            val root = koinViewModel<AppRootViewModel>()
                            LifecycleResumeEffect(root) {
                                root.resumed()
                                onPauseOrDispose {}
                            }
                            WelcomeGate {
                                FirstScreenShown()
                                AppScaffold()
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 사용자가 고른 글꼴. 앱 글자 밖(iOS 광고 카드)도 같은 값을 씀 */
internal val LocalChosenFont = staticCompositionLocalOf { DiaryFont.DEFAULT }
