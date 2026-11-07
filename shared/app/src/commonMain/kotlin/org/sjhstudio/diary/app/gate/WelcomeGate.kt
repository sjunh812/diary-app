package org.sjhstudio.diary.app.gate

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.WelcomeStep
import org.sjhstudio.diary.feature.backup.BackupConnectFailedSheet
import org.sjhstudio.diary.feature.welcome.LetterScreen
import org.sjhstudio.diary.feature.welcome.OnboardingScreen
import org.sjhstudio.diary.feature.welcome.WelcomeViewModel

/**
 * 이전 관문을 지난 뒤의 편지와 온보딩(스펙 004 FR-001, FR-002).
 * 끝나면 [content]로 간다.
 * 광고, 가격, 권한 요청이 없는 구간이다(FR-005).
 */
@Composable
fun WelcomeGate(content: @Composable () -> Unit) {
    val viewModel = koinViewModel<WelcomeViewModel>()
    val step by viewModel.step.collectAsStateWithLifecycle()
    val connected by viewModel.connected.collectAsStateWithLifecycle()
    val connecting by viewModel.connecting.collectAsStateWithLifecycle()
    val connectFailure by viewModel.connectFailure.collectAsStateWithLifecycle()
    when (step) {
        null -> Box(Modifier.fillMaxSize().background(DiaryTheme.colors.canvas))

        WelcomeStep.DONE -> content()

        WelcomeStep.LETTER -> LetterScreen(viewModel.entries, viewModel::letterRead)

        WelcomeStep.ONBOARDING_UPDATED, WelcomeStep.ONBOARDING_NEW -> {
            OnboardingScreen(step == WelcomeStep.ONBOARDING_UPDATED, connected, connecting, viewModel::connect, viewModel::finish)
            connectFailure?.let { BackupConnectFailedSheet(it, viewModel::connectFailureShown) }
        }
    }
}
