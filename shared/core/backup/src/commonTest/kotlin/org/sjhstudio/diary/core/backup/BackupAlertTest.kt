package org.sjhstudio.diary.core.backup

import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.SYSTEM
import org.sjhstudio.diary.core.model.BackupFailure
import org.sjhstudio.diary.core.testing.DriveFault
import org.sjhstudio.diary.core.testing.FakeBackupNotifier
import org.sjhstudio.diary.core.testing.FakeBackupScheduler
import org.sjhstudio.diary.core.testing.FakeDrive

/**
 * "7일 동안 백업하지 못했어요" 알림(FR-016)
 * 잘 되고 있거나 사용자가 끈 사람에게는 오면 안 됨
 */
class BackupAlertTest {
    private val fs = FileSystem.SYSTEM
    private val root = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "alert-${Random.nextLong()}"
    private val phone = TestPhone(root.also(fs::createDirectories), FakeDrive(fs))
    private val notifier = FakeBackupNotifier()
    private val scheduler = FakeBackupScheduler()
    private val alerts = BackupAlerts(AutoBackupNotifier(phone.store, notifier), { "폴더" to "새로 만듦" }) { "제목" to (it?.name ?: "까닭 없음") }
    private val runner = phone.runner(alerts = alerts)
    private val controller =
        BackupController(
            phone.store,
            lazy { phone.db },
            lazy { runner },
            phone.auth,
            scheduler,
            notifier,
            CoroutineScope(Dispatchers.Unconfined),
            phone.clock,
            alerts = alerts
        )

    @AfterTest
    fun close() {
        phone.close()
        fs.deleteRecursively(root)
    }

    private suspend fun connected(auto: Boolean = true) {
        phone.store.update { it.copy(accountKey = "sub-1", accountLabel = "diary@gmail.com", autoEnabled = auto) }
        phone.write("e1")
    }

    /** 일기를 쓰지 않은 주에도 백업은 잘 되는 중이라 알림을 뒤로 미룸 */
    @Test
    fun nothing_new_pushes_the_alert_back() = runTest {
        connected()
        assertIs<BackupOutcome.Done>(controller.runAutomatic())
        val first = phone.clock.now()
        assertEquals(first + 7.days, notifier.at)

        phone.clock.advance(3.days)
        assertEquals(BackupOutcome.Unchanged, controller.runAutomatic())

        assertEquals(first + 10.days, notifier.at)
        assertEquals("까닭 없음", notifier.body)
    }

    /** 바뀐 것이 없어 미룬 알림은 뒤이은 실패가 앞당기지 않고 까닭만 바꿈 */
    @Test
    fun a_failure_after_a_quiet_day_keeps_the_pushed_alert() = runTest {
        connected()
        controller.runAutomatic()
        val first = phone.clock.now()
        phone.clock.advance(3.days)
        controller.runAutomatic()

        phone.clock.advance(1.days)
        repeat(FAULTS) { phone.drive.faults.addLast(DriveFault.OFFLINE) }
        phone.write("e2")
        controller.runAutomatic()

        assertEquals(first + 10.days, notifier.at)
        assertEquals(BackupFailure.OFFLINE.name, notifier.body)
    }

    /** 오래 끊겼다 다시 이으면 그때부터 7일. 그 뒤 실패도 같은 시각에 까닭을 담음 */
    @Test
    fun a_failure_after_reconnecting_keeps_the_reconnect_alert() = runTest {
        connected()
        controller.runAutomatic()
        phone.clock.advance(30.days)
        phone.store.update { it.copy(lastFailure = BackupFailure.AUTH, failingSince = phone.clock.now()) }
        val reconnected = phone.clock.now()
        // 연결 직후 지금 백업은 시스템 작업이 맡아 아래 자동 백업과 겹치지 않음
        scheduler.takesNow = true
        controller.connect()
        assertEquals(reconnected + 7.days, notifier.at)

        phone.clock.advance(1.days)
        repeat(FAULTS) { phone.drive.faults.addLast(DriveFault.OFFLINE) }
        phone.write("e2")
        controller.runAutomatic()

        assertEquals(reconnected + 7.days, notifier.at)
        assertEquals(BackupFailure.OFFLINE.name, notifier.body)
    }

    /** 최근에 성공했으면 다시 이어도 알림이 그 성공 기준보다 앞당겨지지 않음 */
    @Test
    fun a_failure_soon_after_reconnecting_does_not_ring_early() = runTest {
        connected()
        controller.runAutomatic()
        phone.clock.advance(2.days)
        val reconnected = phone.clock.now()
        // 연결 직후 지금 백업은 시스템 작업이 맡아 아래 자동 백업과 겹치지 않음
        scheduler.takesNow = true
        controller.connect()

        phone.clock.advance(1.days)
        repeat(FAULTS) { phone.drive.faults.addLast(DriveFault.OFFLINE) }
        phone.write("e2")
        controller.runAutomatic()

        assertEquals(reconnected + 7.days, notifier.at)
    }

