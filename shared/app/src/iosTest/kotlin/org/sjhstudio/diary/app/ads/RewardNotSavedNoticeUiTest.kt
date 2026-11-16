package org.sjhstudio.diary.app.ads

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.app.resources.Res
import org.sjhstudio.diary.app.resources.reward_not_saved
import org.sjhstudio.diary.core.data.RewardResult
import org.sjhstudio.diary.core.designsystem.component.DiaryToastHost
import org.sjhstudio.diary.core.designsystem.component.DiaryToastState
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme

/** 받은 보상을 못 적으면 토스트가 실제로 보이고, 한 번만 알림 */
@OptIn(ExperimentalTestApi::class)
class RewardNotSavedNoticeUiTest {
    @Test
    fun the_toast_stays_on_screen_after_the_notice_is_marked_shown() = runComposeUiTest {
        val scope = MainScope()
        val removal = AdRemoval(scope) { RewardResult.EarnedNotSaved }
        val viewModel = AdRemovalViewModel(removal)
        val toast = DiaryToastState()
        var message = ""
        setContent {
            message = stringResource(Res.string.reward_not_saved)
            DiaryTheme(darkTheme = false) {
                RewardNotSavedNotice(viewModel, toast)
                DiaryToastHost(toast)
            }
        }

        removal.watch()
        waitUntil(timeoutMillis = 2_000) { onAllNodes(hasText(message)).fetchSemanticsNodes().isNotEmpty() }
        assertFalse(removal.state.value.notSaved, "한 번 알리면 다시 띄우지 않음")
        scope.cancel()
    }
}
