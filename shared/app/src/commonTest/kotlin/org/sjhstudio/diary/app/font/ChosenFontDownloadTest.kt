package org.sjhstudio.diary.app.font

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.sjhstudio.diary.core.model.AppSettings
import org.sjhstudio.diary.core.model.DiaryFont
import org.sjhstudio.diary.core.platform.FontInstallState
import org.sjhstudio.diary.core.platform.FontInstaller
import org.sjhstudio.diary.core.testing.FakeSettingsRepository

// 고른 글꼴이 받는 글꼴인데 없을 때만 받음(스펙 001 T067)
class ChosenFontDownloadTest {
    private class RecordingInstaller(private val installed: Set<String> = emptySet()) : FontInstaller {
        val requested = mutableListOf<String>()
        var kept: Set<String>? = null

        override fun installedPath(id: String): String? = if (id in installed) "/fonts/$id.ttf" else null

        override fun install(id: String): Flow<FontInstallState> {
            requested += id
            return flowOf(FontInstallState.Downloading(0.5f), FontInstallState.Installed("/fonts/$id.ttf"))
        }

        override fun removeExcept(ids: Set<String>) {
            kept = ids
        }
    }

    @Test
    fun downloads_a_chosen_font_that_is_not_installed() = runTest {
        val installer = RecordingInstaller()
        ChosenFontDownload(FakeSettingsRepository(AppSettings(font = DiaryFont.SEJONG_GEULGGOT)), installer, backgroundScope).start()
        testScheduler.runCurrent()
        assertEquals(listOf("sejong_geulggot"), installer.requested)
    }

    @Test
    fun skips_bundled_and_installed_fonts() = runTest {
        val installer = RecordingInstaller(installed = setOf("kimjungchul_myungjo"))
        ChosenFontDownload(FakeSettingsRepository(AppSettings(font = DiaryFont.NANUM_CHEER)), installer, backgroundScope).start()
        ChosenFontDownload(FakeSettingsRepository(AppSettings(font = DiaryFont.KIMJUNGCHUL_MYUNGJO)), installer, backgroundScope).start()
        testScheduler.runCurrent()
        assertEquals(emptyList(), installer.requested)
    }

    @Test
    fun removes_files_of_fonts_taken_out_of_the_app() = runTest {
        val installer = RecordingInstaller()
        ChosenFontDownload(FakeSettingsRepository(AppSettings()), installer, backgroundScope).start()
        testScheduler.runCurrent()
        val kept = checkNotNull(installer.kept)
        assertTrue("sejong_geulggot" in kept && "gowun_batang" in kept)
        assertFalse("gamja_flower" in kept || "hi_melody" in kept || "gowun_dodum" in kept)
    }
}
