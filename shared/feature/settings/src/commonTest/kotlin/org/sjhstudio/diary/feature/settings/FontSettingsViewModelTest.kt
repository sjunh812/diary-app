package org.sjhstudio.diary.feature.settings

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.sjhstudio.diary.core.model.DiaryFont
import org.sjhstudio.diary.core.platform.FontInstallState
import org.sjhstudio.diary.core.platform.FontInstaller
import org.sjhstudio.diary.core.testing.FakeSettingsRepository

// 글꼴(G1): 앱에 있는 글꼴은 바로, 받는 글꼴은 받은 뒤에 바뀜
// 받지 못하면 쓰던 글꼴 그대로(docs/design.md 일기 본문 글꼴 10종)
@OptIn(ExperimentalCoroutinesApi::class)
class FontSettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val settings = FakeSettingsRepository()
    private val installer = FakeInstaller()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun bundled_font_changes_right_away() = runTest(dispatcher) {
        val vm = FontSettingsViewModel(settings, installer, io = dispatcher)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

        vm.select(DiaryFont.HAKGYOANSIM_BADASSEUGI)
        advanceUntilIdle()
        assertEquals(DiaryFont.HAKGYOANSIM_BADASSEUGI, settings.settings.first().font)
        assertEquals(emptyList(), installer.requested)
    }

    /** 받아 둔 글꼴 확인은 파일을 읽어 화면 스레드에서 하지 않음. 확인이 끝나면 바로 고를 수 있음 */
    @Test
    fun downloaded_fonts_are_checked_off_the_screen_thread() = runTest(dispatcher) {
        installer.downloaded["sejong_geulggot"] = "/fonts/sejong_geulggot.ttf"
        val vm = FontSettingsViewModel(settings, installer, io = dispatcher)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        assertEquals(0, installer.fileChecks, "만들 때 파일을 읽지 않음")

        advanceUntilIdle()
        assertTrue(vm.state.value.isReady(DiaryFont.SEJONG_GEULGGOT))
    }

    @Test
    fun downloaded_font_changes_after_it_is_installed() = runTest(dispatcher) {
        val vm = FontSettingsViewModel(settings, installer, io = dispatcher)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

        vm.select(DiaryFont.SEJONG_GEULGGOT)
        advanceUntilIdle()
        installer.emit(FontInstallState.Downloading(0.5f))
        advanceUntilIdle()
        assertEquals(0.5f, vm.state.value.downloading[DiaryFont.SEJONG_GEULGGOT])
        assertEquals(DiaryFont.PRETENDARD, settings.settings.first().font)

        installer.emit(FontInstallState.Installed("/fonts/sejong_geulggot.ttf"))
        advanceUntilIdle()
        assertEquals(DiaryFont.SEJONG_GEULGGOT, settings.settings.first().font)
        assertTrue(vm.state.value.isReady(DiaryFont.SEJONG_GEULGGOT))
        assertEquals(listOf("sejong_geulggot"), installer.requested)
    }

    /** 받는 동안 다른 글꼴을 고르면 받기가 끝나도 그 글꼴을 둠. 고른 것을 말없이 바꾸지 않음 */
    @Test
    fun a_finished_download_does_not_replace_a_font_picked_later() = runTest(dispatcher) {
        val vm = FontSettingsViewModel(settings, installer, io = dispatcher)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

        vm.select(DiaryFont.SEJONG_GEULGGOT)
        advanceUntilIdle()
        vm.select(DiaryFont.HAKGYOANSIM_BADASSEUGI)
        advanceUntilIdle()
        installer.emit(FontInstallState.Installed("/fonts/sejong_geulggot.ttf"))
        advanceUntilIdle()

        assertEquals(DiaryFont.HAKGYOANSIM_BADASSEUGI, settings.settings.first().font)
        assertTrue(vm.state.value.isReady(DiaryFont.SEJONG_GEULGGOT))
    }

    @Test
    fun offline_keeps_the_current_font_and_tells_once() = runTest(dispatcher) {
        val vm = FontSettingsViewModel(settings, installer, io = dispatcher)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        val notices = mutableListOf<FontNotice>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.notice.collect { notices += it } }

        vm.select(DiaryFont.KIMJUNGCHUL_MYUNGJO)
        advanceUntilIdle()
        installer.emit(FontInstallState.Failed(offline = true))
        advanceUntilIdle()
        assertEquals(DiaryFont.PRETENDARD, settings.settings.first().font)
        assertEquals(listOf(FontNotice.Offline), notices)
        assertTrue(DiaryFont.KIMJUNGCHUL_MYUNGJO !in vm.state.value.downloading)
    }

    private class FakeInstaller : FontInstaller {
        val requested = mutableListOf<String>()
        private val results = MutableSharedFlow<FontInstallState>(extraBufferCapacity = 8)

        suspend fun emit(state: FontInstallState) = results.emit(state)

        val downloaded = mutableMapOf<String, String>()
        var fileChecks = 0

        override fun installedPath(id: String): String? {
            fileChecks++
            return downloaded[id]
        }

        override fun knownPath(id: String): String? = null

        override fun install(id: String): Flow<FontInstallState> {
            requested += id
            var done = false
            return results.takeWhile { !done }.also { }.let { flow ->
                kotlinx.coroutines.flow.flow {
                    flow.collect { state ->
                        emit(state)
                        if (state !is FontInstallState.Downloading) done = true
                    }
                }
            }
        }
    }
}
