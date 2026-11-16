package org.sjhstudio.diary.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 쓴 값을 기기 기록보다 먼저 이 프로세스에 반영하는 설정 기록
 * 공간이 차 기록을 못 써도 이 프로세스 안에서는 쓴 값을 읽음. 다음 실행은 기록에 남은 값부터
 * 하루 한 번 광고, 안내에 답함처럼 다시 뜨면 안 되는 기록용
 * 같은 값을 다시 써도 결과가 같은 쓰기만 넘김(덧셈처럼 쌓이는 쓰기는 두 번 반영될 수 있음)
 */
internal class OverlaidPreferences(private val dataStore: DataStore<Preferences>) {
    /** 아직 기록에 닿지 않은 쓰기. 닿으면 빠지고, 실패하면 다음 쓰기가 성공할 때까지 남음 */
    private val unsaved = MutableStateFlow<List<(MutablePreferences) -> Unit>>(emptyList())

    /** 겹친 쓰기가 남은 쓰기를 서로 지우지 않게 한 줄로 */
    private val writing = Mutex()

    val data: Flow<Preferences> =
        combine(dataStore.data, unsaved) { saved, pending ->
            if (pending.isEmpty()) saved else saved.toMutablePreferences().apply { pending.forEach { it(this) } }
        }

    /**
     * 먼저 이 프로세스에 반영하고 기록에 씀. 기록 실패는 그대로 던짐
     * 앞서 못 쓴 쓰기도 순서대로 함께 다시 써, 나중 쓰기가 옛 값에 덮이지 않음
     */
    suspend fun edit(change: (MutablePreferences) -> Unit) {
        unsaved.update { it + change }
        writing.withLock {
            val pending = unsaved.value
            dataStore.edit { prefs -> pending.forEach { it(prefs) } }
            unsaved.update { it.drop(pending.size) }
        }
    }
}
