package org.sjhstudio.diary.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

// 목록 미리보기 방식은 설정과 요금제로 정함(스펙 008)
class PreviewModeTest {
    private val full = ListPreferences(previewLength = PreviewLength.FULL, showFormatting = true)

    @Test
    fun formatting_only_with_plus() {
        assertEquals(PreviewMode(PreviewLength.FULL, formatting = true), PreviewMode.of(full, Plan.PLUS))
        assertEquals(PreviewMode(PreviewLength.FULL), PreviewMode.of(full, Plan.FREE))
        assertEquals(PreviewMode(PreviewLength.FULL), PreviewMode.of(full, null))
    }

    /** 사진 격자는 쓰지 않을 글을 더 읽지 않음 */
    @Test
    fun photo_grid_reads_the_default() {
        assertEquals(PreviewMode.DEFAULT, PreviewMode.of(full.copy(listStyle = ListStyle.PHOTO_GRID), Plan.PLUS))
    }
}
