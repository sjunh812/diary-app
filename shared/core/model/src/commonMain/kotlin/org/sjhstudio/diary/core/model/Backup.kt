package org.sjhstudio.diary.core.model

import kotlin.time.Instant

/**
 * 백업이 실패한 까닭(contracts/backup-flow.md "실패의 종류").
 * 이름이 `last_failure`에 그대로 저장된다.
 */
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

/**
 * 백업 상태 기계의 단계(data-model.md).
 * 이름이 `backup/state.json`에 그대로 저장된다.
 */
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

/**
 * 홈과 설정이 같이 보는 백업 상태.
 * 보이는 시각은 목록 파일까지 확인한 마지막 성공이다.
 */
data class BackupStatus(
    val connection: BackupConnection = BackupConnection.NotConnected,
    val lastSuccessAt: Instant? = null,
    val photosNotBackedUp: Int = 0,
    val running: BackupProgress? = null,
    val failingSince: Instant? = null,
    val lastFailure: BackupFailure? = null,
    val autoEnabled: Boolean = false,
    val photosOnCellular: Boolean = false,
    /** 공간이 모자라 못 올린 사진의 양. */
    val bytesShort: Long = 0,
    /** 드라이브 백업을 되살렸으면 그 백업을 만든 시각. */
    val restoredBackupAt: Instant? = null
)

/** 복원 목록 한 줄의 상태. */
sealed interface BackupEntryState {
    data object Ready : BackupEntryState

    data class Damaged(val reason: DamageReason) : BackupEntryState

    data object NeedsNewerApp : BackupEntryState
}

enum class DamageReason { MANIFEST, SNAPSHOT_MISSING, SNAPSHOT_MISMATCH }

/** 예전 앱 백업 줄의 [BackupEntry.platform] 값. */
const val PLATFORM_LEGACY = "legacy"

/**
 * 복원 목록 한 줄.
 * 일기 내용은 없다.
 */
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
    val deviceBytesFree: Long,
    /**
     * 지금 이 기기에 보이는 일기 수.
     * 바꾸기 확인에 쓴다.
     */
    val entriesHere: Int = 0
)

data class RestoreResult(
    val entriesAdded: Int,
    val entriesUpdated: Int,
    val entriesKeptBoth: Int,
    val photosRestored: Int,
    val photosMissing: Int,
    val photosPending: Int,
    val rollbackUntil: Instant? = null,
    /**
     * 공간이 모자라 백업에 없던 사진.
     * 원래 기기에 있다.
     */
    val photosOnOriginal: Int = 0,
    /**
     * 백업한 기기가 잠금을 쓰고 있었다.
     * 비밀번호는 백업하지 않는다.
     */
    val lockWasOn: Boolean = false,
    /**
     * 1.7.x가 올린 `note.db`를 불러왔다.
     * 사진 자리는 찾을 수 없는 사진으로 남는다.
     */
    val fromLegacy: Boolean = false,
    /**
     * 합치지 않고 이 백업으로 바꿨다.
     * 지금 일기가 그대로라고 말하면 안 된다.
     */
    val replaced: Boolean = false,
    /**
     * 파일에서 복원했다.
     * 파일에 없는 사진은 앱을 다시 열어도 저절로 오지 않는다.
     */
    val fromFile: Boolean = false
)

enum class RestoreFailure { MISMATCH, NO_SPACE, NEEDS_UPDATE, OTHER }

/**
 * 파일로 만들 묶음의 크기.
 * 파일 크기는 어림값이다.
 */
data class BundleEstimate(val entries: Int, val photos: Int, val bytes: Long)

/** 사용자가 고른 파일의 미리보기를 찾는 이름. */
const val RESTORE_FROM_FILE = "file"

/**
 * 바꾸기를 되돌릴 수 있는 동안의 정보.
 * 일기 수만 담는다.
 */
data class RollbackInfo(val until: Instant, val entriesBefore: Int, val entriesAfter: Int)

/** 스냅샷을 받아 확인한 결과. */
sealed interface RestorePreparation {
    data class Ready(val entry: BackupEntry, val preview: RestorePreview) : RestorePreparation

    data class Failed(val reason: RestoreFailure) : RestorePreparation
}

/**
 * 복원하기 진행.
 * 화면을 떠나도 앱이 켜져 있으면 계속 간다.
 */
sealed interface RestoreState {
    data object Idle : RestoreState

    data object Merging : RestoreState

    data class FetchingPhotos(val entries: Int, val done: Int, val total: Int) : RestoreState

    data class Done(val result: RestoreResult) : RestoreState

    data class Failed(val reason: RestoreFailure) : RestoreState
}

/** 권하기 카드(FR-036). */
data class BackupNudge(val entryCount: Int, val photoCount: Int, val suggestedAccount: String? = null)

/**
 * 사진 받기 진행.
 * 복원 화면이 닫혀 있어도 알림으로 보인다.
 */
data class PhotoProgress(val done: Int, val total: Int)
