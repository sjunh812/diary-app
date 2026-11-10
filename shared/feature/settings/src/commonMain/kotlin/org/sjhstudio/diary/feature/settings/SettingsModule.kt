package org.sjhstudio.diary.feature.settings

import kotlinx.datetime.TimeZone
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.sjhstudio.diary.core.migration.MigrationFollowUp

val settingsFeatureModule =
    module {
        viewModel {
            // MigrationFollowUp은 앱 모듈이 일기 DB와 함께 만듦
            val followUp = get<MigrationFollowUp>()
            MigrationReportViewModel(get(), TimeZone.currentSystemDefault(), retryDeferred = followUp::retryDeferredNow)
        }
        viewModel { SettingsViewModel(get(), get(), { showsMigrationReport(get()) }, get()) }
        viewModel { ReminderSettingsViewModel(get(), get(), get(), get()) }
        viewModel { FontSettingsViewModel(get(), get(), get()) }
        viewModel { LicensesViewModel() }
        viewModel { FontReplacedNoticeViewModel(get()) }
    }
