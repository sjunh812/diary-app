package org.sjhstudio.diary.feature.home

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.sjhstudio.diary.core.data.diary.EntryRepository
import org.sjhstudio.diary.core.model.Entry
import org.sjhstudio.diary.core.model.EntryDetail
import org.sjhstudio.diary.core.platform.FontInstallState
import org.sjhstudio.diary.core.platform.FontInstaller
import org.sjhstudio.diary.core.testing.FakeEntryRepository
import org.sjhstudio.diary.core.testing.FakeSettingsRepository

@OptIn(ExperimentalCoroutinesApi::class)
class EntryDetailViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private val written = Instant.parse("2026-10-31T12:40:00Z")
    private val detail =
        EntryDetail(
            Entry(
                id = "a",
                date = LocalDate(2026, 10, 31),
                time = LocalTime(21, 40),
                bodyHtml = "",
                plainText = "",
                mood = null,
                weather = null,
                placeName = null,
                isFavorite = false,
                createdAt = written,
                updatedAt = written
            ),
            emptyList()
        )

    private class Entries(val stored: MutableStateFlow<EntryDetail?>) : EntryRepository by FakeEntryRepository() {
        override fun entry(id: String): Flow<EntryDetail?> = stored

        override suspend fun moveToTrash(id: String) {
            stored.value = null
        }
    }

    private fun viewModel(entries: EntryRepository) = EntryDetailViewModel("a", entries, FakeSettingsRepository(), NoFonts)

    @Test
    fun entry_that_is_gone_is_missing() = runTest(UnconfinedTestDispatcher()) {
        val vm = viewModel(Entries(MutableStateFlow(null)))
        backgroundScope.launch { vm.state.collect {} }
        assertEquals(EntryDetailState.Missing, vm.state.value)
    }

    /** 휴지통으로 옮기면 화면이 닫히는 중. 닫히는 동안 "찾을 수 없어요"가 비치면 안 됨 */
    @Test
    fun moving_to_trash_never_shows_missing() = runTest(UnconfinedTestDispatcher()) {
        val vm = viewModel(Entries(MutableStateFlow(detail)))
        val seen = mutableListOf<EntryDetailState>()
        backgroundScope.launch { vm.state.toList(seen) }
        assertEquals(EntryDetailState.Ready(detail), vm.state.value)

        vm.moveToTrash()

        assertEquals(EntryDetailState.Loading, vm.state.value)
        assertFalse(EntryDetailState.Missing in seen)
    }

    /** 읽기 실패 시 불러오는 중으로 두지 않고, 다시 시도하면 다시 읽음 */
    @Test
    fun a_failed_read_shows_failed_and_retry_reads_again() = runTest(UnconfinedTestDispatcher()) {
        var fail = true
        val entries =
            object : EntryRepository by FakeEntryRepository() {
                override fun entry(id: String): Flow<EntryDetail?> = flow {
                    if (fail) throw IllegalStateException("disk")
                    emit(detail)
                }
            }
        val vm = viewModel(entries)
        backgroundScope.launch { vm.state.collect {} }
        assertEquals(EntryDetailState.Failed, vm.state.value)

        fail = false
        vm.retry()

        assertEquals(EntryDetailState.Ready(detail), vm.state.value)
    }

    private object NoFonts : FontInstaller {
        override fun installedPath(id: String): String? = null

        override fun install(id: String): Flow<FontInstallState> = emptyFlow()
    }
}
