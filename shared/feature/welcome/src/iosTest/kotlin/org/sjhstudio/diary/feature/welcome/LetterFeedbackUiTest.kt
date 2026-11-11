package org.sjhstudio.diary.feature.welcome

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.feature.welcome.resources.Res
import org.sjhstudio.diary.feature.welcome.resources.letter_feedback

// 의견 보내기는 다시 읽는 편지에만 있음
@OptIn(ExperimentalTestApi::class)
class LetterFeedbackUiTest {
    @Test
    fun the_letter_read_again_sends_feedback() = runComposeUiTest {
        var feedback = ""
        var sent = 0
        setContent {
            feedback = stringResource(Res.string.letter_feedback)
            DiaryTheme(darkTheme = false) { FullLetterScreen(1204, onBack = {}, onFeedback = { sent++ }) }
        }

        onNode(hasText(feedback)).performScrollTo().performClick()
        waitForIdle()

        assertEquals(1, sent)
    }

    @Test
    fun the_first_letter_has_no_feedback_button() = runComposeUiTest {
        var feedback = ""
        setContent {
            feedback = stringResource(Res.string.letter_feedback)
            DiaryTheme(darkTheme = false) { LetterScreen(1204, onStart = {}) }
        }

        onNode(hasText(feedback)).assertDoesNotExist()
    }
}
