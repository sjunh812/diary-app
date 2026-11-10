package org.sjhstudio.diary.core.backup

import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import org.sjhstudio.diary.core.model.BackupFailure
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

    /** 일기를 쓰지 않은 주에도 백업은 잘 되는 중이라 알림을 뒤로 밂 */
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

    /** 다시 연결이 필요한 상태는 바뀐 것이 없어도 그대로. 이어서 올릴 길이 막혀 있음 */
    @Test
    fun nothing_new_keeps_a_broken_connection() = runTest {
        connected()
        controller.runAutomatic()
        phone.store.update { it.copy(lastFailure = BackupFailure.AUTH) }

        controller.runAutomatic()

        assertEquals(BackupFailure.AUTH, phone.store.current().lastFailure)
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
