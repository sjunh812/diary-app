package org.sjhstudio.diary.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okio.IOException

class OverlaidPreferencesTest {
    private val key = booleanPreferencesKey("answered")

    @Test
    fun a_saved_write_is_read_back_and_kept_on_disk() = runTest {
        val disk = Disk()
        val store = OverlaidPreferences(disk)
        store.edit { it[key] = true }

        assertEquals(true, store.data.first()[key])
        assertEquals(true, disk.data.first()[key])
    }

    /** 공간이 차 못 적어도 이 프로세스 안에서는 쓴 값을 읽고, 다음 실행은 기록에 남은 값부터 */
    @Test
    fun an_unsaved_write_lives_only_in_this_run() = runTest {
        val disk = Disk(failWrites = true)
        val store = OverlaidPreferences(disk)
        assertFailsWith<IOException> { store.edit { it[key] = true } }

        assertEquals(true, store.data.first()[key])
        assertNull(OverlaidPreferences(disk).data.first()[key])
    }

    /** 못 쓴 옛 값이 나중에 쓴 값을 덮지 않음. 다음 쓰기 때 함께 기록에 남음 */
    @Test
    fun a_later_write_wins_over_an_earlier_one_that_failed() = runTest {
        val day = stringPreferencesKey("day")
        val disk = Disk(failWrites = true)
        val store = OverlaidPreferences(disk)
        assertFailsWith<IOException> { store.edit { it[day] = "day1" } }

        disk.failWrites = false
        store.edit { it[day] = "day2" }

        assertEquals("day2", store.data.first()[day])
        assertEquals("day2", disk.data.first()[day])
    }

    /** 앞 쓰기가 기록하는 동안 들어온 쓰기 둘이 모두 남음 */
    @Test
    fun writes_that_arrive_during_another_write_are_all_kept() = runTest {
        val a = stringPreferencesKey("a")
        val b = stringPreferencesKey("b")
        val c = stringPreferencesKey("c")
        val disk = Disk(hold = CompletableDeferred())
        val store = OverlaidPreferences(disk)
        launch { store.edit { it[a] = "A" } }
        runCurrent()
        launch { store.edit { it[b] = "B" } }
        launch { store.edit { it[c] = "C" } }
        runCurrent()

        disk.hold?.complete(Unit)
        advanceUntilIdle()

        val saved = disk.data.first()
        assertEquals(listOf("A", "B", "C"), listOf(saved[a], saved[b], saved[c]))
        assertEquals(saved, store.data.first())
    }
}

/** 쓰기를 실패하게 하거나 첫 쓰기를 붙잡아 둘 수 있는 기록 */
private class Disk(var failWrites: Boolean = false, val hold: CompletableDeferred<Unit>? = null) : DataStore<Preferences> {
    private val state = MutableStateFlow(emptyPreferences())
    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        if (failWrites) throw IOException("저장 공간이 없다")
        hold?.await()
        return transform(state.value).also { state.value = it }
    }
}
