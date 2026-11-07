package org.sjhstudio.diary.core.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class FormatsTest {
    @Test
    fun grouping() {
        assertEquals(listOf("0", "999", "1,284", "12,345", "1,000,000"), listOf(0, 999, 1284, 12345, 1_000_000).map { it.grouped() })
    }

    @Test
    fun bytes() {
        assertEquals("1KB", formatBytes(1))
        assertEquals("637KB", formatBytes(636_535))
        assertEquals("1MB", formatBytes(999_500))
        assertEquals("1.2MB", formatBytes(1_150_000))
        assertEquals("10MB", formatBytes(9_950_000))
        assertEquals("850MB", formatBytes(850_000_000))
        assertEquals("1GB", formatBytes(999_500_000))
        assertEquals("1.2GB", formatBytes(1_150_000_000))
    }
}
