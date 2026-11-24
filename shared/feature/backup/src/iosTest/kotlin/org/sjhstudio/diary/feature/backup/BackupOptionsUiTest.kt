package org.sjhstudio.diary.feature.backup

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.BackupConnection
import org.sjhstudio.diary.core.model.BackupStatus

/** 백업 설정의 구글 계정 줄(스펙 011) */
@OptIn(ExperimentalTestApi::class)
class BackupOptionsUiTest {
    private fun runScreen(connection: BackupConnection, onChange: () -> Unit) = runComposeUiTest {
        setContent {
            DiaryTheme(darkTheme = false) {
                BackupOptionsScreen(BackupStatus(connection), onChangeAccount = onChange, onPhotosOnCellular = {
                }, onDisconnect = {}, onBack = {})
            }
        }
        onNodeWithText(EMAIL, useUnmergedTree = true).assertExists()
        onNodeWithText(EMAIL).assertHasClickAction().performClick()
    }

    @Test
    fun tapping_the_account_asks_for_another_account() {
        var changes = 0
        runScreen(BackupConnection.Connected(EMAIL)) { changes++ }
        assertEquals(1, changes)
    }

    /** 연결이 끊겨도 어느 계정이었는지 보이고 바꿀 수 있음 */
    @Test
    fun the_account_stays_visible_while_reconnect_is_needed() {
        var changes = 0
        runScreen(BackupConnection.NeedsReconnect(EMAIL)) { changes++ }
        assertEquals(1, changes)
    }

    private companion object {
        const val EMAIL = "diary@gmail.com"
    }
}
