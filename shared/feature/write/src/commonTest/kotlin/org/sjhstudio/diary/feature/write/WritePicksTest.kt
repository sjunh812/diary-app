package org.sjhstudio.diary.feature.write

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import org.sjhstudio.diary.core.data.PlanRepository
import org.sjhstudio.diary.core.model.AddPhotosResult
import org.sjhstudio.diary.core.model.PickedImage
import org.sjhstudio.diary.core.model.Plan
import org.sjhstudio.diary.core.testing.FakeDraftRepository
import org.sjhstudio.diary.core.testing.FakePlanRepository
import org.sjhstudio.diary.core.testing.TestClock

// 사진·체크리스트 누름이 요금제 확정을 기다리는 흐름
@OptIn(ExperimentalCoroutinesApi::class)
class WritePicksTest {
    private val dispatcher = StandardTestDispatcher()
    private val clock = TestClock(Instant.parse("2026-11-01T12:00:00Z"))

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun full_photos_notice_the_limit_by_plan() = runTest(dispatcher) {
        val plans = object : PlanRepository {
            override val plan = MutableStateFlow(Plan.FREE)
        }
        val viewModel = testWriteViewModel(WriteTarget.New(), FakeDraftRepository(), clock, TimeZone.UTC, plans = plans)
        advanceUntilIdle()

        viewModel.addPhotos(List(10) { i -> PickedImage { byteArrayOf(i.toByte()) } })
        advanceUntilIdle()
        viewModel.requestPhotos(PhotoSource.LIBRARY)
        advanceUntilIdle()
        assertEquals(PhotoNotice.Limit(10, canUpgrade = true), viewModel.state.value.photoNotice)
        assertNull(viewModel.state.value.photoPick)

        viewModel.photoNoticeShown()
        plans.plan.value = Plan.PLUS
        advanceUntilIdle()
        viewModel.requestPhotos(PhotoSource.LIBRARY)
        // 기다리는 동안 다시 눌러도 한 번만
        viewModel.requestPhotos(PhotoSource.LIBRARY)
        advanceUntilIdle()
        assertEquals(PhotoPick.Open(PhotoSource.LIBRARY, 30), viewModel.state.value.photoPick)
        assertNull(viewModel.state.value.photoNotice)
    }

    /** 새 체크리스트 표시는 요금제 확정을 화면 모델이 기다렸다가 Plus면 허락, 무료면 Plus 시트 */
    @Test
    fun checklist_waits_for_the_plan_then_allows_or_locks() = runTest(dispatcher) {
        val plans = FakePlanRepository(Plan.FREE, known = false)
        val viewModel = testWriteViewModel(WriteTarget.New(), FakeDraftRepository(), clock, TimeZone.UTC, plans = plans)
        advanceUntilIdle()
        val target = ChecklistTarget("우유", 0, 2)

        viewModel.requestChecklist(target)
        // 기다리는 동안 다시 눌러도 한 번만
        viewModel.requestChecklist(ChecklistTarget("다른 글", 0, 0))
        advanceTimeBy(1.seconds)
        assertEquals(ChecklistPick.Waiting(target), viewModel.state.value.checklistPick)

        plans.plan.value = Plan.PLUS
        plans.known.value = true
        advanceUntilIdle()
        assertEquals(ChecklistPick.Allowed(target), viewModel.state.value.checklistPick)

        viewModel.checklistPickHandled()
        plans.plan.value = Plan.FREE
        advanceUntilIdle()
        viewModel.requestChecklist(target)
        advanceUntilIdle()
        assertEquals(ChecklistPick.Locked(target), viewModel.state.value.checklistPick)

        // 권유를 띄운 사이 Plus가 되면 권하지 않고 누른 대로 넣음
        plans.plan.value = Plan.PLUS
        advanceUntilIdle()
        assertEquals(ChecklistPick.Allowed(target, unlocked = true), viewModel.state.value.checklistPick)
    }

