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
import org.sjhstudio.diary.core.migration.MigrationResult
import org.sjhstudio.diary.core.migration.NoLegacyMigration
import org.sjhstudio.diary.core.model.MigrationFailureKind
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
        override suspend fun state(): MigrationState = MigrationState(step = step)

        override suspend fun report(): MigrationReport? = result
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

    /**
     * 미룬 기기는 다시 시도를 보이고, 실패하면 알린다.
     * 앱은 그대로 쓴다.
     */
    @Test
    fun deferred_retry_shows_failure_then_result() = runTest(dispatcher) {
        var step = MigrationStep.DEFERRED
        var result: MigrationResult = MigrationResult.Failed(MigrationStep.COPYING, MigrationFailureKind.WRITE_ERROR)
        val migration =
            object : LegacyMigration by NoLegacyMigration {
                override suspend fun state(): MigrationState = MigrationState(step = step)

                override suspend fun report(): MigrationReport? = report.takeIf { step != MigrationStep.DEFERRED }
            }
        val vm = MigrationReportViewModel(migration, TimeZone.UTC, dispatcher) { result }
        advanceUntilIdle()
        assertTrue(vm.state.value.deferred)

        vm.retry()
        advanceUntilIdle()
        assertEquals(MigrationRetry.Failed, vm.state.value.retry)

        result = MigrationResult.Finished(report, showResult = false)
        step = MigrationStep.PARTIAL
        vm.retry()
        advanceUntilIdle()
        assertFalse(vm.state.value.deferred)
        assertEquals(report, vm.state.value.report)
    }

    /** 설정 줄은 일부만 옮겨졌거나 미룬 기기에서만 둔다. */
    @Test
    fun settings_row_only_for_partial_or_deferred() = runTest {
        assertTrue(showsMigrationReport(migration(MigrationStep.PARTIAL, report)))
        assertTrue(showsMigrationReport(migration(MigrationStep.DEFERRED, null)))
        assertFalse(showsMigrationReport(migration(MigrationStep.DONE, report)))
        assertFalse(showsMigrationReport(NoLegacyMigration))
    }
}
