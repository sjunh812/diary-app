package org.sjhstudio.diary.feature.trash

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val trashFeatureModule =
    module {
        viewModel { TrashViewModel(get()) }
    }
