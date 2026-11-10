package org.sjhstudio.diary.feature.settings

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.sjhstudio.diary.core.model.DiaryFont

// 라이선스 목록 라이브러리가 Java 21 빌드라 안드로이드 호스트 JVM(17)에서 못 돌아 iOS 시뮬레이터에서 돌림
// 앱에 든 라이선스 파일을 읽지 못해도 목록이 말없이 사라지거나 앱이 꺼지지 않음
@OptIn(ExperimentalCoroutinesApi::class)
class LicensesViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private val list =
        """{"libraries":[{"uniqueId":"a:b","name":"Alpha","artifactVersion":"1.0","developers":[],"licenses":["MIT"]}],""" +
            """"licenses":{"MIT":{"name":"MIT License","url":"https://opensource.org/licenses/MIT","hash":"MIT"}}}"""

    @Test
    fun an_unreadable_list_shows_a_retry_and_trying_again_reads_it() = runTest {
        var broken = true
        val viewModel =
            LicensesViewModel(readList = { if (broken) error("읽을 수 없음") else list }, background = StandardTestDispatcher(testScheduler))
        backgroundScope.launch { viewModel.libraries.collect {} }
        advanceUntilIdle()
        assertTrue(viewModel.failed.value)
        assertNull(viewModel.libraries.value)

        broken = false
        viewModel.retry()
        advanceUntilIdle()

        assertFalse(viewModel.failed.value)
        assertEquals(listOf("Alpha"), viewModel.libraries.value!!.map { it.name })
    }

    @Test
    fun an_unreadable_full_text_returns_nothing_instead_of_throwing() = runTest {
        val viewModel = LicensesViewModel(readList = { error("읽을 수 없음") }, readFont = { error("읽을 수 없음") })

        assertNull(viewModel.libraryText("a:b"))
        assertNull(viewModel.fontText(FONT_NOTICES.first { it.font != DiaryFont.SYSTEM }))
    }

    @Test
    fun a_readable_full_text_comes_back() = runTest {
        val viewModel = LicensesViewModel(readList = { list }, readFont = { "Copyright\nline" })

        assertEquals("Alpha", viewModel.libraryText("a:b")?.name)
        assertTrue(viewModel.fontText(FONT_NOTICES.first()).orEmpty().startsWith("Copyright"))
    }
}
