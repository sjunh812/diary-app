package org.sjhstudio.diary.feature.backup

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.RestoreFailure
import org.sjhstudio.diary.core.model.RestoreState
import org.sjhstudio.diary.feature.backup.resources.Res
import org.sjhstudio.diary.feature.backup.resources.restore_retry
import org.sjhstudio.diary.feature.backup.resources.restore_update_in_store

// 새 버전이 필요한 복원은 다시 시도 대신 스토어로 보냄(스펙 009 US3)
@OptIn(ExperimentalTestApi::class)
class RestoreNeedsUpdateUiTest {
    @Test
    fun needs_update_opens_the_store_instead_of_retrying() = runComposeUiTest {
        var updates = 0
        var retries = 0
        var update = ""
        var retry = ""
        setContent {
            update = stringResource(Res.string.restore_update_in_store)
            retry = stringResource(Res.string.restore_retry)
            DiaryTheme(darkTheme = false) {
                RestoreProgressScreen(
                    RestoreState.Failed(RestoreFailure.NEEDS_UPDATE),
                    onSeeEntries = {},
                    onStart = {},
                    onLock = {},
                    onRetry = { retries++ },
                    onPickOther = {},
                    onUpdate = { updates++ }
                )
            }
        }
        onNode(hasText(retry)).assertDoesNotExist()
        onNode(hasText(update)).performClick()
        assertEquals(1, updates)
        assertEquals(0, retries)
    }

    @Test
    fun other_failures_still_retry() = runComposeUiTest {
        var retries = 0
        var retry = ""
        setContent {
            retry = stringResource(Res.string.restore_retry)
            DiaryTheme(darkTheme = false) {
                RestoreProgressScreen(
                    RestoreState.Failed(RestoreFailure.OTHER),
                    onSeeEntries = {},
                    onStart = {},
                    onLock = {},
                    onRetry = { retries++ },
                    onPickOther = {}
                )
            }
        }
        onNode(hasText(retry)).performClick()
        assertEquals(1, retries)
    }
}
