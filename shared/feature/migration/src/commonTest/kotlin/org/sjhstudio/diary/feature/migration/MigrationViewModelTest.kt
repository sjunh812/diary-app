package org.sjhstudio.diary.feature.migration

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.sjhstudio.diary.core.migration.FollowUpResult
import org.sjhstudio.diary.core.migration.LegacyMigration
import org.sjhstudio.diary.core.migration.MigrationResult
import org.sjhstudio.diary.core.migration.NoLegacyMigration
import org.sjhstudio.diary.core.model.MigrationOutcome
import org.sjhstudio.diary.core.model.MigrationReport

@OptIn(ExperimentalCoroutinesApi::class)
class MigrationViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private val report =
        MigrationReport(
            entriesTotal = 4, entriesMigrated = 4, entriesUnreadable = 0, photosFound = 3, photosMissing = 1, dateDiffers = 0,
            dateUnparsed = 0, thumbnailsFailed = 0, settingsMigrated = emptyList(), finishedAt = Instant.fromEpochMilliseconds(0),
            outcome = MigrationOutcome.DONE
        )

    /** 옮길 것이 이미 끝난 기기: 앱을 열면 뒷일을 한 번 하고 찾은 것만 알린다. */
    @Test
    fun finished_device_runs_follow_up_once_and_notifies() = runTest(dispatcher) {
        var calls = 0
        val vm =
            MigrationViewModel(NoLegacyMigration, followUp = {
                calls++
                FollowUpResult(entriesRecovered = 2, photosFound = 0)
            }, background = dispatcher)
        val notices = mutableListOf<MigrationNotice>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.notices.toList(notices) }
        advanceUntilIdle()
        assertEquals(MigrationUiState.Passed, vm.state.value)
        assertEquals(1, calls)
        assertEquals(listOf<MigrationNotice>(MigrationNotice.EntriesRecovered(2)), notices)
    }

    /** 방금 옮긴 기기: 결과 화면을 닫은 뒤에야 뒷일을 하고, 시작하기를 여러 번 눌러도 한 번이다. */
    @Test
    fun follow_up_waits_for_the_result_screen() = runTest(dispatcher) {
        var calls = 0
        val migration =
            object : LegacyMigration by NoLegacyMigration {
                override fun needsMigration(): Boolean = true

                override suspend fun run(): MigrationResult = MigrationResult.Finished(report, showResult = true)
            }
        val vm =
            MigrationViewModel(migration, followUp = {
                calls++
                FollowUpResult(photosFound = 1)
            }, background = dispatcher)
        val notices = mutableListOf<MigrationNotice>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.notices.toList(notices) }
        advanceUntilIdle()
        assertIs<MigrationUiState.Result>(vm.state.value)
        assertEquals(0, calls)

        vm.acknowledgeResult()
        vm.acknowledgeResult()
        advanceUntilIdle()
        assertEquals(MigrationUiState.Passed, vm.state.value)
        assertEquals(1, calls)
        assertEquals(listOf<MigrationNotice>(MigrationNotice.PhotosFound(1)), notices)
    }

    /** 뒷일이 실패해도 앱 틀로 가고 아무것도 알리지 않는다. */
    @Test
    fun failing_follow_up_does_not_block_the_app() = runTest(dispatcher) {
        val vm = MigrationViewModel(NoLegacyMigration, followUp = { error("읽기 실패") }, background = dispatcher)
        val notices = mutableListOf<MigrationNotice>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.notices.toList(notices) }
        advanceUntilIdle()
        assertEquals(MigrationUiState.Passed, vm.state.value)
        assertEquals(emptyList(), notices)
    }
}
