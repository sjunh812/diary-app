package org.sjhstudio.diary.core.data

import androidx.datastore.preferences.core.emptyPreferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.sjhstudio.diary.core.datastore.WelcomeStore
import org.sjhstudio.diary.core.model.ContextTip
import org.sjhstudio.diary.core.model.WelcomeStep
import org.sjhstudio.diary.core.testing.InMemoryDataStore

class WelcomeRepositoryTest {
    private val store = WelcomeStore(InMemoryDataStore(emptyPreferences()))

    private fun repository(migrated: Int? = null) = DefaultWelcomeRepository(store) { migrated }

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
    fun reading_the_letter_tells_the_ad_side_once() = runTest {
        var told = 0
        val repository = DefaultWelcomeRepository(store, onLetterRead = { told++ }) { 3 }

        repository.letterRead()

        assertEquals(1, told)
    }

    @Test
    fun one_tip_per_session_and_never_again_once_closed() = runTest {
        val session = repository()

        assertTrue(session.claim(ContextTip.INSIGHT_OPEN))
        assertFalse(session.claim(ContextTip.INSIGHT_OPEN))

        val next = repository()
        assertTrue(next.claim(ContextTip.INSIGHT_OPEN))
        next.done(ContextTip.INSIGHT_OPEN)

        assertFalse(repository().claim(ContextTip.INSIGHT_OPEN))
    }
}
