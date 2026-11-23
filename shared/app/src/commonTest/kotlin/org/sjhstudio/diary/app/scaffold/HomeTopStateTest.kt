package org.sjhstudio.diary.app.scaffold

import androidx.datastore.preferences.core.emptyPreferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.sjhstudio.diary.core.data.AppUpdateRepository
import org.sjhstudio.diary.core.data.BackupRepository
import org.sjhstudio.diary.core.data.PromptRepository
import org.sjhstudio.diary.core.datastore.PromptStore
import org.sjhstudio.diary.core.model.BackupNudge
import org.sjhstudio.diary.core.model.StoreCheck
import org.sjhstudio.diary.core.model.UpdateNotice
import org.sjhstudio.diary.core.testing.FakeBackupRepository
import org.sjhstudio.diary.core.testing.FakeStoreUpdates
import org.sjhstudio.diary.core.testing.InMemoryDataStore

// 홈 맨 위 칸은 앞 순위 카드를 다 읽은 뒤에 한 장으로 정함
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

    private val store = FakeStoreUpdates()

    // 처음 확인을 마친 상태. 그 전에는 맨 위 칸이 읽는 중
    private suspend fun TestScope.updates() = AppUpdateRepository(store, {
        ""
    }, PromptStore(InMemoryDataStore(emptyPreferences())), "2.0.1", CoroutineScope(backgroundScope.coroutineContext))
        .also { it.refresh() }

    @Test
    fun the_top_waits_until_the_first_update_check() = runTest(UnconfinedTestDispatcher()) {
        val updates =
            AppUpdateRepository(store, {
                ""
            }, PromptStore(InMemoryDataStore(emptyPreferences())), "2.0.1", CoroutineScope(backgroundScope.coroutineContext))
        val state = HomeTopState(backup, prompts(), updates, backgroundScope)
        state.start()
        assertEquals(HomeTop.Reading, state.top.value)
        updates.refresh()
        assertEquals(HomeTop.Empty, state.top.value)
    }

    /** 백업 안내는 기록을 지키는 카드라 새 버전 확인(iOS는 네트워크)이 늦어도 바로 보임 */
    @Test
    fun the_backup_nudge_does_not_wait_for_the_update_check() = runTest(UnconfinedTestDispatcher()) {
        val updates =
            AppUpdateRepository(store, {
                ""
            }, PromptStore(InMemoryDataStore(emptyPreferences())), "2.0.1", CoroutineScope(backgroundScope.coroutineContext))
        val nudge = BackupNudge(10, 2, null)
        backup.nudge.value = nudge
        val state = HomeTopState(backup, prompts(), updates, backgroundScope)
        state.start()
        assertEquals(HomeTop.Backup(nudge), state.top.value)
    }

    /** 복원한 사진을 받는 동안은 백업 안내를 띄우지 않음. 연결은 받기가 끝날 때까지 기다려 눌러도 반응이 없음 */
    @Test
    fun the_backup_nudge_waits_while_photos_arrive() = runTest(UnconfinedTestDispatcher()) {
        val nudge = BackupNudge(10, 2, null)
        backup.nudge.value = nudge
        val receiving = MutableStateFlow(true)
        val state = HomeTopState(backup, prompts(), updates(), backgroundScope, receiving)
        state.start()
        assertNotEquals(HomeTop.Backup(nudge), state.top.value)

        receiving.value = false

        assertEquals(HomeTop.Backup(nudge), state.top.value)
    }

    /** 새 버전 확인 전에 백업 안내를 닫으면, 닫은 카드를 남기지 않고 확인이 끝날 때까지 정해진 빈칸 */
    @Test
    fun a_closed_backup_nudge_does_not_stay_while_the_update_check_runs() = runTest(UnconfinedTestDispatcher()) {
        val updates =
            AppUpdateRepository(store, {
                ""
            }, PromptStore(InMemoryDataStore(emptyPreferences())), "2.0.1", CoroutineScope(backgroundScope.coroutineContext))
        val nudge = BackupNudge(10, 2, null)
        backup.nudge.value = nudge
        val state = HomeTopState(backup, prompts(), updates, backgroundScope)
        state.start()
        assertEquals(HomeTop.Backup(nudge), state.top.value)

        backup.nudge.value = null
        assertEquals(HomeTop.Waiting, state.top.value)
        updates.refresh()
        assertEquals(HomeTop.Empty, state.top.value)
    }

    /** 백업 안내 값이 같은 "없음"을 되풀이해도 일기 수를 다시 세지 않음 */
    @Test
    fun repeated_empty_nudges_do_not_count_again() = runTest(UnconfinedTestDispatcher()) {
        val prompts = prompts()
        val slow = SlowNudgeBackup()
        val state = HomeTopState(slow, prompts, updates(), backgroundScope)
        state.start()
        slow.read.emit(null)
        val before = counted
        repeat(3) { slow.read.emit(null) }
        assertEquals(before, counted)
    }

    @Test
    fun the_update_card_sits_between_backup_and_feedback() = runTest(UnconfinedTestDispatcher()) {
        val prompts = prompts()
        now = start + 4.days
        written.value = 3
        val updates = updates()
        val state = HomeTopState(backup, prompts, updates, backgroundScope)
        state.start()
        assertEquals(HomeTop.Feedback, state.top.value)

        store.answer = StoreCheck.Available("45")
        updates.refresh()
        assertEquals(HomeTop.Update(UpdateNotice.Available(required = false, note = null)), state.top.value)

        val nudge = BackupNudge(10, 2, null)
        backup.nudge.value = nudge
        assertEquals(HomeTop.Backup(nudge), state.top.value)

        backup.nudge.value = null
        updates.dismiss()
        assertEquals(HomeTop.Feedback, state.top.value)
    }

    @Test
    fun nothing_is_read_before_start() = runTest(UnconfinedTestDispatcher()) {
        val state = HomeTopState(backup, prompts(), updates(), backgroundScope)
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
        val state = HomeTopState(backup, prompts, updates(), backgroundScope)
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
        val state = HomeTopState(slow, prompts, updates(), backgroundScope)
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
