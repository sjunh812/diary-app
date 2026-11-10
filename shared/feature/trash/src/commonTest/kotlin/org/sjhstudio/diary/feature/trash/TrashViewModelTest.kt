package org.sjhstudio.diary.feature.trash

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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.sjhstudio.diary.core.data.diary.TrashRepository
import org.sjhstudio.diary.core.model.TrashedEntry

@OptIn(ExperimentalCoroutinesApi::class)
class TrashViewModelTest {
    private val calls = mutableListOf<String>()
    private val repository =
        object : TrashRepository {
            override fun items(): Flow<List<TrashedEntry>> = flowOf(emptyList())

            override suspend fun restore(id: String) {
                calls += "restore $id"
            }

            override suspend fun deleteForever(ids: List<String>) {
                calls += "delete $ids"
            }

            override suspend fun empty() {
                calls += "empty"
            }

            override suspend fun purgeExpired(now: Instant) = Unit
        }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun forwards_each_action() {
        val model = TrashViewModel(repository)

        model.restore("a")
        model.deleteForever("b")
        model.empty()

        assertEquals(listOf("restore a", "delete [b]", "empty"), calls)
    }

    /** 복원이 끝나면 화면에 이벤트로 알림. 콜백을 쥐지 않아 화면이 다시 만들어져도 받음 */
    @Test
    fun a_restore_is_announced_once_done() = runTest(UnconfinedTestDispatcher()) {
        val model = TrashViewModel(repository)
        val restored = mutableListOf<String>()
        backgroundScope.launch { model.restored.collect { restored += it } }

        model.restore("a")

        assertEquals(listOf("a"), restored)
    }

    /** 휴지통을 읽지 못하면 빈 화면으로 남지 않고 다시 시도를 보이며, 다시 시도하면 다시 읽음 */
    @Test
    fun a_failed_read_shows_retry_and_reads_again() = runTest(UnconfinedTestDispatcher()) {
        var fail = true
        val failing =
            object : TrashRepository by repository {
                override fun items(): Flow<List<TrashedEntry>> = flow {
                    if (fail) throw IllegalStateException("disk")
                    emit(emptyList())
                }
            }
        val model = TrashViewModel(failing)
        backgroundScope.launch { model.items.collect {} }
        assertTrue(model.failed.value)
        assertNull(model.items.value)

        fail = false
        model.retry()

        assertFalse(model.failed.value)
        assertEquals(emptyList(), model.items.value)
    }
}