    /** 앱을 열 때 도는 자동 백업도 같은 길 */
    @Test
    fun opening_the_app_with_nothing_new_pushes_the_alert_back() = runTest {
        connected()
        controller.runAutomatic()
        val first = phone.clock.now()

        phone.clock.advance(2.days)
        controller.runIfDue()

        assertEquals(first + 9.days, notifier.at)
    }

    /** 시스템 작업이 결과를 알도록 실패하면 거짓 */
    @Test
    fun a_due_backup_that_fails_reports_failure() = runTest {
        connected()
        repeat(FAULTS) { phone.drive.faults.addLast(DriveFault.OFFLINE) }
        assertFalse(controller.runIfDue())

        phone.drive.faults.clear()
        assertTrue(controller.runIfDue())
    }

    /** 바뀐 것이 없으면 지난 실패는 지나간 일. 백업은 이미 드라이브에 있음 */
    @Test
    fun nothing_new_clears_an_old_failure() = runTest {
        connected()
        controller.runAutomatic()
        phone.store.update { it.copy(lastFailure = BackupFailure.OFFLINE, failingSince = phone.clock.now()) }

        controller.runAutomatic()

        assertNull(phone.store.current().lastFailure)
        assertNull(phone.store.current().failingSince)
    }

    /** 드라이브가 차 사진을 못 올린 날은 글이 그대로여도 공간 부족을 남김. 백업 화면이 무엇이 모자란지 알림 */
    @Test
    fun nothing_new_keeps_a_full_drive() = runTest {
        connected()
        controller.runAutomatic()
        phone.drive.limit = phone.drive.visible.sumOf { it.size ?: 0 } + 2 * 1024 * 1024
        phone.photo("e1", 0, "큰 사진".repeat(1_000_000))
        phone.db.entryDao().update(TestPhone.entry("e1", updated = 11))
        assertIs<BackupOutcome.Done>(controller.runAutomatic())
        phone.clock.advance(1.days)

        assertEquals(BackupOutcome.Unchanged, controller.runAutomatic())

        assertEquals(BackupFailure.NO_SPACE, phone.store.current().lastFailure)
        assertTrue(phone.store.current().bytesShort > 0)
    }

    /** 다시 연결이 필요한 상태는 바뀐 것이 없어도 그대로. 이어서 올릴 길이 막혀 있음 */
    @Test
    fun nothing_new_keeps_a_broken_connection() = runTest {
        connected()
        controller.runAutomatic()
        val first = phone.clock.now()
        phone.store.update { it.copy(lastFailure = BackupFailure.AUTH) }

        phone.clock.advance(3.days)
        controller.runAutomatic()

        assertEquals(BackupFailure.AUTH, phone.store.current().lastFailure)
        // 다시 연결하라는 알림을 미루지 않음
        assertEquals(first + 7.days, notifier.at)
    }

    /** 자동 백업을 끈 사람이 직접 눌러 성공해도 7일 뒤 알림을 걸지 않음 */
    @Test
    fun a_manual_backup_with_auto_off_sets_no_alert() = runTest {
        connected(auto = false)

        assertIs<BackupOutcome.Done>(runner.run())

        assertNull(notifier.at)
    }

    @Test
    fun a_failed_backup_with_auto_off_sets_no_alert() = runTest {
        connected(auto = false)
        phone.drive.limit = 0

        assertIs<BackupOutcome.Failed>(runner.run())

        assertNull(notifier.at)
    }

    @Test
    fun a_backup_after_disconnecting_sets_no_alert() = runTest {
        connected()
        controller.disconnect()

        runner.run()

        assertNull(notifier.at)
    }

    @Test
    fun turning_auto_off_or_disconnecting_clears_the_waiting_alert() = runTest {
        connected()
        controller.runAutomatic()

        controller.setAuto(false)
        assertNull(notifier.at)

        controller.setAuto(true)
        controller.disconnect()
        assertNull(notifier.at)
    }

    /** 끈 사이 지워진 알림은 다시 켜면 돌아옴. 아니면 백업이 멈춰도 아무도 모름 */
    @Test
    fun turning_auto_back_on_sets_the_alert_again() = runTest {
        connected()
        controller.setAuto(false)

        controller.setAuto(true)

        assertEquals(phone.clock.now() + 7.days, notifier.at)
    }
}

/** 다시 시도까지 모두 막을 만큼의 끊김 */
private const val FAULTS = 10
