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
        assertEquals("1MB", formatBytes(1))
        assertEquals("850MB", formatBytes(850L * 1024 * 1024))
        assertEquals("1.2GB", formatBytes((1.15 * 1024 * 1024 * 1024).toLong()))
    }
}
