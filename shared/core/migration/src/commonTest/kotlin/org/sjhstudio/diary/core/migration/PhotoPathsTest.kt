package org.sjhstudio.diary.core.migration

import kotlin.test.Test
import kotlin.test.assertEquals

// contracts/legacy-mapping.md "사진"(스펙 001 T034)
class PhotoPathsTest {
    @Test
    fun fragments_drop_empty_pieces_and_keep_order() {
        assertEquals(emptyList(), PhotoPaths.fragments(null))
        assertEquals(emptyList(), PhotoPaths.fragments(""))
        assertEquals(listOf("/a.jpg", "/b.jpg"), PhotoPaths.fragments("/a.jpg,/b.jpg,"))
        assertEquals(listOf("/a.jpg", " /b.jpg"), PhotoPaths.fragments("/a.jpg,, /b.jpg, ,"))
    }

    @Test
    fun trimmed_path_is_tried_once_more() {
        assertEquals(listOf("/a.jpg"), PhotoPaths.candidates("/a.jpg"))
        assertEquals(listOf(" /a.jpg ", "/a.jpg"), PhotoPaths.candidates(" /a.jpg "))
    }

    @Test
    fun extension_from_path_then_head_bytes() {
        assertEquals("jpg", PhotoPaths.extension("/x/IMG.JPG", ByteArray(0)))
        assertEquals("png", PhotoPaths.extension("/x/a.png ", ByteArray(0)))
        val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())
        val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A)
        val webp = "RIFF\u0000\u0000\u0000\u0000WEBPVP8 ".encodeToByteArray()
        val heic = "\u0000\u0000\u0000\u0018ftypheic".encodeToByteArray()
        val mif1 = "\u0000\u0000\u0000\u0018ftypmif1".encodeToByteArray()
        assertEquals("jpg", PhotoPaths.extension("/cache/1699999999", jpeg))
        assertEquals("png", PhotoPaths.extension("/cache/photo.tmp", png))
        assertEquals("webp", PhotoPaths.extension("/cache/a", webp))
        assertEquals("heic", PhotoPaths.extension("/cache/a", heic))
        assertEquals("heic", PhotoPaths.extension("/cache/a", mif1))
        assertEquals("bin", PhotoPaths.extension("/cache/a", "hello".encodeToByteArray()))
        assertEquals("bin", PhotoPaths.extension("/cache/.hidden", ByteArray(0)))
    }

    @Test
    fun file_name_has_no_directory() {
        assertEquals("abc.jpg", PhotoPaths.fileName("abc", "jpg"))
    }
}
