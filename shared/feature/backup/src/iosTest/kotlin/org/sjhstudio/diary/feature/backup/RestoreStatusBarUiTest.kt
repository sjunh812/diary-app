package org.sjhstudio.diary.feature.backup

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.PhotoStop
import org.sjhstudio.diary.core.model.RollbackRun
import org.sjhstudio.diary.core.model.WaitingPhotos
import org.sjhstudio.diary.feature.backup.resources.Res
import org.sjhstudio.diary.feature.backup.resources.restore_wait_now
import org.sjhstudio.diary.feature.backup.resources.restore_wait_other_account

// 다른 계정이 연결돼 멈춘 복원 사진은 사진이 온 계정을 말하고, 눌러도 나아지지 않는 [지금 받기]는 두지 않음
@OptIn(ExperimentalTestApi::class)
class RestoreStatusBarUiTest {
    @Test
    fun photos_on_another_account_name_that_account_without_fetch_now() = runComposeUiTest {
        var body = ""
        var fetch = ""
        setContent {
            body = stringResource(Res.string.restore_wait_other_account, "first@gmail.com")
            fetch = stringResource(Res.string.restore_wait_now)
            DiaryTheme(darkTheme = false) {
                RestoreStatusBar(WaitingPhotos(2, PhotoStop.OTHER_ACCOUNT, "first@gmail.com"), RollbackRun.IDLE, {}, {}, {})
            }
        }
        onNode(hasText(body)).assertExists()
        onNode(hasText(fetch)).assertDoesNotExist()
    }
}
