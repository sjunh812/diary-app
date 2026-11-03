package org.sjhstudio.diary.feature.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import org.sjhstudio.diary.core.model.DiaryFont

class LicenseNoticesTest {
    // 글꼴을 새로 넣으면 고지도 같이 넣어야 한다(docs/release.md 글꼴 라이선스).
    @Test
    fun everyBundledFontHasANotice() {
        val fonts = DiaryFont.entries.filter { it != DiaryFont.SYSTEM }
        assertEquals(fonts.toSet(), FONT_NOTICES.map { it.font }.toSet())
        assertEquals(FONT_NOTICES.size, FONT_NOTICES.map { it.file }.toSet().size)
    }
}
