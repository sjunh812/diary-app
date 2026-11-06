package org.sjhstudio.diary.feature.backup

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
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
import okio.IOException
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

    private fun written() = Buffer()

    @Test
    fun a_finished_file_is_kept() = runTest {
        val viewModel = viewModel()
        var discarded = false

        viewModel.write("a.zip", { written() }, { discarded = true })
        waitUntil { viewModel.state.value is FileBackupUi.Saved }

        assertFalse(discarded)
    }

    @Test
    fun a_failed_file_is_removed_from_where_it_was_saved() = runTest {
        restore.writeFailure = IOException("공간이 없다")
        val viewModel = viewModel()
        var discarded = false

        viewModel.write("a.zip", { written() }, { discarded = true })
        waitUntil { viewModel.state.value is FileBackupUi.WriteFailed }

        assertTrue(discarded)
        assertEquals(FileBackupUi.WriteFailed(left = false), viewModel.state.value)
    }

    @Test
    fun a_failed_file_that_cannot_be_removed_asks_the_user_to_delete_it() = runTest {
        restore.writeFailure = IOException("공간이 없다")
        val viewModel = viewModel()

        viewModel.write("a.zip", { written() }, { throw IOException("지울 수 없다") })
        waitUntil { viewModel.state.value is FileBackupUi.WriteFailed }

        assertEquals(FileBackupUi.WriteFailed(left = true), viewModel.state.value)
    }

    @Test
    fun stopping_removes_the_file_and_forgets_it() = runTest {
        restore.holdWrite = true
        val viewModel = viewModel()
        var discarded = false
        viewModel.write("a.zip", { written() }, { discarded = true })
        waitUntil { restore.writing }

        viewModel.stop()

        waitUntil { discarded }
        assertEquals(FileBackupUi.Idle, viewModel.state.value)
        assertEquals(1, restore.bundleForgets)
    }

    @Test
    fun leaving_by_another_way_removes_the_file_too() = runTest {
        restore.holdWrite = true
        val viewModel = viewModel()
        var discarded = false
        viewModel.write("a.zip", { written() }, { discarded = true })
        waitUntil { restore.writing }

        store.clear()

        waitUntil { discarded }
        assertEquals(0, restore.bundleForgets)
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
