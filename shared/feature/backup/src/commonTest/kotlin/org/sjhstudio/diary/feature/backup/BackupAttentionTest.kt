package org.sjhstudio.diary.feature.backup

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import org.sjhstudio.diary.core.model.BackupConnection
import org.sjhstudio.diary.core.model.BackupProgress
import org.sjhstudio.diary.core.model.BackupStatus
import org.sjhstudio.diary.core.model.BackupStep

/** 접힌 바의 백업 경고는 홈 백업 줄의 빨간 점과 같은 때만 보임 */
class BackupAttentionTest {
    private val now = Instant.parse("2026-11-08T09:00:00Z")
    private val connected = BackupConnection.Connected("me")

    @Test
    fun reconnect_needs_attention() {
        assertEquals(BackupAttention.Reconnect, attentionOf(BackupStatus(connection = BackupConnection.NeedsReconnect), false, now))
    }

    @Test
    fun failing_for_more_than_a_week_needs_attention() {
        val status = BackupStatus(connection = connected, lastSuccessAt = now - 10.days, failingSince = now - 8.days)
        assertEquals(BackupAttention.LongFailure, attentionOf(status, false, now))
    }

    @Test
    fun a_short_failure_or_a_healthy_backup_stays_quiet() {
        assertNull(attentionOf(BackupStatus(connection = connected, lastSuccessAt = now - 3.days, failingSince = now - 2.days), false, now))
        assertNull(attentionOf(BackupStatus(connection = connected, lastSuccessAt = now - 1.days), false, now))
        // 백업을 켜지 않은 것은 고장이 아니라 선택이라 경고하지 않음
        assertNull(attentionOf(BackupStatus(), false, now))
    }

    @Test
    fun no_warning_while_the_line_says_something_else() {
        val failing = BackupStatus(connection = connected, lastSuccessAt = now - 10.days, failingSince = now - 8.days)
        assertNull(attentionOf(failing.copy(running = BackupProgress(BackupStep.Preparing)), false, now))
        assertNull(attentionOf(failing.copy(findingBackup = true), false, now))
        assertNull(attentionOf(failing, true, now))
    }
}
