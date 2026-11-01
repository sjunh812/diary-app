package org.sjhstudio.diary.feature.migration

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val migrationFeatureModule =
    module {
        viewModel { MigrationViewModel(get()) }
    }
