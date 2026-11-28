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
        assertEquals(ManifestCheck.OK, ManifestCodec.check(manifest.copy(formatVersion = 3)))
        assertEquals(ManifestCheck.OK, ManifestCodec.check(manifest.copy(formatVersion = 4)))
        assertEquals(ManifestCheck.NEEDS_NEWER_APP, ManifestCodec.check(manifest.copy(formatVersion = 5)))
        assertEquals(ManifestCheck.NEEDS_NEWER_APP, ManifestCodec.check(manifest.copy(snapshot = manifest.snapshot.copy(dbVersion = 3))))
    }

    @Test
    fun no_diary_content_fields_exist() {
        val text = ManifestCodec.encode(manifest)
        for (key in listOf("body", "plainText", "place", "mood", "path", "pin")) assertFalse("\"$key" in text, key)
    }

    /** 스펙 008의 목록 설정 칸은 있어도 그만. 옛 백업은 null, 새 백업은 그대로 읽힘 */
    @Test
    fun list_preview_settings_are_optional() {
        val old =
            assertNotNull(
                ManifestCodec.decode("""{"format":"org.sjhstudio.diary.backup","formatVersion":2,"settings":{"listStyle":"card"}}""")
            )
        assertNull(old.settings.previewLength)
        assertNull(old.settings.showListFormatting)
        val withList = manifest.copy(settings = manifest.settings.copy(previewLength = "full", showListFormatting = true))
        assertEquals(withList, ManifestCodec.decode(ManifestCodec.encode(withList)))
        // 형식 버전은 그대로라 옛 앱도 읽음
        assertEquals(ManifestCheck.OK, ManifestCodec.check(withList))
    }

    /**
     * 새 기분이 든 백업만 형식 4로 올려 옛 앱(2.2.x는 2까지, 다음 판은 3까지)이 막게 함
     * 새 기분이 없으면 지금 번호라 옛 앱에서도 복원됨
     */
    @Test
    fun format_version_follows_new_moods() {
        assertEquals(3, Manifest.driveFormat(newMoods = 0))
        assertEquals(4, Manifest.driveFormat(newMoods = 1))
        assertEquals(2, Manifest.bundleFormat(newMoods = 0))
        assertEquals(4, Manifest.bundleFormat(newMoods = 3))
    }
}
