package org.sjhstudio.diary.feature.backup

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.BackupConnection
import org.sjhstudio.diary.core.model.BackupFailure
import org.sjhstudio.diary.core.model.BackupStatus
import org.sjhstudio.diary.feature.backup.resources.Res
import org.sjhstudio.diary.feature.backup.resources.backup_all_done
import org.sjhstudio.diary.feature.backup.resources.backup_failed_offline
import org.sjhstudio.diary.feature.backup.resources.backup_line_all
import org.sjhstudio.diary.feature.backup.resources.backup_line_days
import org.sjhstudio.diary.feature.backup.resources.backup_line_failed
import org.sjhstudio.diary.feature.backup.resources.backup_stopped_title

/** 화면이 사실을 말하는지 본다. 실패했는데 다 됐다고 하면 안 된다. */
@OptIn(ExperimentalTestApi::class)
class BackupStatusUiTest {
    private val connected =
        BackupStatus(
            connection = BackupConnection.Connected("diary@gmail.com"),
            lastSuccessAt = Clock.System.now() - 1.days,
            autoEnabled = true
        )

    @Composable
    private fun Screen(status: BackupStatus) = DiaryTheme(darkTheme = false) {
        BackupSettingsScreen(status, false, {}, {}, {}, {}, {}, {}, {})
    }

    /** 비행기 모드에서 지금 백업: 예전 성공이 있어도 실패를 말한다. */
    @Test
    fun a_failed_attempt_is_not_reported_as_all_backed_up() = runComposeUiTest {
        var allDone = ""
        var title = ""
        var reason = ""
        setContent {
            allDone = stringResource(Res.string.backup_all_done)
            title = stringResource(Res.string.backup_stopped_title)
            reason = stringResource(Res.string.backup_failed_offline)
            Screen(connected.copy(lastFailure = BackupFailure.OFFLINE, failingSince = Clock.System.now()))
        }

        onNodeWithText(title).assertExists()
        onNode(hasText(reason, substring = true)).assertExists()
        onAllNodesWithText(allDone).assertCountEquals(0)
    }

    @Test
    fun a_success_still_says_all_backed_up() = runComposeUiTest {
        var allDone = ""
        setContent {
            allDone = stringResource(Res.string.backup_all_done)
            Screen(connected)
        }

        onNodeWithText(allDone).assertExists()
    }

    @Test
    fun the_home_line_says_the_last_attempt_failed() = runComposeUiTest {
        var failed = ""
        var all = ""
        setContent {
            failed = stringResource(Res.string.backup_line_failed)
            all = stringResource(Res.string.backup_line_all)
            DiaryTheme(darkTheme = false) {
                BackupStatusLine(connected.copy(lastFailure = BackupFailure.DRIVE_ERROR, failingSince = Clock.System.now()), onOpen = {})
            }
        }

        onNodeWithText(failed).assertExists()
        onAllNodesWithText(all, substring = true).assertCountEquals(0)
    }

    /** 글은 올렸고 공간 때문에 사진만 남았으면 실패가 아니다. */
    @Test
    fun the_home_line_keeps_the_success_when_only_photos_wait() = runComposeUiTest {
        var failed = ""
        setContent {
            failed = stringResource(Res.string.backup_line_failed)
            DiaryTheme(darkTheme = false) {
                BackupStatusLine(connected.copy(lastFailure = BackupFailure.NO_SPACE, photosNotBackedUp = 3), onOpen = {})
            }
        }

        onAllNodesWithText(failed).assertCountEquals(0)
    }

    @Test
    fun a_week_of_failure_keeps_the_days_since_the_last_backup() = runComposeUiTest {
        var days = ""
        setContent {
            days = stringResource(Res.string.backup_line_days, 8)
            DiaryTheme(darkTheme = false) {
                BackupStatusLine(
                    connected.copy(
                        lastSuccessAt = Clock.System.now() - 8.days,
                        lastFailure = BackupFailure.OFFLINE,
                        failingSince = Clock.System.now() - 8.days
                    ),
                    onOpen = {}
                )
            }
        }

        onNodeWithText(days).assertExists()
    }
}
