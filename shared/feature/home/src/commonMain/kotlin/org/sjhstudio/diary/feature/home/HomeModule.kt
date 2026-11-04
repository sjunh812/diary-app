package org.sjhstudio.diary.feature.home

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val homeFeatureModule =
    module {
        viewModel { HomeViewModel(get(), get(), get()) }
        viewModel { (entryId: String) -> EntryDetailViewModel(entryId, get(), get(), get()) }
    }
