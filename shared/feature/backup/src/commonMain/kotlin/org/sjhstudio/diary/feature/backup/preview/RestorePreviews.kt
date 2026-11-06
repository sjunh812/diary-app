package org.sjhstudio.diary.feature.backup.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlin.time.Instant
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.BackupEntry
import org.sjhstudio.diary.core.model.BackupEntryState
import org.sjhstudio.diary.core.model.BackupFailure
import org.sjhstudio.diary.core.model.DamageReason
import org.sjhstudio.diary.core.model.PLATFORM_LEGACY
import org.sjhstudio.diary.core.model.RestoreFailure
import org.sjhstudio.diary.core.model.RestorePreparation
import org.sjhstudio.diary.core.model.RestorePreview
import org.sjhstudio.diary.core.model.RestoreResult
import org.sjhstudio.diary.core.model.RestoreState
import org.sjhstudio.diary.feature.backup.BackupConnectFailedSheet
import org.sjhstudio.diary.feature.backup.BackupFoundSheet
import org.sjhstudio.diary.feature.backup.RestoreListScreen
import org.sjhstudio.diary.feature.backup.RestoreListUi
import org.sjhstudio.diary.feature.backup.RestorePreviewScreen
import org.sjhstudio.diary.feature.backup.RestoreProgressScreen

private val Sample = BackupEntry(
    "a.snapshot",
    "phone",
    "Galaxy S24",
    "android",
    Instant.parse("2026-10-31T12:30:00Z"),
    1204,
    860,
    0,
    "2.0.0",
    BackupEntryState.Ready
)

private val Numbers = RestorePreview(120, 8, 2, 1074, 340, 1_200_000_000, 0, true, 8_000_000_000)

@Composable
private fun List(dark: Boolean, state: RestoreListUi) = DiaryTheme(darkTheme = dark) { RestoreListScreen(state, {}, {}, {}) }

@Composable
private fun Check(dark: Boolean, state: RestorePreparation?) = DiaryTheme(darkTheme = dark) { RestorePreviewScreen(state, {}, {}, {}) }

@Composable
private fun Progress(dark: Boolean, state: RestoreState) = DiaryTheme(darkTheme = dark) { RestoreProgressScreen(state, {}, {}, {}, {}, {}) }

private val Listed =
    RestoreListUi.Loaded(listOf(Sample, Sample.copy(snapshotName = "b", state = BackupEntryState.Damaged(DamageReason.SNAPSHOT_MISSING))))

@Preview(locale = "ko")
@Composable
private fun ListLight() = List(false, Listed)

@Preview(locale = "ko")
@Composable
private fun ListDark() = List(true, Listed)

@Preview(locale = "ko")
@Composable
private fun ListEmptyLight() = List(false, RestoreListUi.Loaded(emptyList()))

@Preview(locale = "ko")
@Composable
private fun ListFailedLight() = List(false, RestoreListUi.Failed)

@Preview(locale = "ko")
@Composable
private fun CheckLight() = Check(false, RestorePreparation.Ready(Sample, Numbers))

@Preview(locale = "ko")
@Composable
private fun CheckDark() = Check(true, RestorePreparation.Ready(Sample, Numbers))

@Preview(locale = "ko")
@Composable
private fun CheckNoSpaceLight() = Check(false, RestorePreparation.Ready(Sample, Numbers.copy(deviceBytesFree = 200_000_000)))

@Preview(locale = "ko")
@Composable
private fun CheckMismatchLight() = Check(false, RestorePreparation.Failed(RestoreFailure.MISMATCH))

@Preview(locale = "ko")
@Composable
private fun RunningLight() = Progress(false, RestoreState.FetchingPhotos(1324, 120, 340))

@Preview(locale = "ko")
@Composable
private fun RunningDark() = Progress(true, RestoreState.FetchingPhotos(1324, 120, 340))

private val Result = RestoreResult(120, 8, 2, 328, 0, 0, photosOnOriginal = 12, lockWasOn = true)

@Preview(locale = "ko")
@Composable
private fun ResultLight() = Progress(false, RestoreState.Done(Result))

@Preview(locale = "ko")
@Composable
private fun ResultDark() = Progress(true, RestoreState.Done(Result))

@Preview(locale = "ko")
@Composable
private fun FoundLight() = DiaryTheme(darkTheme = false) { BackupFoundSheet(Sample, {}, {}) }

@Preview(name = "찾은 백업 · 예전 앱", locale = "ko")
@Composable
private fun FoundLegacyLight() = DiaryTheme(darkTheme = false) {
    BackupFoundSheet(Sample.copy(platform = PLATFORM_LEGACY, entryCount = 0, photoCount = 0), {}, {})
}

@Preview(name = "연결 실패 · 인터넷 없음", locale = "ko")
@Composable
private fun ConnectFailedLight() = DiaryTheme(darkTheme = false) { BackupConnectFailedSheet(BackupFailure.OFFLINE, {}) }

@Preview(name = "연결 실패 · 계정 · 다크", locale = "ko")
@Composable
private fun ConnectFailedDark() = DiaryTheme(darkTheme = true) { BackupConnectFailedSheet(BackupFailure.AUTH, {}) }
