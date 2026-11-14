package org.sjhstudio.diary.feature.backup

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.sjhstudio.diary.core.data.RestoreRepository
import org.sjhstudio.diary.core.testing.FakeRestoreRepository

// 파일 백업이 멈췄다는 안내는 한 번만 꺼내고, 꺼낸 뒤 화면이 다시 만들어져도 잃지 않음
@OptIn(ExperimentalCoroutinesApi::class)
class BundleNoticeViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun a_stopped_bundle_is_told_once() = runTest(dispatcher) {
        val restore = FakeRestoreRepository().apply { bundle.stoppedBundle = true }
        val viewModel = BundleNoticeViewModel(restore)
        advanceUntilIdle()
        assertTrue(viewModel.pending.value)

        viewModel.shown()
        assertFalse(viewModel.pending.value)
        // 저장소에서는 이미 꺼냈으니 다음 실행에도 다시 뜨지 않음
        assertFalse(BundleNoticeViewModel(restore).also { advanceUntilIdle() }.pending.value)
    }

    @Test
    fun an_unreadable_flag_does_not_crash() = runTest(dispatcher) {
        val broken = object : RestoreRepository by FakeRestoreRepository() {
            override suspend fun takeStoppedBundle(): Boolean = error("읽을 수 없음")
        }
        val viewModel = BundleNoticeViewModel(broken)
        advanceUntilIdle()
        assertFalse(viewModel.pending.value)
    }
}
