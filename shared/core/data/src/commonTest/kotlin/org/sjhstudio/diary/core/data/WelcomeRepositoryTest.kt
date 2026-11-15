package org.sjhstudio.diary.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okio.IOException
import org.sjhstudio.diary.core.datastore.WelcomeStore
import org.sjhstudio.diary.core.model.ContextTip
import org.sjhstudio.diary.core.model.WelcomeStep
import org.sjhstudio.diary.core.testing.InMemoryDataStore

class WelcomeRepositoryTest {
    private val store = WelcomeStore(InMemoryDataStore(emptyPreferences()))

    private fun TestScope.repository(migrated: Int? = null) = DefaultWelcomeRepository(store, this) { migrated }

    @Test
    fun new_user_sees_only_the_onboarding() = runTest {
        val repository = repository()

        assertEquals(WelcomeStep.ONBOARDING_NEW, repository.step())
        repository.onboardingFinished()
        assertEquals(WelcomeStep.DONE, repository.step())
    }

    @Test
    fun updated_user_goes_letter_then_onboarding_then_home() = runTest {
        val repository = repository(migrated = 1204)

        assertEquals(WelcomeStep.LETTER, repository.step())
        repository.letterRead()
        assertEquals(WelcomeStep.ONBOARDING_UPDATED, repository.step())
        repository.onboardingFinished()
        assertEquals(WelcomeStep.DONE, repository.step())
    }

    @Test
    fun turning_the_app_off_after_the_letter_resumes_at_the_onboarding() = runTest {
        repository(migrated = 3).letterRead()

        assertEquals(WelcomeStep.ONBOARDING_UPDATED, repository(migrated = 3).step())
    }

    @Test
    fun each_tip_stays_on_its_own_until_closed() = runTest {
        val session = repository()
        assertEquals(null, session.isOpen(ContextTip.INSIGHT_OPEN))
        session.load()

        // 화면을 오가도 닫기 전까지 남고, 다른 안내도 따로 뜸
        assertEquals(true, session.isOpen(ContextTip.INSIGHT_OPEN))
        session.load()
        assertEquals(true, session.isOpen(ContextTip.INSIGHT_OPEN))
        assertEquals(true, session.isOpen(ContextTip.HOME_LIST_STYLE))

        session.done(ContextTip.INSIGHT_OPEN)
        assertEquals(false, session.isOpen(ContextTip.INSIGHT_OPEN))
        testScheduler.advanceUntilIdle()

        val next = repository()
        next.load()
        assertEquals(false, next.isOpen(ContextTip.INSIGHT_OPEN))
        assertEquals(true, next.isOpen(ContextTip.HOME_LIST_STYLE))
    }

    @Test
    fun unreadable_tips_show_nothing_instead_of_crashing() = runTest {
        val broken =
            object : DataStore<Preferences> {
                override val data: Flow<Preferences> = flow { throw IOException("읽을 수 없음") }

                override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
                    throw IOException("읽을 수 없음")
            }
        val repository = DefaultWelcomeRepository(WelcomeStore(broken), this) { null }
        repository.load()
        assertEquals(false, repository.isOpen(ContextTip.INSIGHT_OPEN))
        // 닫기 기록 실패도 앱을 끄지 않음
        repository.done(ContextTip.INSIGHT_OPEN)
        testScheduler.advanceUntilIdle()
    }

    @Test
    fun closing_a_tip_is_written_by_the_repository_even_when_writing_is_slow() = runTest {
        // 부른 화면은 기다리지 않음
        // 쓰기가 늦어도 저장소 범위에서 끝까지
        val gate = CompletableDeferred<Unit>()
        val base = InMemoryDataStore(emptyPreferences())
        val slow =
            object : DataStore<Preferences> by base {
                override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
                    gate.await()
                    return base.updateData(transform)
                }
            }
        val repository = DefaultWelcomeRepository(WelcomeStore(slow), this) { null }
        repository.load()
        repository.done(ContextTip.HOME_LIST_STYLE)
        assertEquals(false, repository.isOpen(ContextTip.HOME_LIST_STYLE))
        gate.complete(Unit)
        testScheduler.advanceUntilIdle()
        val next = DefaultWelcomeRepository(WelcomeStore(base), this) { null }
        next.load()
        assertEquals(false, next.isOpen(ContextTip.HOME_LIST_STYLE))
    }
}
