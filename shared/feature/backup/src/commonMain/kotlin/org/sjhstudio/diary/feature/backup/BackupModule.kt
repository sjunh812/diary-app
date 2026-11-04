package org.sjhstudio.diary.feature.backup

import org.jetbrains.compose.resources.getString
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.sjhstudio.diary.feature.backup.resources.Res
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
