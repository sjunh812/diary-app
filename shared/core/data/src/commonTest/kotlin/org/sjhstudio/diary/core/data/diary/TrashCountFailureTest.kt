package org.sjhstudio.diary.core.data.diary

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.sjhstudio.diary.core.testing.TestClock

// 설정 화면이 바로 모으는 휴지통 개수는 DB를 읽지 못해도 예외를 던지지 않아야 함
class TrashCountFailureTest {
    @Test
    fun a_closed_database_gives_no_count_instead_of_a_crash() = runTest {
        val db = memoryDatabase()
        db.close()
        assertEquals(emptyList(), DefaultTrashRepository(db, MEDIA_DIR, TestClock(T0)).count().toList())
    }
}
