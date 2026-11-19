package org.sjhstudio.diary.core.backup

import androidx.datastore.preferences.core.emptyPreferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.test.runTest
import org.sjhstudio.diary.core.datastore.BackupStore
import org.sjhstudio.diary.core.model.BackupFailure
import org.sjhstudio.diary.core.testing.FakeBackupNotifier
import org.sjhstudio.diary.core.testing.InMemoryDataStore
import org.sjhstudio.diary.core.testing.TestClock

/** 바뀐 것이 없어 올리지 않은 날의 정리(`docs/backup.md` "자동 백업") */
class BackupFailureLogTest {
    private val clock = TestClock()
    private val store = BackupStore(InMemoryDataStore(emptyPreferences()))
    private val notifier = FakeBackupNotifier()
    private val log = BackupFailureLog(store, clock, BackupAlerts(notifier, { "" to "" }) { "제목" to (it?.name ?: "까닭 없음") })

    @Test
    fun an_old_failure_is_over_and_the_alert_moves_on() = runTest {
        store.update { it.copy(lastFailure = BackupFailure.OFFLINE, failingSince = clock.now() - 2.days) }

        log.unchanged(bytesShort = 0)

        assertNull(store.current().lastFailure)
        assertNull(store.current().failingSince)
        assertEquals(clock.now() + 7.days, notifier.at)
        assertEquals("까닭 없음", notifier.body)
    }

    /** 사진을 보지 않은 날(Wi-Fi를 기다림)은 지난 공간 부족을 그대로 둠 */
    @Test
    fun a_day_without_photos_keeps_the_last_shortage() = runTest {
        store.update { it.copy(lastFailure = BackupFailure.NO_SPACE, bytesShort = 500) }

        log.unchanged()

        assertEquals(BackupFailure.NO_SPACE, store.current().lastFailure)
        assertEquals(500, store.current().bytesShort)
    }

    /** 이번에 모자란 것이 없으면 공간 부족은 풀림 */
    @Test
    fun room_again_clears_the_shortage() = runTest {
        store.update { it.copy(lastFailure = BackupFailure.NO_SPACE, bytesShort = 500) }

        log.unchanged(bytesShort = 0)

        assertNull(store.current().lastFailure)
        assertEquals(0, store.current().bytesShort)
    }

    /** 연결이 끊긴 것은 사용자가 풀어야 해서 기록도 알림도 그대로 */
    @Test
    fun a_broken_connection_is_left_alone() = runTest {
        val since = clock.now() - 3.days
        store.update { it.copy(lastFailure = BackupFailure.AUTH, failingSince = since, bytesShort = 0) }

        log.unchanged(bytesShort = 0)

        assertEquals(BackupFailure.AUTH, store.current().lastFailure)
        assertEquals(since, store.current().failingSince)
        assertNull(notifier.at)
    }
}
