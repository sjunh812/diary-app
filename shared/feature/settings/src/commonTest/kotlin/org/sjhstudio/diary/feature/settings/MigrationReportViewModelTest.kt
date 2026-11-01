package org.sjhstudio.diary.feature.settings

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.sjhstudio.diary.core.migration.LegacyMigration
import org.sjhstudio.diary.core.migration.NoLegacyMigration
import org.sjhstudio.diary.core.model.MigrationOutcome
import org.sjhstudio.diary.core.model.MigrationReport
import org.sjhstudio.diary.core.model.MigrationState
import org.sjhstudio.diary.core.model.MigrationStep

@OptIn(ExperimentalCoroutinesApi::class)
class MigrationReportViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    // 2026-10-31 15:30 UTC는 서울로 2026-11-01 00:30이다.
    private val report =
        MigrationReport(
            entriesTotal = 1_296, entriesMigrated = 1_284, entriesUnreadable = 12, photosFound = 1_998, photosMissing = 33,
            dateDiffers = 2, dateUnparsed = 0, thumbnailsFailed = 0, settingsMigrated = emptyList(),
            finishedAt = Instant.parse("2026-10-31T15:30:00Z"), outcome = MigrationOutcome.PARTIAL
        )

    private fun migration(step: MigrationStep, result: MigrationReport?) = object : LegacyMigration by NoLegacyMigration {
        override fun state(): MigrationState = MigrationState(step = step)

        override fun report(): MigrationReport? = result
    }

    /** 옮긴 날은 기기 시간대의 날짜다. */
    @Test
    fun report_and_local_day_are_shown() = runTest(dispatcher) {
        val vm = MigrationReportViewModel(migration(MigrationStep.PARTIAL, report), TimeZone.of("Asia/Seoul"), dispatcher)
        advanceUntilIdle()
        assertEquals(report, vm.state.value.report)
        assertEquals(LocalDate(2026, 11, 1), vm.state.value.migratedOn)
    }

    @Test
    fun missing_report_leaves_the_screen_empty() = runTest(dispatcher) {
        val vm = MigrationReportViewModel(migration(MigrationStep.PARTIAL, null), TimeZone.UTC, dispatcher)
        advanceUntilIdle()
        assertNull(vm.state.value.report)
    }

    /** 설정 줄은 일부만 옮겨진 기기에서만 둔다. */
    @Test
    fun settings_row_only_for_partial() {
        assertTrue(showsMigrationReport(migration(MigrationStep.PARTIAL, report)))
        assertFalse(showsMigrationReport(migration(MigrationStep.DONE, report)))
        assertFalse(showsMigrationReport(NoLegacyMigration))
    }
}
