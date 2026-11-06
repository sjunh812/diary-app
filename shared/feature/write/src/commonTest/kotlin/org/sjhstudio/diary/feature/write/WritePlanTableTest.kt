package org.sjhstudio.diary.feature.write

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import org.sjhstudio.diary.core.model.AddPhotosResult
import org.sjhstudio.diary.core.model.Checklist
import org.sjhstudio.diary.core.model.Draft
import org.sjhstudio.diary.core.model.DraftKey
import org.sjhstudio.diary.core.model.PickedImage
import org.sjhstudio.diary.core.model.Plan
import org.sjhstudio.diary.core.testing.FakeDraftRepository
import org.sjhstudio.diary.core.testing.FakePlanRepository
import org.sjhstudio.diary.core.testing.TestClock

// 쓰기 화면이 요금제에서 받는 값. 표에 없는 것은 어느 요금제든 같고, 쓰던 글은 막지 않는다(헌법 III).
@OptIn(ExperimentalCoroutinesApi::class)
class WritePlanTableTest {
    private val dispatcher = StandardTestDispatcher()
    private val clock = TestClock(Instant.parse("2026-11-01T12:00:00Z"))

    private val table =
        listOf(
            Triple(Plan.FREE, false, 10),
            Triple(Plan.AD_FREE, false, 10),
            Triple(Plan.PLUS, true, 30)
        )

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun vm(plans: FakePlanRepository, drafts: FakeDraftRepository = FakeDraftRepository(), target: WriteTarget = WriteTarget.New()) =
        WriteViewModel(target, drafts, clock, TimeZone.UTC, plans = plans)

    @Test
    fun the_checklist_tool_and_the_photo_limit_follow_the_table() = runTest(dispatcher) {
        for ((plan, checklist, photos) in table) {
            val viewModel = vm(FakePlanRepository(plan))
            advanceUntilIdle()

            assertEquals(checklist, viewModel.checklistOpen.value, "$plan 체크리스트")
            assertEquals(photos, viewModel.photoLimit.value, "$plan 사진")
        }
    }

    /** 쓰는 도중에 구독이 끝나면 화면이 바로 따르고 쓰던 글은 그대로다. */
    @Test
    fun plus_ending_while_writing_changes_only_the_tools() = runTest(dispatcher) {
        val plans = FakePlanRepository(Plan.PLUS)
        val viewModel = vm(plans)
        advanceUntilIdle()
        viewModel.onBodyChange("<p>${Checklist.PREFIX}장보기</p>")
        advanceUntilIdle()

        plans.plan.value = Plan.FREE
        advanceUntilIdle()

        assertEquals(false, viewModel.checklistOpen.value)
        assertEquals(10, viewModel.photoLimit.value)
        assertEquals("<p>${Checklist.PREFIX}장보기</p>", viewModel.currentBody())
        assertEquals(true, viewModel.state.value.canSave)
    }

    /** 구독이 끝나도 이미 쓴 체크리스트는 고쳐 저장할 수 있다(FR-020). */
    @Test
    fun an_old_checklist_can_be_saved_on_every_plan() = runTest(dispatcher) {
        for ((plan) in table) {
            val body = "<p>${Checklist.DONE} 끝낸 일</p>"
            val key = DraftKey.Edit("e1")
            val draft = Draft(key, LocalDate(2026, 11, 1), LocalTime(9, 0), body, null, null, null, false, false, clock.now())
            val drafts = FakeDraftRepository(editable = mapOf("e1" to draft))
            val viewModel = vm(FakePlanRepository(plan), drafts, WriteTarget.Edit("e1"))
            advanceUntilIdle()

            viewModel.onBodyChange("<p>${Checklist.OPEN} 끝낸 일</p>")
            viewModel.save()
            advanceUntilIdle()

            assertEquals(listOf<DraftKey>(key), drafts.committed, "$plan")
        }
    }

    @Test
    fun the_limit_sheet_offers_plus_only_to_those_without_it() = runTest(dispatcher) {
        for ((plan, _, photos) in table) {
            val drafts = FakeDraftRepository()
            val viewModel = vm(FakePlanRepository(plan), drafts)
            advanceUntilIdle()
            drafts.nextAddResult = AddPhotosResult.LimitReached(photos, canUpgrade = plan != Plan.PLUS, added = 0)

            viewModel.addPhotos(listOf(PickedImage { byteArrayOf(1) }))
            advanceUntilIdle()

            assertEquals(PhotoNotice.Limit(photos, plan != Plan.PLUS), viewModel.state.value.photoNotice, "$plan")
        }
    }
}
