package org.sjhstudio.diary.feature.trash

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
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
}
