package org.sjhstudio.diary.feature.migration

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.sjhstudio.diary.core.migration.MigrationFollowUp

val migrationFeatureModule =
    module {
        // MigrationFollowUp은 일기 DB를 알아서 앱 모듈(shared/app)이 만든다.
        viewModel { MigrationViewModel(get(), get<MigrationFollowUp>()::run) }
        viewModel { LegacyReaderViewModel(get()) }
    }
