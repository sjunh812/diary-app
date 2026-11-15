package org.sjhstudio.diary.feature.home

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.feature.home.resources.Res
import org.sjhstudio.diary.feature.home.resources.feedback_card_action
import org.sjhstudio.diary.feature.home.resources.feedback_card_close
import org.sjhstudio.diary.feature.home.resources.feedback_card_title

// 의견 카드: 제목이 보이고, 두 버튼이 각자 할 일을 부르는지
@OptIn(ExperimentalTestApi::class)
class FeedbackCardUiTest {
    @Test
    fun buttons_send_or_close() = runComposeUiTest {
        var sent = 0
        var closed = 0
        var title = ""
        var send = ""
        var close = ""
        setContent {
            title = stringResource(Res.string.feedback_card_title)
            send = stringResource(Res.string.feedback_card_action)
            close = stringResource(Res.string.feedback_card_close)
            DiaryTheme(darkTheme = false) { FeedbackCard(onSend = { sent++ }, onClose = { closed++ }) }
        }
        onNode(hasText(title)).assertExists()
        onNode(hasText(send)).performClick()
        onNode(hasText(close)).performClick()
        assertEquals(1, sent)
        assertEquals(1, closed)
    }
}
