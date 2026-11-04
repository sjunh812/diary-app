package org.sjhstudio.diary.feature.backup

import org.jetbrains.compose.resources.getString
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.sjhstudio.diary.core.model.BackupFailure
import org.sjhstudio.diary.feature.backup.resources.Res
import org.sjhstudio.diary.feature.backup.resources.backup_alert_auth
import org.sjhstudio.diary.feature.backup.resources.backup_alert_offline
import org.sjhstudio.diary.feature.backup.resources.backup_alert_other
import org.sjhstudio.diary.feature.backup.resources.backup_alert_space
import org.sjhstudio.diary.feature.backup.resources.backup_alert_title
import org.sjhstudio.diary.feature.backup.resources.backup_channel
import org.sjhstudio.diary.feature.backup.resources.backup_readme_body

val backupFeatureModule =
    module {
        viewModel { BackupViewModel(get(), get()) }
        viewModel { RestoreListViewModel(get()) }
        viewModel { parameters -> RestorePreviewViewModel(parameters.get(), get()) }
        viewModel { RestoreProgressViewModel(get()) }
    }

/** 드라이브 폴더에 함께 올리는 안내 글. */
suspend fun backupReadme(): String = getString(Res.string.backup_readme_body)

/** 7일 실패 알림의 제목과 본문. 일기 내용과 개수는 넣지 않는다(FR-016). */
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

/** 알림 채널 이름. */
suspend fun backupChannelName(): String = getString(Res.string.backup_channel)
