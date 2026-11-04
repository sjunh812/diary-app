package org.sjhstudio.diary.feature.welcome

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val welcomeFeatureModule =
    module {
        viewModel { WelcomeViewModel(get(), get(), get()) }
        viewModel { WhatsNewViewModel(get()) }
    }
