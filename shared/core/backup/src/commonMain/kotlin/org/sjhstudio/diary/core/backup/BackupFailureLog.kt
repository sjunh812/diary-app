package org.sjhstudio.diary.core.backup

import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.CancellationException
import org.sjhstudio.diary.core.datastore.BackupStore
import org.sjhstudio.diary.core.model.BackupFailure

/**
 * 드라이브 일이 실패한 까닭을 한 자리에 적음
 * 백업 실행, 복원 목록, 복원 사진 받기가 함께 써서 백업 화면과 7일 알림이 같은 사실을 말함
 */
class BackupFailureLog(private val store: BackupStore, private val clock: Clock = Clock.System, private val alerts: BackupAlerts? = null) {
    suspend fun record(kind: BackupFailure) {
        try {
            val now = clock.now()
            store.update { it.copy(lastFailure = kind, failingSince = it.failingSince ?: now) }
            val prefs = store.current()
            val since = prefs.lastSuccessAt ?: prefs.failingSince ?: now
            // 까닭이 바뀌면 문구만 바꿈. 때가 지났으면 이미 한 번 알렸음
            if (since + ALERT_AFTER > now) alerts?.schedule(since + ALERT_AFTER, kind)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            // 공간이 차면 실패 기록도 못 쓸 수 있음. 실패는 부른 쪽이 그대로 알림
        }
    }

    companion object {
        val ALERT_AFTER = 7.days
    }
}
