package org.sjhstudio.diary.app.maintenance

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.sjhstudio.diary.core.data.diary.MediaRepository
import org.sjhstudio.diary.core.data.diary.TrashRepository
import org.sjhstudio.diary.core.model.TrashedEntry
import org.sjhstudio.diary.core.testing.TestClock

class DiaryCleanupTest {
    private val calls = mutableListOf<String>()

    private fun trash(failing: Boolean = false) = object : TrashRepository {
        override fun items(): Flow<List<TrashedEntry>> = emptyFlow()

        override suspend fun restore(id: String) = Unit

        override suspend fun deleteForever(ids: List<String>) = Unit

        override suspend fun empty() = Unit

        override suspend fun purgeExpired(now: Instant) {
            calls += "purge"
            if (failing) error("디스크 오류")
        }
    }

    private val media =
        object : MediaRepository {
            override suspend fun cleanUnused(now: Instant) {
                calls += "clean"
            }
        }

    @Test
    fun purges_before_cleaning_files() = runTest {
        DiaryCleanup(trash(), media, TestClock(Instant.fromEpochSeconds(0))).run()

        assertEquals(listOf("purge", "clean"), calls)
    }

    @Test
    fun failure_does_not_reach_the_app() = runTest {
        DiaryCleanup(trash(failing = true), media, TestClock(Instant.fromEpochSeconds(0))).run()

        assertEquals(listOf("purge"), calls)
    }
}
