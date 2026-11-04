package org.sjhstudio.diary.core.backup

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ManifestCodecTest {
    private val manifest =
        Manifest(
            createdAt = 1798720200000,
            timeZone = "Asia/Seoul",
            app = ManifestApp("2.0.0", 37, "android"),
            device = ManifestDevice("2f1c", "Galaxy S24"),
            snapshot = ManifestSnapshot("a.db", 10, "aa", 1),
            counts = ManifestCounts(entries = 3, mediaRows = 2),
            media = listOf(ManifestMedia("3f9a", "jpg", 5, true)),
            settings = ManifestSettings(font = "pretendard", lockEnabled = true)
        )

    @Test
    fun round_trip() = assertEquals(manifest, ManifestCodec.decode(ManifestCodec.encode(manifest)))

    @Test
    fun unknown_keys_are_ignored_and_missing_keys_use_defaults() {
        val read = assertNotNull(ManifestCodec.decode("""{"format":"org.sjhstudio.diary.backup","formatVersion":2,"future":{"a":1}}"""))
        assertEquals(0, read.counts.entries)
        assertEquals(emptyList(), read.media)
    }

    @Test
    fun broken_file_reads_as_null() {
        assertNull(ManifestCodec.decode("{not json"))
        assertNull(ManifestCodec.decode("""{"format":"other"}"""))
        assertEquals(ManifestCheck.BROKEN, ManifestCodec.check(null))
    }

    @Test
    fun newer_format_or_database_needs_newer_app() {
        assertEquals(ManifestCheck.OK, ManifestCodec.check(manifest))
        assertEquals(ManifestCheck.NEEDS_NEWER_APP, ManifestCodec.check(manifest.copy(formatVersion = 3)))
        assertEquals(ManifestCheck.NEEDS_NEWER_APP, ManifestCodec.check(manifest.copy(snapshot = manifest.snapshot.copy(dbVersion = 2))))
    }

    @Test
    fun no_diary_content_fields_exist() {
        val text = ManifestCodec.encode(manifest)
        for (key in listOf("body", "plainText", "place", "mood", "path", "pin")) assertFalse("\"$key" in text, key)
    }
}
