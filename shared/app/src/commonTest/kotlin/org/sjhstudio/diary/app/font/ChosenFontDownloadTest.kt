package org.sjhstudio.diary.app.font

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.sjhstudio.diary.core.model.AppSettings
import org.sjhstudio.diary.core.model.DiaryFont
import org.sjhstudio.diary.core.platform.FontInstallState
import org.sjhstudio.diary.core.platform.FontInstaller
import org.sjhstudio.diary.core.testing.FakeSettingsRepository

// 고른 글꼴이 받는 글꼴인데 없을 때만 받는다(스펙 001 T067).
class ChosenFontDownloadTest {
    private class RecordingInstaller(private val installed: Set<String> = emptySet()) : FontInstaller {
        val requested = mutableListOf<String>()

        override fun installedPath(id: String): String? = if (id in installed) "/fonts/$id.ttf" else null

        override fun install(id: String): Flow<FontInstallState> {
            requested += id
            return flowOf(FontInstallState.Downloading(0.5f), FontInstallState.Installed("/fonts/$id.ttf"))
        }
    }

    @Test
    fun downloads_a_chosen_font_that_is_not_installed() = runTest {
        val installer = RecordingInstaller()
        ChosenFontDownload(FakeSettingsRepository(AppSettings(font = DiaryFont.GAMJA_FLOWER)), installer, backgroundScope).start()
        testScheduler.runCurrent()
        assertEquals(listOf("gamja_flower"), installer.requested)
    }

    @Test
    fun skips_bundled_and_installed_fonts() = runTest {
        val installer = RecordingInstaller(installed = setOf("hi_melody"))
        ChosenFontDownload(FakeSettingsRepository(AppSettings(font = DiaryFont.KKOMI)), installer, backgroundScope).start()
        ChosenFontDownload(FakeSettingsRepository(AppSettings(font = DiaryFont.HI_MELODY)), installer, backgroundScope).start()
        testScheduler.runCurrent()
        assertEquals(emptyList(), installer.requested)
    }
}
