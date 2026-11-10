package org.sjhstudio.diary.core.testing

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** 파일 없이 메모리에만 두는 DataStore. 저장소 시험에서 실제 SettingsStore·LockStore를 감싸 씀 */
class InMemoryDataStore<T>(initial: T) : DataStore<T> {
    private val state = MutableStateFlow(initial)
    private val mutex = Mutex()

    /** true면 저장 공간이 꽉 찬 기기처럼 읽기는 되고 쓰기가 실패함 */
    var failWrites = false

    override val data: Flow<T> = state

    override suspend fun updateData(transform: suspend (t: T) -> T): T = mutex.withLock {
        val next = transform(state.value)
        check(!failWrites) { "저장 공간이 없다" }
        state.value = next
        next
    }
}
