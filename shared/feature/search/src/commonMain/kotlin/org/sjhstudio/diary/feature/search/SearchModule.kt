package org.sjhstudio.diary.feature.search

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.sjhstudio.diary.core.model.SearchFilter

val searchFeatureModule =
    module {
        viewModel { (filter: SearchFilter) -> SearchViewModel(get(), filter, get(), get()) }
    }
