package org.sjhstudio.diary.feature.migration

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.sjhstudio.diary.core.migration.LegacyArchive
import org.sjhstudio.diary.core.migration.LegacyEntryView
import org.sjhstudio.diary.core.migration.LegacyNote

@OptIn(ExperimentalCoroutinesApi::class)
class LegacyReaderViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun view(id: Long) = LegacyEntryView(
        id = id,
        source = LegacyNote(id, null, null, null, null, null, null, null, null, null, null),
        date = LocalDate(2024, 10, 1),
        time = LocalTime(9, 0),
        text = "",
        mood = null,
        weather = null,
        placeName = null,
        favorite = false,
        photos = emptyList()
    )

    /** 첫 쪽이 늦게 오는 동안 "더 읽기"가 여러 번 불려도 같은 쪽이 두 번 붙지 않는다. */
    @Test
    fun repeated_load_more_while_loading_does_not_duplicate() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        var calls = 0
        val archive =
            object : LegacyArchive {
                override suspend fun page(after: LegacyEntryView?, limit: Int): List<LegacyEntryView> {
                    calls++
                    gate.await()
                    val start = (after?.id ?: 0L) + 1
                    return (start until start + 3).map(::view)
                }
            }
        val vm = LegacyReaderViewModel(archive)
        repeat(5) { vm.loadMore() }
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, calls)
        assertEquals(listOf(1L, 2L, 3L), vm.state.value.entries.map { it.id })
        assertTrue(vm.state.value.end)
    }

    /** 읽지 못한 것은 빈 목록과 다르다. 끝으로 치지 않고, 다시 시도하면 이어서 읽는다. */
    @Test
    fun failed_page_is_not_an_empty_list() = runTest(dispatcher) {
        var broken = true
        val archive =
            object : LegacyArchive {
                override suspend fun page(after: LegacyEntryView?, limit: Int): List<LegacyEntryView> {
                    if (broken) error("읽기 실패")
                    return listOf(view(1))
                }
            }
        val vm = LegacyReaderViewModel(archive)
        advanceUntilIdle()
        assertTrue(vm.state.value.failed)
        assertFalse(vm.state.value.end)
        assertFalse(vm.state.value.loading)

        broken = false
        vm.loadMore()
        advanceUntilIdle()
        assertFalse(vm.state.value.failed)
        assertEquals(listOf(1L), vm.state.value.entries.map { it.id })
    }

    /** 취소는 실패가 아니다. */
    @Test
    fun cancellation_is_not_reported_as_failure() = runTest(dispatcher) {
        val archive =
            object : LegacyArchive {
                override suspend fun page(after: LegacyEntryView?, limit: Int): List<LegacyEntryView> = throw CancellationException()
            }
        val vm = LegacyReaderViewModel(archive)
        advanceUntilIdle()
        assertFalse(vm.state.value.failed)
    }
}
