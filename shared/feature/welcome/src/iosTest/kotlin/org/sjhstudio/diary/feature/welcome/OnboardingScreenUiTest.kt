package org.sjhstudio.diary.feature.welcome

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.feature.welcome.resources.Res
import org.sjhstudio.diary.feature.welcome.resources.onboarding_next
import org.sjhstudio.diary.feature.welcome.resources.onboarding_skip
import org.sjhstudio.diary.feature.welcome.resources.onboarding_start
import org.sjhstudio.diary.feature.welcome.resources.onboarding_tablet_title

// 업데이트 사용자 다섯 장, 새 사용자 네 장. 마지막은 태블릿
@OptIn(ExperimentalTestApi::class)
class OnboardingScreenUiTest {
    private fun goesThrough(updated: Boolean, pages: Int) = runComposeUiTest {
        var next = ""
        var start = ""
        var tablet = ""
        var finished = 0
        setContent {
            next = stringResource(Res.string.onboarding_next)
            start = stringResource(Res.string.onboarding_start)
            tablet = stringResource(Res.string.onboarding_tablet_title)
            DiaryTheme(darkTheme = false) { OnboardingScreen(updated = updated, onFinish = { finished++ }) }
        }

        repeat(pages - 1) {
            onNode(hasText(next)).performClick()
            waitForIdle()
        }
        onNode(hasText(tablet)).assertExists()
        onNode(hasText(start)).performClick()
        waitForIdle()

        assertEquals(1, finished)
    }

    @Test
    fun an_updated_user_sees_five_pages_ending_with_tablet() = goesThrough(updated = true, pages = 5)

    @Test
    fun a_new_user_sees_four_pages_ending_with_tablet() = goesThrough(updated = false, pages = 4)

    @Test
    fun skip_finishes_at_once() = runComposeUiTest {
        var skip = ""
        var finished = 0
        setContent {
            skip = stringResource(Res.string.onboarding_skip)
            DiaryTheme(darkTheme = false) { OnboardingScreen(updated = true, onFinish = { finished++ }) }
        }

        onNode(hasText(skip)).performClick()
        waitForIdle()

        assertEquals(1, finished)
    }
}
