package org.sjhstudio.diary.feature.backup

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
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
import org.sjhstudio.diary.core.model.BackupEntry
import org.sjhstudio.diary.core.model.BackupEntryState
import org.sjhstudio.diary.core.model.BackupFailure
import org.sjhstudio.diary.core.model.BackupStatus
import org.sjhstudio.diary.core.model.PLATFORM_LEGACY
import org.sjhstudio.diary.core.model.PhotoProgress
import org.sjhstudio.diary.feature.backup.resources.Res
import org.sjhstudio.diary.feature.backup.resources.backup_all_done
import org.sjhstudio.diary.feature.backup.resources.backup_auto
import org.sjhstudio.diary.feature.backup.resources.backup_connect_failed_offline
import org.sjhstudio.diary.feature.backup.resources.backup_failed_offline
import org.sjhstudio.diary.feature.backup.resources.backup_finding_title
import org.sjhstudio.diary.feature.backup.resources.backup_line_all
import org.sjhstudio.diary.feature.backup.resources.backup_line_days
import org.sjhstudio.diary.feature.backup.resources.backup_line_failed
import org.sjhstudio.diary.feature.backup.resources.backup_line_receiving
import org.sjhstudio.diary.feature.backup.resources.backup_no_space_title
import org.sjhstudio.diary.feature.backup.resources.backup_now
import org.sjhstudio.diary.feature.backup.resources.backup_options
import org.sjhstudio.diary.feature.backup.resources.backup_stopped_title
import org.sjhstudio.diary.feature.backup.resources.restore_legacy_row
import org.sjhstudio.diary.feature.backup.resources.restore_row

/** 화면이 사실을 말하는지 확인. 실패했는데 다 됐다고 하면 안 됨 */
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

    /** 비행기 모드에서 지금 백업: 예전 성공이 있어도 실패를 말함 */
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

    /** 연결한 뒤 드라이브를 찾는 동안에는 찾는다고 말하고 백업을 막음 */
    @Test
    fun finding_backups_is_shown_and_backup_waits() = runComposeUiTest {
        var title = ""
        var now = ""
        setContent {
            title = stringResource(Res.string.backup_finding_title)
            now = stringResource(Res.string.backup_now)
            Screen(connected.copy(lastSuccessAt = null, findingBackup = true))
        }

        onNodeWithText(title).assertExists()
        onNodeWithText(now, useUnmergedTree = true).assertExists()
        onNode(hasText(now) and hasClickAction()).assertIsNotEnabled()
    }

    /** 사진을 받는 동안 홈이 사진까지 모두 백업했다고 말하면 안 됨 */
    @Test
    fun the_home_line_shows_photos_arriving() = runComposeUiTest {
        var receiving = ""
        var all = ""
        setContent {
            receiving = stringResource(Res.string.backup_line_receiving, "3", "10")
            all = stringResource(Res.string.backup_line_all)
            DiaryTheme(darkTheme = false) { BackupStatusLine(connected, {}, photos = PhotoProgress(3, 10)) }
        }

        onNodeWithText(receiving).assertExists()
        onAllNodesWithText(all, substring = true).assertCountEquals(0)
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

    /** 글은 올렸고 공간 때문에 사진만 남았으면 실패가 아님 */
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

    /** 드라이브 공간이 부족해도 복원, 자동 백업, 백업 설정 길이 사라지면 안 됨 */
    @Test
    fun running_out_of_drive_space_keeps_every_other_path() = runComposeUiTest {
        var title = ""
        var restore = ""
        var auto = ""
        var options = ""
        setContent {
            title = stringResource(Res.string.backup_no_space_title)
            restore = stringResource(Res.string.restore_row)
            auto = stringResource(Res.string.backup_auto)
            options = stringResource(Res.string.backup_options)
            Screen(connected.copy(lastFailure = BackupFailure.NO_SPACE, bytesShort = 2_300_000_000, photosNotBackedUp = 420))
        }

        onNodeWithText(title).assertExists()
        onNodeWithText(restore).assertExists()
        onNodeWithText(auto).assertExists()
        onNodeWithText(options).assertExists()
    }

    @Test
    fun a_failed_connection_says_what_to_do() = runComposeUiTest {
        var offline = ""
        setContent {
            offline = stringResource(Res.string.backup_connect_failed_offline)
            DiaryTheme(darkTheme = false) { BackupConnectFailedSheet(BackupFailure.OFFLINE, {}) }
        }

        onNodeWithText(offline).assertExists()
    }

    /** 예전 앱 백업은 편수를 모름. 0편으로 보이면 빈 백업으로 오해함 */
    @Test
    fun an_old_backup_shows_no_counts() = runComposeUiTest {
        val old =
            BackupEntry(
                "legacy:1", PLATFORM_LEGACY, "", PLATFORM_LEGACY, Clock.System.now() - 100.days, 0, 0, 0, "", BackupEntryState.Ready
            )
        var row = ""
        var counts = ""
        setContent {
            row = stringResource(Res.string.restore_legacy_row)
            counts = countsText(old)
            DiaryTheme(darkTheme = false) { BackupFoundSheet(old, {}, {}) }
        }

        onNodeWithText(row).assertExists()
        onAllNodesWithText(counts).assertCountEquals(0)
    }
}
