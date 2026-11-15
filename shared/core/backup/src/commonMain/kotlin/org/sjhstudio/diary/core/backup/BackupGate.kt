package org.sjhstudio.diary.core.backup

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.sync.Mutex

/**
 * 백업과 복원이 같은 일기를 동시에 건드리지 않게 막는 잠금 하나(스펙 003 T037)
 * 복원이 일기를 바꾸는 동안 백업이 스냅샷을 뜨면 반쯤 바뀐 기록이 가장 새 백업이 됨
 * 기다리는 일도 [longWork]에 넣어 앞선 일과 뒤 일 사이에 시스템 시간이 끊기지 않게 함
 * 잠금 밖의 파일 만들기·받아 두기는 [longWork]에 직접 넣음
 */
class BackupGate(val longWork: LongWork = LongWork()) {
    private val lock = Mutex()

    /** 다른 일이 돌고 있으면 기다리지 않고 false */
    fun tryEnter(): Boolean = lock.tryLock().also { if (it) longWork.begin() }

    /** 앞선 일이 끝날 때까지 기다림 */
    suspend fun enter() {
        longWork.begin()
        try {
            lock.lock()
        } catch (error: CancellationException) {
            longWork.end()
            throw error
        }
    }

    fun leave() {
        lock.unlock()
        longWork.end()
    }
}
