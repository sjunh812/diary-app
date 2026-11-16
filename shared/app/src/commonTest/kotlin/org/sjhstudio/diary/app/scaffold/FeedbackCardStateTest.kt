package org.sjhstudio.diary.app.scaffold

import androidx.datastore.preferences.core.emptyPreferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.sjhstudio.diary.core.data.PromptRepository
import org.sjhstudio.diary.core.datastore.PromptStore
import org.sjhstudio.diary.core.testing.InMemoryDataStore

// 의견 카드는 일기 DB를 셈. 기록 이전이 끝나기 전에 DB를 열면 이전이 실패함
class FeedbackCardStateTest {
    @Test
    fun the_diary_is_counted_only_once_home_reads_the_card() = runTest(UnconfinedTestDispatcher()) {
        var counted = 0
        val prompts = PromptRepository(
            store = PromptStore(InMemoryDataStore(emptyPreferences())),
            writtenSince = {
                counted++
                flowOf(0)
            },
            review = { false }
        )
        prompts.start()
        val state = FeedbackCardState(prompts, backgroundScope)
        assertEquals(0, counted)

        backgroundScope.launch { state.visible.collect {} }
        assertEquals(1, counted)
        assertEquals(false, state.visible.value)
    }
}
