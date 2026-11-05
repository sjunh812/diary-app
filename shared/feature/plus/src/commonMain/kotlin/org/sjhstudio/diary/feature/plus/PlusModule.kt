package org.sjhstudio.diary.feature.plus

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val plusFeatureModule =
    module {
        viewModel { PlusViewModel(get(), get()) }
    }
