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
import org.sjhstudio.diary.core.model.MigrationFailureKind
import org.sjhstudio.diary.core.model.MigrationOutcome
import org.sjhstudio.diary.core.model.MigrationReport
import org.sjhstudio.diary.core.model.MigrationStep

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

    /** 옮길 것이 이미 끝난 기기: 앱을 열면 후속 작업을 한 번 하고 찾은 것만 알린다. */
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

    /** 방금 옮긴 기기: 결과 화면을 닫은 뒤에야 후속 작업을 하고, 시작하기를 여러 번 눌러도 한 번이다. */
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

    /** 이전이 예외를 던져도 앱을 끄지 않고 실패 화면을 보인다. */
    @Test
    fun a_crash_in_the_migration_lands_on_the_failure_screen() = runTest(dispatcher) {
        val migration =
            object : LegacyMigration by NoLegacyMigration {
                override fun needsMigration(): Boolean = true

                override suspend fun run(): MigrationResult = error("저장 공간 없음")
            }
        val vm = MigrationViewModel(migration, followUp = { FollowUpResult() }, background = dispatcher)
        advanceUntilIdle()

        assertEquals(MigrationUiState.Failed(MigrationFailureKind.UNKNOWN, null), vm.state.value)
    }

    /** 원본이 있는지 보다가 실패해도 첫 화면에서 꺼지지 않는다. */
    @Test
    fun a_crash_while_checking_lands_on_the_failure_screen() = runTest(dispatcher) {
        val migration =
            object : LegacyMigration by NoLegacyMigration {
                override fun needsMigration(): Boolean = error("읽을 수 없음")

                override suspend fun run(): MigrationResult = error("읽을 수 없음")
            }
        val vm = MigrationViewModel(migration, followUp = { FollowUpResult() }, background = dispatcher)
        advanceUntilIdle()

        assertIs<MigrationUiState.Failed>(vm.state.value)
    }

    /** 공간이 차 미뤄 두기를 적지 못해도 새 일기부터 쓰기는 앱을 연다. */
    @Test
    fun skip_opens_the_app_even_when_the_deferral_cannot_be_written() = runTest(dispatcher) {
        val migration =
            object : LegacyMigration by NoLegacyMigration {
                override fun needsMigration(): Boolean = true

                override suspend fun run(): MigrationResult =
                    MigrationResult.Failed(MigrationStep.COPYING, MigrationFailureKind.WRITE_ERROR)

                override fun defer() = error("저장 공간 없음")
            }
        val vm = MigrationViewModel(migration, followUp = { FollowUpResult() }, background = dispatcher)
        advanceUntilIdle()

        vm.skip()
        advanceUntilIdle()

        assertEquals(MigrationUiState.Passed, vm.state.value)
    }

    /** 실패 화면의 새 일기부터 쓰기: 미뤄 두고 앱 틀로 간다. */
    @Test
    fun skip_defers_and_opens_the_app() = runTest(dispatcher) {
        var deferred = 0
        val migration =
            object : LegacyMigration by NoLegacyMigration {
                override fun needsMigration(): Boolean = true

                override suspend fun run(): MigrationResult =
                    MigrationResult.Failed(MigrationStep.COPYING, MigrationFailureKind.WRITE_ERROR)

                override fun defer() {
                    deferred++
                }
            }
        val vm = MigrationViewModel(migration, followUp = { FollowUpResult() }, background = dispatcher)
        advanceUntilIdle()
        assertIs<MigrationUiState.Failed>(vm.state.value)

        vm.skip()
        advanceUntilIdle()

        assertEquals(MigrationUiState.Passed, vm.state.value)
        assertEquals(1, deferred)
    }

    /** 후속 작업이 실패해도 앱 틀로 가고 아무것도 알리지 않는다. */
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
