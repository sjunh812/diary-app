package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertTrue
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme

// 팁 글은 낱말 가운데서 줄이 끊기지 않게 그릴 때만 붙임 글자를 넣음.
// 화면 읽기와 시험에는 원래 글이 가야 함
@OptIn(ExperimentalTestApi::class)
class DiaryTipTest {
    @Test
    fun screen_readers_get_the_original_text_and_close_still_works() = runComposeUiTest {
        val text = "기분을 누르면 해당 일기를 모아 볼 수 있어요."
        var closed = false
        setContent {
            DiaryTheme(darkTheme = false) {
                // 390 폭 휴대폰처럼 두 줄로 넘어가는 폭
                Box(Modifier.width(350.dp)) { DiaryTip(text, "닫기", onClose = { closed = true }) }
            }
        }
        onNodeWithText(text).assertIsDisplayed()
        onNodeWithContentDescription("닫기").performClick()
        assertTrue(closed)
    }
}
