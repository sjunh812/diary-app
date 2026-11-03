package org.sjhstudio.diary.feature.settings

import kotlinx.datetime.TimeZone
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val settingsFeatureModule =
    module {
        viewModel { MigrationReportViewModel(get(), TimeZone.currentSystemDefault()) }
        viewModel { SettingsViewModel(get(), get(), { showsMigrationReport(get()) }, get()) }
        viewModel { ReminderSettingsViewModel(get(), get(), get(), get()) }
    }
