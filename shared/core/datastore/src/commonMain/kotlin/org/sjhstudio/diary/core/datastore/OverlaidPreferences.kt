package org.sjhstudio.diary.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

/**
 * 쓴 값을 기기 기록보다 먼저 이 프로세스에 반영하는 설정 기록
 * 공간이 차 기록을 못 써도 이 프로세스 안에서는 쓴 값을 읽음. 다음 실행은 기록에 남은 값부터
 * 하루 한 번 광고, 안내에 답함처럼 다시 뜨면 안 되는 기록용
 * 같은 값을 다시 써도 결과가 같은 쓰기만 넘김(덧셈처럼 쌓이는 쓰기는 두 번 반영될 수 있음)
 */
internal class OverlaidPreferences(private val dataStore: DataStore<Preferences>) {
    /** 아직 기록에 닿지 않은 쓰기. 닿으면 빠지고, 실패하면 이 프로세스가 끝날 때까지 남음 */
    private val unsaved = MutableStateFlow<List<(MutablePreferences) -> Unit>>(emptyList())

    val data: Flow<Preferences> =
        combine(dataStore.data, unsaved) { saved, pending ->
            if (pending.isEmpty()) saved else saved.toMutablePreferences().apply { pending.forEach { it(this) } }
        }

    /** 먼저 이 프로세스에 반영하고 기록에 씀. 기록 실패는 그대로 던짐 */
    suspend fun edit(change: (MutablePreferences) -> Unit) {
        unsaved.update { it + change }
        dataStore.edit(change)
        unsaved.update { pending -> pending.filterNot { it === change } }
    }
}
