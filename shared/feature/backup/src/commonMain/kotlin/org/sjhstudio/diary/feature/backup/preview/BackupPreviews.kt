package org.sjhstudio.diary.feature.backup.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlin.time.Instant
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.BackupConnection
import org.sjhstudio.diary.core.model.BackupFailure
import org.sjhstudio.diary.core.model.BackupNudge
import org.sjhstudio.diary.core.model.BackupStatus
import org.sjhstudio.diary.feature.backup.BackupNudgeContent
import org.sjhstudio.diary.feature.backup.BackupSettingsScreen
import org.sjhstudio.diary.feature.backup.BackupStatusFailedLine
import org.sjhstudio.diary.feature.backup.BackupStatusRead

private val Connected =
    BackupStatus(BackupConnection.Connected("diary@gmail.com"), Instant.parse("2026-11-04T12:30:00Z"), autoEnabled = true)

@Composable
private fun Screen(status: BackupStatus, dark: Boolean, read: BackupStatusRead = BackupStatusRead.Loaded) = DiaryTheme(darkTheme = dark) {
    BackupSettingsScreen(status, connecting = false, onConnect = {
    }, onBackUp = {}, onCellular = {}, onAuto = {}, onOptions = {}, onRestore = {}, onBack = {}, statusRead = read)
}

@Preview(locale = "ko")
@Composable
private fun NotConnectedLight() = Screen(BackupStatus(), dark = false)

@Preview(locale = "ko")
@Composable
private fun NotConnectedDark() = Screen(BackupStatus(), dark = true)

@Preview(locale = "ko")
@Composable
private fun ConnectedLight() = Screen(Connected, dark = false)

@Preview(locale = "ko")
@Composable
private fun ConnectedDark() = Screen(Connected, dark = true)

@Preview(locale = "ko")
@Composable
private fun PhotosWaitingLight() = Screen(Connected.copy(photosNotBackedUp = 3), dark = false)

@Preview(locale = "ko")
@Composable
private fun NeedsReconnectLight() = Screen(Connected.copy(connection = BackupConnection.NeedsReconnect), dark = false)

@Preview(locale = "ko")
@Composable
private fun NudgeLight() = DiaryTheme(darkTheme = false) { BackupNudgeContent(BackupNudge(1204, 860), {}, {}) }

@Preview(locale = "ko")
@Composable
private fun NudgeDark() = DiaryTheme(darkTheme = true) { BackupNudgeContent(BackupNudge(3, 0), {}, {}) }

@Preview(locale = "ko")
@Composable
private fun NoSpaceLight() =
    Screen(Connected.copy(lastFailure = BackupFailure.NO_SPACE, photosNotBackedUp = 420, bytesShort = 2_300_000_000), dark = false)

@Preview(locale = "ko")
@Composable
private fun NoSpaceDark() =
    Screen(Connected.copy(lastFailure = BackupFailure.NO_SPACE, photosNotBackedUp = 420, bytesShort = 2_300_000_000), dark = true)

private val Failed = Connected.copy(lastFailure = BackupFailure.OFFLINE, failingSince = Instant.parse("2026-11-05T12:30:00Z"))

@Preview(name = "마지막 시도 실패", locale = "ko")
@Composable
private fun FailedLight() = Screen(Failed, dark = false)

@Preview(name = "마지막 시도 실패 · 다크", locale = "ko")
@Composable
private fun FailedDark() = Screen(Failed, dark = true)

@Preview(name = "첫 백업 실패", locale = "ko")
@Composable
private fun FirstFailedLight() = Screen(Failed.copy(lastSuccessAt = null), dark = false)

@Preview(name = "상태를 읽지 못함", locale = "ko")
@Composable
private fun StatusFailedLight() = Screen(BackupStatus(), dark = false, read = BackupStatusRead.Failed)

@Preview(name = "상태를 읽지 못함 · 다크", locale = "ko")
@Composable
private fun StatusFailedDark() = Screen(BackupStatus(), dark = true, read = BackupStatusRead.Failed)

@Preview(name = "홈 줄 · 상태를 읽지 못함", locale = "ko")
@Composable
private fun StatusFailedLineLight() = DiaryTheme(darkTheme = false) { BackupStatusFailedLine({}) }

@Preview(name = "홈 줄 · 상태를 읽지 못함 · 다크", locale = "ko")
@Composable
private fun StatusFailedLineDark() = DiaryTheme(darkTheme = true) { BackupStatusFailedLine({}) }
