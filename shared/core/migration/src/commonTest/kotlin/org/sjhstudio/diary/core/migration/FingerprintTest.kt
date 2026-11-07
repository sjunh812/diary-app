package org.sjhstudio.diary.core.migration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.sjhstudio.diary.core.model.Media
import org.sjhstudio.diary.core.model.MediaStatus
import org.sjhstudio.diary.core.model.Mood

// 같은 일기는 원본 지문과 새 DB 지문이 같고, 하나만 달라도 다르다(스펙 001 T035).
class FingerprintTest {
    private val mapper = LegacyEntryMapper(LocalDate(2026, 11, 1), TimeZone.of("Asia/Seoul")) { "id-1" }
    private val note =
        LegacyNote(
            id = 3,
            createDate = "2024-03-01 23:59:457",
            modifyDate = "2024-03-02 08:00:00",
            contents = "첫 줄\n\n둘째 \"문단\" & <끝>😀",
            mood = 7,
            weather = 4,
            address = " 서울 ",
            star = null,
            locationX = "37.5",
            locationY = "127.0",
            picture = "/a.jpg,/b.jpg,"
        )
    private val photos = listOf(Fingerprint.LegacyPhoto("/a.jpg", "aaa"), Fingerprint.LegacyPhoto("/b.jpg", null))

    private fun restoredMedia() = listOf(
        Media("m1", "id-1", 0, MediaStatus.NORMAL, "aaa.jpg", "aaa", 10, 10, 1, "/a.jpg", Instant.fromEpochMilliseconds(0)),
        Media("m2", "id-1", 1, MediaStatus.MISSING, null, null, null, null, null, "/b.jpg", Instant.fromEpochMilliseconds(0))
    )

    @Test
    fun legacy_and_restored_fingerprints_match() {
        val mapped = mapper.map(note)
        assertEquals(Fingerprint.legacyCanonical(note, mapped, photos), Fingerprint.restoredCanonical(mapped.entry, restoredMedia()))
    }

    @Test
    fun any_single_difference_changes_the_fingerprint() {
        val mapped = mapper.map(note)
        val expected = Fingerprint.legacyCanonical(note, mapped, photos)
        val entry = mapped.entry
        val variants =
            listOf(
                Fingerprint.restoredCanonical(entry.copy(plainText = entry.plainText + "!"), restoredMedia()),
                Fingerprint.restoredCanonical(entry.copy(bodyHtml = entry.bodyHtml.replace("첫", "첮")), restoredMedia()),
                Fingerprint.restoredCanonical(entry.copy(mood = Mood.BAD), restoredMedia()),
                Fingerprint.restoredCanonical(entry, restoredMedia().reversed().mapIndexed { i, m -> m.copy(sortOrder = i) }),
                Fingerprint.restoredCanonical(entry, restoredMedia().map { if (it.id == "m1") it.copy(sha256 = "bbb") else it }),
                Fingerprint.restoredCanonical(entry.copy(isFavorite = true), restoredMedia())
            )
        variants.forEach { assertNotEquals(expected, it) }
    }

    @Test
    fun fingerprint_is_lowercase_sha256_hex() {
        val value = Fingerprint.restoredCanonical(mapper.map(note).entry, restoredMedia())
        assertEquals(64, value.length)
        assertEquals(value.lowercase(), value)
    }
}
