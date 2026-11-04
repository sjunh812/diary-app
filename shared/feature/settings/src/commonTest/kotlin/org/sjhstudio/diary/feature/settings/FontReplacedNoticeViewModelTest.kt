package org.sjhstudio.diary.feature.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import org.sjhstudio.diary.core.model.AppSettings
import org.sjhstudio.diary.core.model.DiaryFont
import org.sjhstudio.diary.core.testing.FakeSettingsRepository

// 옛 글꼴이 바뀌었다는 안내는 한 번만 띄운다(docs/design.md 일기 본문 글꼴).
class FontReplacedNoticeViewModelTest {
    @Test
    fun replaced_font_is_told_once() = runTest {
        val settings = FakeSettingsRepository(AppSettings(font = DiaryFont.NANUM_PEN, fontReplacedFrom = "교보손글씨체"))
        val viewModel = FontReplacedNoticeViewModel(settings)

        assertEquals(DiaryFont.NANUM_PEN, viewModel.take())
        assertNull(settings.settings.value.fontReplacedFrom)
        assertNull(viewModel.take())
    }

    @Test
    fun nothing_to_tell_when_the_font_was_kept() = runTest {
        assertNull(FontReplacedNoticeViewModel(FakeSettingsRepository(AppSettings(font = DiaryFont.NANUM_CHEER))).take())
    }
}
