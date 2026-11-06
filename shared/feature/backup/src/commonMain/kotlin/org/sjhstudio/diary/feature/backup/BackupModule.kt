package org.sjhstudio.diary.feature.backup

import org.jetbrains.compose.resources.getString
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.sjhstudio.diary.core.model.BackupFailure
import org.sjhstudio.diary.core.model.BackupProgress
import org.sjhstudio.diary.core.model.BackupStep
import org.sjhstudio.diary.core.model.PhotoRun
import org.sjhstudio.diary.core.ui.grouped
import org.sjhstudio.diary.feature.backup.resources.Res
import org.sjhstudio.diary.feature.backup.resources.backup_alert_auth
import org.sjhstudio.diary.feature.backup.resources.backup_alert_offline
import org.sjhstudio.diary.feature.backup.resources.backup_alert_other
import org.sjhstudio.diary.feature.backup.resources.backup_alert_space
import org.sjhstudio.diary.feature.backup.resources.backup_alert_title
import org.sjhstudio.diary.feature.backup.resources.backup_channel
import org.sjhstudio.diary.feature.backup.resources.backup_done_body
import org.sjhstudio.diary.feature.backup.resources.backup_done_left
import org.sjhstudio.diary.feature.backup.resources.backup_done_title
import org.sjhstudio.diary.feature.backup.resources.backup_readme_body
import org.sjhstudio.diary.feature.backup.resources.backup_running
import org.sjhstudio.diary.feature.backup.resources.backup_running_photos
import org.sjhstudio.diary.feature.backup.resources.backup_stopped_title
import org.sjhstudio.diary.feature.backup.resources.restore_notice_body
import org.sjhstudio.diary.feature.backup.resources.restore_notice_left
import org.sjhstudio.diary.feature.backup.resources.restore_notice_some
import org.sjhstudio.diary.feature.backup.resources.restore_notice_stopped_title
import org.sjhstudio.diary.feature.backup.resources.restore_notice_title
import org.sjhstudio.diary.feature.backup.resources.restore_photo_progress
import org.sjhstudio.diary.feature.backup.resources.restore_running_title

val backupFeatureModule =
    module {
        viewModel { BackupViewModel(get(), get()) }
        viewModel { RestoreListViewModel(get()) }
        viewModel { parameters -> RestorePreviewViewModel(parameters.get(), get()) }
        viewModel { RestoreProgressViewModel(get()) }
        viewModel { RestoreStatusViewModel(get()) }
        viewModel { FileBackupViewModel(get()) }
    }

/** 드라이브 폴더에 함께 올리는 안내 글. */
suspend fun backupReadme(): String = getString(Res.string.backup_readme_body)

/**
 * 7일 실패 알림의 제목과 본문.
 * 일기 내용과 개수는 넣지 않는다(FR-016).
 */
suspend fun backupAlertText(failure: BackupFailure?): Pair<String, String> {
    val body =
        when (failure) {
            BackupFailure.AUTH -> Res.string.backup_alert_auth
            BackupFailure.NO_SPACE -> Res.string.backup_alert_space
            BackupFailure.OFFLINE -> Res.string.backup_alert_offline
            else -> Res.string.backup_alert_other
        }
    return getString(Res.string.backup_alert_title) to getString(body)
}

/**
 * 앱을 나가 있는 동안 지금 백업이 끝났을 때의 알림.
 * 일기 내용은 넣지 않는다(헌법 II).
 */
suspend fun backupDoneText(photosLeft: Int): Pair<String, String> {
    val body = if (photosLeft > 0) getString(Res.string.backup_done_left, photosLeft.grouped()) else getString(Res.string.backup_done_body)
    return getString(Res.string.backup_done_title) to body
}

/** 앱을 나가 있는 동안 지금 백업이 멈췄을 때의 알림. */
suspend fun backupStoppedText(failure: BackupFailure?): Pair<String, String> =
    getString(Res.string.backup_stopped_title) to backupAlertText(failure).second

/**
 * 앱을 나가 있는 동안 복원이 끝났을 때의 알림.
 * 실제로 받은 수와 남은 수로 말한다.
 */
suspend fun restoreDoneText(run: PhotoRun): Pair<String, String> = when {
    run.left > 0 -> getString(Res.string.restore_notice_stopped_title) to getString(Res.string.restore_notice_left)
    run.complete -> getString(Res.string.restore_notice_title) to getString(Res.string.restore_notice_body)
    else -> getString(Res.string.restore_notice_title) to getString(Res.string.restore_notice_some)
}

/** 지금 백업 진행 알림의 제목. */
suspend fun backupProgressTitle(): String = getString(Res.string.backup_running)

/**
 * 진행 알림의 본문.
 * 사진을 올리는 동안만 숫자를 보인다.
 * 일기 내용은 넣지 않는다(헌법 II).
 */
suspend fun backupProgressText(progress: BackupProgress): String? = if (progress.step == BackupStep.UploadingPhotos) {
    getString(Res.string.backup_running_photos, progress.photosDone, progress.photosTotal)
} else {
    null
}

/** 복원 사진을 받는 동안의 알림 제목. */
suspend fun restoreProgressTitle(): String = getString(Res.string.restore_running_title)

/**
 * 복원 사진을 받는 동안의 알림 본문.
 * 장수만 넣는다(헌법 II).
 */
suspend fun restoreProgressText(done: Int, total: Int): String =
    getString(Res.string.restore_photo_progress, done.grouped(), total.grouped())

/** 알림 채널 이름. */
suspend fun backupChannelName(): String = getString(Res.string.backup_channel)
