package org.sjhstudio.diary.core.model

import kotlin.time.Instant

/** 백업이 실패한 까닭(contracts/backup-flow.md "실패의 종류"). 이름이 `last_failure`에 그대로 저장된다. */
enum class BackupFailure {
    AUTH,
    NO_SPACE,
    OFFLINE,
    DRIVE_ERROR,
    LOCAL_IO,
    UNKNOWN
    ;

    companion object {
        fun fromName(name: String?): BackupFailure? = entries.firstOrNull { it.name == name }
    }
}

/** 드라이브 연결 상태. */
sealed interface BackupConnection {
    data object NotConnected : BackupConnection

    data class Connected(val accountLabel: String) : BackupConnection

    data object NeedsReconnect : BackupConnection
}

/** 백업 상태 기계의 단계(data-model.md). 이름이 `backup/state.json`에 그대로 저장된다. */
enum class BackupStep {
    Preparing,
    Snapshotting,
    UploadingPhotos,
    UploadingSnapshot,
    UploadingManifest,
    Verifying,
    Pruning,
    Done,
    Failed
}

data class BackupProgress(
    val step: BackupStep,
    val photosDone: Int = 0,
    val photosTotal: Int = 0,
    val bytesDone: Long = 0,
    val bytesTotal: Long = 0
)

/** 홈과 설정이 같이 보는 백업 상태. 보이는 시각은 목록 파일까지 확인한 마지막 성공이다. */
data class BackupStatus(
    val connection: BackupConnection = BackupConnection.NotConnected,
    val lastSuccessAt: Instant? = null,
    val photosNotBackedUp: Int = 0,
    val running: BackupProgress? = null,
    val failingSince: Instant? = null,
    val lastFailure: BackupFailure? = null,
    val autoEnabled: Boolean = false,
    val photosOnCellular: Boolean = false
)

/** 복원 목록 한 줄의 상태. */
sealed interface BackupEntryState {
    data object Ready : BackupEntryState

    data class Damaged(val reason: DamageReason) : BackupEntryState

    data object NeedsNewerApp : BackupEntryState
}

enum class DamageReason { MANIFEST, SNAPSHOT_MISSING, SNAPSHOT_MISMATCH }

/** 복원 목록 한 줄. 일기 내용은 없다. */
data class BackupEntry(
    val snapshotName: String,
    val deviceId: String,
    val deviceName: String,
    val platform: String,
    val createdAt: Instant,
    val entryCount: Int,
    val photoCount: Int,
    val photosNotInBackup: Int,
    val appVersion: String,
    val state: BackupEntryState
)

/** 1.7.x가 드라이브에 올린 `note.db`. */
data class LegacyBackupEntry(val driveFileId: String, val modifiedAt: Instant, val byteSize: Long)

/** 합치기 전에 보이는 숫자(FR-023). */
data class RestorePreview(
    val entriesAdded: Int,
    val entriesUpdated: Int,
    val entriesKeptBoth: Int,
    val entriesUnchanged: Int,
    val photosToDownload: Int,
    val bytesToDownload: Long,
    val photosNotInBackup: Int,
    val settingsChange: Boolean,
    val deviceBytesFree: Long
)

data class RestoreResult(
    val entriesAdded: Int,
    val entriesUpdated: Int,
    val entriesKeptBoth: Int,
    val photosRestored: Int,
    val photosMissing: Int,
    val photosPending: Int,
    val rollbackUntil: Instant? = null
)

/** 권하기 카드(FR-036). */
data class BackupNudge(val entryCount: Int, val photoCount: Int, val suggestedAccount: String? = null)
