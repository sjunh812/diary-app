package org.sjhstudio.diary.feature.calendar

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val calendarFeatureModule =
    module {
        viewModel { CalendarViewModel(get(), get(), get()) }
    }
