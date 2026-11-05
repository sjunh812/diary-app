package org.sjhstudio.diary.core.backup

import kotlinx.coroutines.sync.Mutex

/**
 * 백업과 복원이 같은 일기를 동시에 만지지 않게 막는 잠금 하나(스펙 003 T037).
 * 복원이 일기를 바꾸는 동안 백업이 스냅샷을 뜨면 반쯤 바뀐 기록이 가장 새 백업이 된다.
 */
class BackupGate {
    private val lock = Mutex()

    /** 다른 일이 돌고 있으면 기다리지 않고 false다. */
    fun tryEnter(): Boolean = lock.tryLock()

    /** 앞선 일이 끝날 때까지 기다린다. */
    suspend fun enter() = lock.lock()

    fun leave() = lock.unlock()
}
