package org.sjhstudio.diary.feature.lock

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val lockFeatureModule =
    module {
        viewModel { LockViewModel(get(), telemetry = get()) }
        viewModel { PinSetupViewModel(get()) }
        viewModel { LockSettingsViewModel(get(), get()) }
    }
