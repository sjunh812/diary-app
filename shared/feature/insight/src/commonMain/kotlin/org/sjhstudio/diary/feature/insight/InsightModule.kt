package org.sjhstudio.diary.feature.insight

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val insightFeatureModule =
    module {
        viewModel { InsightViewModel(get(), plans = get(), saved = get(), tips = get(), entries = get()) }
        viewModel { AdvancedStatsViewModel(get()) }
    }
