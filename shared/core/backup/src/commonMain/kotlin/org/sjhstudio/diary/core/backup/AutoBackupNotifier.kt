package org.sjhstudio.diary.core.backup

import kotlin.time.Instant
import org.sjhstudio.diary.core.datastore.BackupStore
import org.sjhstudio.diary.core.platform.BackupNotifier

/**
 * 자동 백업을 켜 둔 사람에게만 7일 실패 알림을 건다(FR-016).
 * 직접 눌러 백업한 뒤에 7일이 지나도 스스로 끈 사람에게는 실패가 아니다.
 */
class AutoBackupNotifier(private val store: BackupStore, private val notifier: BackupNotifier) : BackupNotifier {
    override suspend fun schedule(at: Instant, title: String, body: String) {
        val prefs = store.current()
        if (prefs.accountKey != null && prefs.autoEnabled) notifier.schedule(at, title, body)
    }

    override fun show(title: String, body: String) = notifier.show(title, body)

    override fun cancel() = notifier.cancel()
}
