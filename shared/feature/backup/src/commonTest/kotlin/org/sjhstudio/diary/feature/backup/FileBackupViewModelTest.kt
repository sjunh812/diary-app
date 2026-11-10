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
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okio.Buffer
import okio.IOException
import org.sjhstudio.diary.core.model.BackupEntry
import org.sjhstudio.diary.core.model.BackupEntryState
import org.sjhstudio.diary.core.model.RestoreFailure
import org.sjhstudio.diary.core.model.RestorePreparation
import org.sjhstudio.diary.core.model.RestorePreview
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

    /** 화면 모델이 입출력 스레드를 거쳐 시험 시계가 아니라 실제 시간으로 기다림 */
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
        restore.bundle.writeFailure = IOException("공간이 없다")
        val viewModel = viewModel()
        var discarded = false

        viewModel.write("a.zip", { written() }, { discarded = true })
        waitUntil { viewModel.state.value is FileBackupUi.WriteFailed }

        assertTrue(discarded)
        assertEquals(FileBackupUi.WriteFailed(left = false), viewModel.state.value)
    }

    @Test
    fun a_failed_file_that_cannot_be_removed_asks_the_user_to_delete_it() = runTest {
        restore.bundle.writeFailure = IOException("공간이 없다")
        val viewModel = viewModel()

        viewModel.write("a.zip", { written() }, { throw IOException("지울 수 없다") })
        waitUntil { viewModel.state.value is FileBackupUi.WriteFailed }

        assertEquals(FileBackupUi.WriteFailed(left = true), viewModel.state.value)
    }

    @Test
    fun stopping_removes_the_file_and_forgets_it() = runTest {
        restore.bundle.holdWrite = true
        val viewModel = viewModel()
        var discarded = false
        viewModel.write("a.zip", { written() }, { discarded = true })
        waitUntil { restore.bundle.writing }

        viewModel.stop()

        waitUntil { discarded }
        assertEquals(FileBackupUi.Idle, viewModel.state.value)
        assertEquals(1, restore.bundle.bundleForgets)
    }

    @Test
    fun leaving_by_another_way_removes_the_file_too() = runTest {
        restore.bundle.holdWrite = true
        val viewModel = viewModel()
        var discarded = false
        viewModel.write("a.zip", { written() }, { discarded = true })
        waitUntil { restore.bundle.writing }

        store.clear()

        waitUntil { discarded }
        assertEquals(0, restore.bundle.bundleForgets)
    }

    /** 확인이 끝나면 미리보기로 가라는 이벤트. 콜백을 쥐지 않아 화면이 다시 만들어져도 받음 */
    @Test
    fun a_checked_file_announces_ready() = runTest {
        val entry = BackupEntry("f", "d", "기기", "android", Instant.fromEpochSeconds(0), 1, 0, 0, "2.0.0", BackupEntryState.Ready)
        restore.preparation = RestorePreparation.Ready(entry, RestorePreview(1, 0, 0, 0, 0, 0, 0, false, 1_000))
        val viewModel = viewModel()
        var ready = 0
        val collecting = backgroundScope.launch(Dispatchers.Default) { viewModel.ready.collect { ready++ } }

        viewModel.read({ Buffer() })
        waitUntil { ready == 1 }

        assertEquals(FileBackupUi.Idle, viewModel.state.value)
        collecting.cancel()
    }

    @Test
    fun a_lack_of_space_reaches_the_screen_with_the_number() = runTest {
        restore.preparation = RestorePreparation.Failed(RestoreFailure.NO_SPACE, 5_000)
        val viewModel = viewModel()

        viewModel.read({ Buffer() }, { 1_234L })
        waitUntil { viewModel.state.value != FileBackupUi.Reading }

        assertEquals(1_234L, restore.preparedFileSize)
        assertEquals(FileBackupUi.ReadFailed(RestoreFailure.NO_SPACE, 5_000), viewModel.state.value)
    }

    /** 끝난 화면은 어림값이 아니라 실제로 쓴 크기를 보임 */
    @Test
    fun the_saved_size_is_what_was_written() = runTest {
        restore.bundle.bundleBytes = 4_321
        val viewModel = viewModel()
        val target = written()

        viewModel.write("a.zip", { target }, {})
        waitUntil { viewModel.state.value is FileBackupUi.Saved }

        assertEquals(FileBackupUi.Saved("a.zip", 4_321), viewModel.state.value)
        assertEquals(4_321, target.size)
    }

    /** 저장 자리를 열지 못해도 꺼지지 않고, 지울 파일이 없으니 지우라고 하지 않음 */
    @Test
    fun a_place_that_cannot_be_opened_fails_without_a_file_left() = runTest {
        val viewModel = viewModel()

        viewModel.write("a.zip", { throw IOException("열 수 없다") }, {})
        waitUntil { viewModel.state.value is FileBackupUi.WriteFailed }

        assertEquals(FileBackupUi.WriteFailed(left = false), viewModel.state.value)
    }

    /** 고른 파일을 열지 못하면 파일이 틀렸다고 하지 않고 받지 못했다고 알림 */
    @Test
    fun a_picked_file_that_cannot_be_opened_is_not_called_a_wrong_file() = runTest {
        val viewModel = viewModel()

        viewModel.read({ throw IOException("열 수 없다") })
        waitUntil { viewModel.state.value != FileBackupUi.Reading }

        assertEquals(FileBackupUi.ReadFailed(RestoreFailure.OTHER), viewModel.state.value)
    }

    /** 크기를 묻다 실패해도 읽기는 이어 감 */
    @Test
    fun an_unknown_size_still_reads_the_file() = runTest {
        restore.preparation = RestorePreparation.Failed(RestoreFailure.NO_SPACE, 5_000)
        val viewModel = viewModel()

        viewModel.read({ Buffer() }, { throw IOException("크기를 모른다") })
        waitUntil { viewModel.state.value != FileBackupUi.Reading }

        // 저장소까지 가서 공간 답을 받음
        assertEquals(FileBackupUi.ReadFailed(RestoreFailure.NO_SPACE, 5_000), viewModel.state.value)
        assertEquals(null, restore.preparedFileSize)
    }
}
