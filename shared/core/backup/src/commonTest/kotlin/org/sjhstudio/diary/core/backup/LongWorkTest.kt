package org.sjhstudio.diary.core.backup

import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.blackholeSink
import org.sjhstudio.diary.core.model.RestorePreparation
import org.sjhstudio.diary.core.testing.FakeDrive

/** iOS가 시스템 시간을 받아 두는 기준. 백업·복원·파일 일이 도는 동안만 true */
@OptIn(ExperimentalCoroutinesApi::class)
class LongWorkTest {
    private val fs = FileSystem.SYSTEM
    private lateinit var dir: Path
    private lateinit var phone: TestPhone

    @BeforeTest
    fun setUp() {
        dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "long-work-${Random.nextLong()}"
        phone = TestPhone(dir / "a", FakeDrive(fs))
    }

    @AfterTest
    fun tearDown() {
        phone.close()
        fs.deleteRecursively(dir)
    }

    /** 바뀔 때마다 적음 */
    private fun TestScope.watch(work: LongWork): List<Boolean> {
        val seen = mutableListOf<Boolean>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { work.running.collect { seen += it } }
        return seen
    }

    @Test
    fun running_only_while_held_even_when_it_fails() = runTest {
        val work = LongWork()
        val seen = watch(work)

        work.holding { assertTrue(work.running.first()) }
        assertFailsWith<IOException> { work.holding { throw IOException("공간 부족") } }

        assertEquals(listOf(false, true, false, true, false), seen)
    }

    @Test
    fun the_gate_counts_its_holder() = runTest {
        val gate = BackupGate()
        val seen = watch(gate.longWork)

        assertTrue(gate.tryEnter())
        assertFalse(gate.tryEnter())
        gate.leave()

        assertEquals(listOf(false, true, false), seen)
    }

    @Test
    fun a_restore_waiting_for_a_backup_keeps_the_time_between_them() = runTest {
        val gate = BackupGate()
        val seen = watch(gate.longWork)
        assertTrue(gate.tryEnter())
        val entered = CompletableDeferred<Unit>()
        val waiting =
            launch {
                gate.enter()
                entered.complete(Unit)
            }
        runCurrent()

        gate.leave()
        entered.await()
        gate.leave()
        waiting.join()

        assertEquals(listOf(false, true, false), seen)
    }

    @Test
    fun giving_up_the_wait_lets_the_time_go() = runTest {
        val gate = BackupGate()
        assertTrue(gate.tryEnter())
        val waiting = launch { gate.enter() }
        runCurrent()

        waiting.cancel()
        waiting.join()
        gate.leave()

        assertFalse(gate.longWork.running.first())
    }

    @Test
    fun writing_a_file_holds_the_time_until_it_is_written() = runTest {
        phone.write("one")
        val seen = watch(phone.gate.longWork)

        phone.controller(backgroundScope).writeBundle({ blackholeSink() }) { _, _ -> }

        assertEquals(listOf(false, true, false), seen)
    }

    @Test
    fun a_file_that_fails_to_write_lets_the_time_go() = runTest {
        phone.write("one")
        val seen = watch(phone.gate.longWork)

        assertFailsWith<IOException> { phone.controller(backgroundScope).writeBundle({ throw IOException("공간 부족") }) { _, _ -> } }

        assertEquals(listOf(false, true, false), seen)
    }

    @Test
    fun reading_a_picked_file_lets_the_time_go_even_when_it_is_not_a_backup() = runTest {
        val picked = dir / "picked.txt"
        fs.write(picked) { writeUtf8("백업 파일 아님") }
        val seen = watch(phone.gate.longWork)

        assertIs<RestorePreparation.Failed>(phone.controller(backgroundScope).prepareFile(fs.source(picked), null))

        assertEquals(listOf(false, true, false), seen)
    }
}
