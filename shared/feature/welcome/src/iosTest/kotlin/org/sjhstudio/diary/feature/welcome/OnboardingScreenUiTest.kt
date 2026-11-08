package org.sjhstudio.diary.feature.welcome

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.feature.welcome.resources.Res
import org.sjhstudio.diary.feature.welcome.resources.onboarding_later
import org.sjhstudio.diary.feature.welcome.resources.onboarding_start

// 업데이트 사용자의 백업 장에서 [나중에]가 연결과 엇갈리지 않는지 본다.
@OptIn(ExperimentalTestApi::class)
class OnboardingScreenUiTest {
    @Test
    fun later_waits_while_the_drive_is_connecting() = runComposeUiTest {
        var later = ""
        setContent {
            later = stringResource(Res.string.onboarding_later)
            DiaryTheme(darkTheme = false) {
                OnboardingScreen(updated = true, connected = false, connecting = true, onConnect = {}, onFinish = {})
            }
        }

        onNode(hasText(later)).assertIsNotEnabled()
    }

    @Test
    fun later_moves_on_without_connecting() = runComposeUiTest {
        var later = ""
        var start = ""
        var connects = 0
        setContent {
            later = stringResource(Res.string.onboarding_later)
            start = stringResource(Res.string.onboarding_start)
            DiaryTheme(darkTheme = false) {
                OnboardingScreen(updated = true, connected = false, connecting = false, onConnect = { connects++ }, onFinish = {})
            }
        }

        onNode(hasText(later)).assertIsEnabled().performClick()
        waitForIdle()

        onNode(hasText(start)).assertExists()
        assertEquals(0, connects)
    }
}
