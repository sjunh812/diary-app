package org.sjhstudio.diary.feature.settings

import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.sjhstudio.diary.core.data.BackupRepository
import org.sjhstudio.diary.core.data.PlanRepository
import org.sjhstudio.diary.core.data.diary.TrashRepository
import org.sjhstudio.diary.core.migration.MigrationFollowUp

val settingsFeatureModule =
    module {
        viewModel {
            // MigrationFollowUp은 앱 모듈이 일기 DB와 함께 만듦
            val followUp = get<MigrationFollowUp>()
            MigrationReportViewModel(get(), TimeZone.currentSystemDefault(), retryDeferred = followUp::retryDeferredNow)
        }
        viewModel {
            SettingsViewModel(
                get(),
                get(),
                trashCount = get<TrashRepository>().count(),
                plan = get<PlanRepository>().plan,
                showsMigrationReport = { showsMigrationReport(get()) },
                lastBackupAt = get<BackupRepository>().status.map { it.lastSuccessAt }.distinctUntilChanged(),
                telemetry = get()
            )
        }
        viewModel { ReminderSettingsViewModel(get(), get(), get(), get()) }
        viewModel { FontSettingsViewModel(get(), get(), get()) }
        viewModel { LicensesViewModel() }
        viewModel { FontReplacedNoticeViewModel(get()) }
    }