    /** 닫거나 버리기 시작하면 기다리던 누름을 끝냄. 떠나는 화면에서 고르기를 열거나 표시를 넣지 않음 */
    @Test
    fun leaving_drops_the_waiting_picks() = runTest(dispatcher) {
        val plans = FakePlanRepository(Plan.PLUS, known = false)
        val viewModel = testWriteViewModel(WriteTarget.New(), FakeDraftRepository(), clock, TimeZone.UTC, plans = plans)
        advanceUntilIdle()
        viewModel.requestPhotos(PhotoSource.LIBRARY)
        viewModel.requestChecklist(ChecklistTarget("우유", 0, 2))
        advanceTimeBy(1.seconds)

        viewModel.close()
        plans.known.value = true
        advanceUntilIdle()

        assertNull(viewModel.state.value.photoPick)
        assertNull(viewModel.state.value.checklistPick)
    }

    /** 재설치 직후 요금제를 모를 때 산 사람에게 무료 한도로 막지 않고 확인을 기다림 */
    @Test
    fun photos_wait_for_the_plan_before_the_limit() = runTest(dispatcher) {
        val plans = FakePlanRepository(Plan.FREE, known = false)
        val viewModel = testWriteViewModel(WriteTarget.New(), FakeDraftRepository(), clock, TimeZone.UTC, plans = plans)
        viewModel.addPhotos(List(10) { i -> PickedImage { byteArrayOf(i.toByte()) } })
        advanceUntilIdle()

        viewModel.requestPhotos(PhotoSource.LIBRARY)
        advanceTimeBy(1.seconds)
        assertEquals(PhotoPick.Waiting(PhotoSource.LIBRARY), viewModel.state.value.photoPick)
        assertNull(viewModel.state.value.photoNotice)

        plans.plan.value = Plan.PLUS
        plans.known.value = true
        advanceUntilIdle()
        assertEquals(PhotoPick.Open(PhotoSource.LIBRARY, 30), viewModel.state.value.photoPick)
        assertNull(viewModel.state.value.photoNotice)

        viewModel.photoPickOpened()
        viewModel.requestPhotos(PhotoSource.CAMERA)
        advanceUntilIdle()
        assertEquals(PhotoPick.Open(PhotoSource.CAMERA, 30), viewModel.state.value.photoPick)
    }

    /** 확인이 끝내 오지 않으면 무료 한도로 안내하고 기다림을 풂 */
    @Test
    fun photos_fall_back_to_the_free_limit_when_the_plan_never_comes() = runTest(dispatcher) {
        val plans = FakePlanRepository(Plan.FREE, known = false)
        val viewModel = testWriteViewModel(WriteTarget.New(), FakeDraftRepository(), clock, TimeZone.UTC, plans = plans)
        viewModel.addPhotos(List(10) { i -> PickedImage { byteArrayOf(i.toByte()) } })
        advanceUntilIdle()

        viewModel.requestPhotos(PhotoSource.LIBRARY)
        advanceUntilIdle()

        assertEquals(PhotoNotice.Limit(10, canUpgrade = true), viewModel.state.value.photoNotice)
        assertNull(viewModel.state.value.photoPick)
    }

    /** 고른 사진이 한도를 넘어 시트가 떠도 누른 순간 쓰던 중이었는지를 이어 받음 */
    @Test
    fun a_limit_after_picking_remembers_writing() = runTest(dispatcher) {
        val drafts = FakeDraftRepository()
        val viewModel = testWriteViewModel(WriteTarget.New(), drafts, clock, TimeZone.UTC, plans = FakePlanRepository(Plan.FREE))
        viewModel.addPhotos(List(9) { i -> PickedImage { byteArrayOf(i.toByte()) } })
        advanceUntilIdle()

        viewModel.requestPhotos(PhotoSource.LIBRARY, writing = true)
        advanceUntilIdle()
        assertEquals(PhotoPick.Open(PhotoSource.LIBRARY, 10, writing = true), viewModel.state.value.photoPick)
        viewModel.photoPickOpened()
        drafts.nextAddResult = AddPhotosResult.LimitReached(10, canUpgrade = true, added = 1)
        viewModel.addPhotos(List(3) { i -> PickedImage { byteArrayOf((20 + i).toByte()) } })
        advanceUntilIdle()

        assertEquals(PhotoNotice.Limit(10, canUpgrade = true, writing = true), viewModel.state.value.photoNotice)
    }
}
