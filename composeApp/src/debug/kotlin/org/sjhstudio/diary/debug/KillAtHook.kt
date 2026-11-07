package org.sjhstudio.diary.debug

import android.os.Process
import java.io.File
import org.sjhstudio.diary.core.migration.MigrationEvents
import org.sjhstudio.diary.core.model.MigrationStep

/**
 * 개발 빌드만: `files/debug/kill_at`에 적힌 단계(Snapshotting, Copying, Verifying, Committing)에 들어서면 프로세스를 끝낸다
 * (스펙 001 T049, quickstart §3, FR-007).
 * 끊긴 뒤 다시 열면 처음부터 다시 하고 결과가 같아야 한다.
 * 한 번만 끊도록 끊기 전에 파일을 지운다.
 */
class KillAtHook(private val file: File, private val inner: MigrationEvents) : MigrationEvents by inner {
    override fun stepStarted(step: MigrationStep) {
        inner.stepStarted(step)
        val target = runCatching { file.readText().trim() }.getOrNull() ?: return
        if (!target.equals(step.id, ignoreCase = true)) return
        file.delete()
        Process.killProcess(Process.myPid())
    }
}
