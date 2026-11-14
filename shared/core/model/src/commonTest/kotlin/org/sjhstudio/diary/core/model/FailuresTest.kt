package org.sjhstudio.diary.core.model

import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FailuresTest {
    @Test
    fun a_failure_gives_the_fallback() = assertEquals("대신", orElseOnFailure("대신") { error("실패") })

    @Test
    fun success_passes_through() = assertEquals(1, orElseOnFailure(0) { 1 })

    @Test
    fun cancelling_is_never_swallowed() {
        assertFailsWith<CancellationException> { orElseOnFailure(0) { throw CancellationException("취소") } }
    }
}
