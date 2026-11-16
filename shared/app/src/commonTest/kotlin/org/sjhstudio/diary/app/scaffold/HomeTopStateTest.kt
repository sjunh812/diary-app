package org.sjhstudio.diary.app.scaffold

import androidx.datastore.preferences.core.emptyPreferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.sjhstudio.diary.core.data.BackupRepository
import org.sjhstudio.diary.core.data.PromptRepository
import org.sjhstudio.diary.core.datastore.PromptStore
import org.sjhstudio.diary.core.model.BackupNudge
import org.sjhstudio.diary.core.testing.FakeBackupRepository
import org.sjhstudio.diary.core.testing.InMemoryDataStore

// 홈 맨 위 칸은 백업 안내와 의견 카드를 둘 다 읽은 뒤에 한 장으로 정함
class HomeTopStateTest {
    private val start = Instant.parse("2026-11-10T01:00:00Z")
    private var now = start
    private val clock = object : Clock {
        override fun now() = now
    }
    private val written = MutableStateFlow(0)
    private var counted = 0
    private val backup = FakeBackupRepository()

    private suspend fun prompts() = PromptRepository(
        store = PromptStore(InMemoryDataStore(emptyPreferences())),
        writtenSince = {
            counted++
            written
        },
        review = { false },
        clock = clock,
        timeZone = { TimeZone.UTC }
    ).also { it.start() }

    @Test
    fun nothing_is_read_before_start() = runTest(UnconfinedTestDispatcher()) {
        val state = HomeTopState(backup, prompts(), backgroundScope)
        assertEquals(0, counted)
        assertEquals(HomeTop.Reading, state.top.value)

        state.start()
        state.start()
        assertEquals(1, counted)
        assertEquals(HomeTop.Empty, state.top.value)
    }

    @Test
    fun the_backup_nudge_comes_before_the_feedback_card() = runTest(UnconfinedTestDispatcher()) {
        val prompts = prompts()
        now = start + 4.days
        written.value = 3
        val state = HomeTopState(backup, prompts, backgroundScope)
        state.start()
        assertEquals(HomeTop.Feedback, state.top.value)

        val nudge = BackupNudge(10, 2, null)
        backup.nudge.value = nudge
        assertEquals(HomeTop.Backup(nudge), state.top.value)

        backup.nudge.value = null
        assertEquals(HomeTop.Feedback, state.top.value)
    }

    /** 백업 안내를 아직 읽지 못했으면 의견 카드도 그리지 않음. 잠깐 그려져도 본 것으로 적힘 */
    @Test
    fun the_card_waits_until_the_nudge_is_read() = runTest(UnconfinedTestDispatcher()) {
        val prompts = prompts()
        now = start + 4.days
        written.value = 3
        val slow = SlowNudgeBackup()
        val state = HomeTopState(slow, prompts, backgroundScope)
        state.start()
        assertEquals(HomeTop.Reading, state.top.value)

        slow.read.emit(null)
        assertEquals(HomeTop.Feedback, state.top.value)
    }
}

/** 백업 안내를 늦게 읽는 저장소 */
private class SlowNudgeBackup(base: FakeBackupRepository = FakeBackupRepository()) : BackupRepository by base {
    val read = MutableSharedFlow<BackupNudge?>()
    override val nudge = read
}
