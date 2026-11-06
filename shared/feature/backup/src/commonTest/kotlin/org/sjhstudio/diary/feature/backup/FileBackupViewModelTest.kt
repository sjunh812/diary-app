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
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okio.Buffer
import org.sjhstudio.diary.core.model.RestoreFailure
import org.sjhstudio.diary.core.model.RestorePreparation
import org.sjhstudio.diary.core.testing.FakeRestoreRepository

@OptIn(ExperimentalCoroutinesApi::class)
class FileBackupViewModelTest {
    private val restore = FakeRestoreRepository()
    private val store = ViewModelStore()

    @BeforeTest
    fun useTestMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun resetTestMain() {
        store.clear()
        Dispatchers.resetMain()
    }

    private fun viewModel(): FileBackupViewModel {
        val factory = viewModelFactory { initializer { FileBackupViewModel(restore) } }
        return ViewModelProvider.create(store, factory)[FileBackupViewModel::class]
    }

    /** 화면 모델은 입출력 스레드를 거친다. 시험 시계가 아니라 실제 시간으로 기다린다. */
    private suspend fun waitUntil(check: () -> Boolean) = withContext(Dispatchers.Default) {
        withTimeout(5_000) {
            while (!check()) delay(5)
        }
    }

    @Test
    fun a_lack_of_space_reaches_the_screen_with_the_number() = runTest {
        restore.preparation = RestorePreparation.Failed(RestoreFailure.NO_SPACE, 5_000)
        val viewModel = viewModel()

        viewModel.read({ Buffer() }, { 1_234L }) {}
        waitUntil { viewModel.state.value != FileBackupUi.Reading }

        assertEquals(1_234L, restore.preparedFileSize)
        assertEquals(FileBackupUi.ReadFailed(RestoreFailure.NO_SPACE, 5_000), viewModel.state.value)
    }
}
