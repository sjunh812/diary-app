package org.sjhstudio.diary.feature.backup

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.sjhstudio.diary.core.testing.FakeRestoreRepository

@OptIn(ExperimentalCoroutinesApi::class)
class RestorePreviewViewModelTest {
    private val restore = FakeRestoreRepository()
    private val store = ViewModelStore()

    @BeforeTest
    fun useTestMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun resetTestMain() = Dispatchers.resetMain()

    private fun viewModel(): RestorePreviewViewModel {
        val factory = viewModelFactory { initializer { RestorePreviewViewModel("snapshot", restore) } }
        return ViewModelProvider.create(store, factory)[RestorePreviewViewModel::class]
    }

    @Test
    fun leaving_without_restoring_clears_the_copy() {
        viewModel()

        store.clear()

        assertEquals(1, restore.dismisses)
    }

    @Test
    fun leaving_for_the_progress_screen_keeps_the_restore_running() {
        viewModel().merge()

        store.clear()

        assertEquals(0, restore.dismisses)
    }

    @Test
    fun replacing_also_keeps_the_restore_running() {
        viewModel().replace()

        store.clear()

        assertEquals(0, restore.dismisses)
    }
}
