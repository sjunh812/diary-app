package org.sjhstudio.diary.feature.welcome

import androidx.datastore.preferences.core.emptyPreferences
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.sjhstudio.diary.core.data.DefaultWelcomeRepository
import org.sjhstudio.diary.core.data.WelcomeRepository
import org.sjhstudio.diary.core.datastore.WelcomeStore
import org.sjhstudio.diary.core.model.ContextTip
import org.sjhstudio.diary.core.model.WelcomeStep
import org.sjhstudio.diary.core.testing.InMemoryDataStore

/** 편지·온보딩 상태를 읽지 못해도 앱을 엶 */
@OptIn(ExperimentalCoroutinesApi::class)
class WelcomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    private fun viewModel() = WelcomeViewModel(DefaultWelcomeRepository(WelcomeStore(InMemoryDataStore(emptyPreferences()))) { null })

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    /** 저장 공간 문제로 진행 상태를 못 읽어도 꺼지거나 시작 화면에 갇히지 않음 */
    @Test
    fun a_new_install_goes_to_onboarding() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(WelcomeStep.ONBOARDING_NEW, viewModel.step.value)
        viewModel.finish()
        advanceUntilIdle()
        assertEquals(WelcomeStep.DONE, viewModel.step.value)
    }

    @Test
    fun an_unreadable_step_opens_the_app_instead_of_crashing() = runTest(dispatcher) {
        val broken =
            object : WelcomeRepository {
                override suspend fun step(): WelcomeStep = error("읽을 수 없음")

                override suspend fun migratedEntries(): Int? = null

                override suspend fun letterRead() = Unit

                override suspend fun onboardingFinished() = Unit

                override suspend fun claim(tip: ContextTip): Boolean = false

                override suspend fun done(tip: ContextTip) = Unit
            }
        val viewModel = WelcomeViewModel(broken)
        advanceUntilIdle()

        assertEquals(WelcomeStep.DONE, viewModel.step.value)
    }
}
