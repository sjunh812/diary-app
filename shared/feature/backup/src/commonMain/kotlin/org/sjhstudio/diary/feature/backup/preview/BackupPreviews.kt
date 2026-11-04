package org.sjhstudio.diary.feature.backup.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlin.time.Instant
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.BackupConnection
import org.sjhstudio.diary.core.model.BackupStatus
import org.sjhstudio.diary.feature.backup.BackupSettingsScreen

private val Connected =
    BackupStatus(BackupConnection.Connected("diary@gmail.com"), Instant.parse("2026-11-04T12:30:00Z"), autoEnabled = true)

@Composable
private fun Screen(status: BackupStatus, dark: Boolean) = DiaryTheme(darkTheme = dark) {
    BackupSettingsScreen(status, connecting = false, onConnect = {
    }, onBackUp = {}, onCellular = {}, onAuto = {}, onOptions = {}, onBack = {})
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
