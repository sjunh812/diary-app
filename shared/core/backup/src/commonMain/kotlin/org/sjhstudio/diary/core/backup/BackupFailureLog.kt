package org.sjhstudio.diary.core.backup

import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.coroutines.CancellationException
import org.sjhstudio.diary.core.datastore.BackupStore
import org.sjhstudio.diary.core.model.BackupFailure

/**
 * 드라이브 일이 실패한 까닭과 7일 알림을 한 자리에서 다룸
 * 백업 실행, 복원 목록, 복원 사진 받기가 함께 써서 백업 화면과 7일 알림이 같은 사실을 말함
 * 자동 백업을 켜고 연결한 사람에게만 거는 규칙은 [AutoBackupNotifier]가 맡음
 */
class BackupFailureLog(private val store: BackupStore, private val clock: Clock = Clock.System, private val alerts: BackupAlerts? = null) {
    suspend fun record(kind: BackupFailure) {
        try {
            val now = clock.now()
            store.update { it.copy(lastFailure = kind, failingSince = it.failingSince ?: now) }
            val prefs = store.current()
            val at = prefs.alertAt ?: ((prefs.lastSuccessAt ?: prefs.failingSince ?: now) + ALERT_AFTER)
            // 걸어 둔 시각은 두고 까닭만 바꿈. 때가 지났으면 이미 한 번 알렸음
            if (at > now) remind(at, kind)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            // 공간이 차면 실패 기록도 못 쓸 수 있음. 실패는 부른 쪽이 그대로 알림
        }
    }

    /** 백업이 끝났거나 바뀐 것이 없거나 다시 연결해 [from]부터 7일 뒤로 알림을 미룸 */
    suspend fun stillFine(from: Instant) {
        try {
            remind(from + ALERT_AFTER, null)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            // 시각을 못 적어도 알림은 걸어 둔 그대로
        }
    }

    private suspend fun remind(at: Instant, kind: BackupFailure?) {
        store.update { it.copy(alertAt = at) }
        alerts?.schedule(at, kind)
    }

    companion object {
        val ALERT_AFTER = 7.days
    }
}
