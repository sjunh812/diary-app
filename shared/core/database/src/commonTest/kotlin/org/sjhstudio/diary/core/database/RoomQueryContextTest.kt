package org.sjhstudio.diary.core.database

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlinx.coroutines.CoroutineExceptionHandler

// DB를 못 열어도 Room 안쪽 작업의 예외가 앱을 끄지 않아야 함
class RoomQueryContextTest {
    @Test
    fun room_work_has_an_exception_handler() {
        assertNotNull(RoomQueryContext[CoroutineExceptionHandler])
    }
}
