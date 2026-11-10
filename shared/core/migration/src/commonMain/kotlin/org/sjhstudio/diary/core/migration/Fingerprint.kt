package org.sjhstudio.diary.core.migration

import okio.ByteString.Companion.encodeUtf8
import org.sjhstudio.diary.core.model.Entry
import org.sjhstudio.diary.core.model.Media
import org.sjhstudio.diary.core.model.MediaStatus
import org.sjhstudio.diary.core.model.richtext.toPlainText

/**
 * 검증 지문(contracts/legacy-mapping.md "검증 지문").
 * 원본 줄로 만든 지문과 새 DB에서 다시 읽은 값으로 만든 지문이 줄마다 같아야 이전이 끝남.
 * 필드는 U+001F, 사진 항목은 U+001E로 잇고 UTF-8 바이트의 SHA-256을 소문자 hex로 씀.
 */
object Fingerprint {
    private const val US = '\u001F'
    private const val RS = '\u001E'
    private const val MISSING = "missing"

    /** 원본 사진 조각 하나와 찾은 파일의 해시(못 찾았으면 null) */
    data class LegacyPhoto(val fragment: String, val sha256: String?)

    fun legacyCanonical(note: LegacyNote, mapped: MappedEntry, photos: List<LegacyPhoto>): String {
        val e = mapped.entry
        val fields =
            listOf(
                "v1",
                note.id.toString(),
                note.createDate.orEmpty(),
                e.date.toString(),
                timeText(e),
                note.contents.orEmpty(),
                note.contents.orEmpty(),
                (note.mood?.takeIf { it in 0L..8L } ?: -1L).toString(),
                (note.weather?.takeIf { it in 0L..6L } ?: -1L).toString(),
                LegacyEntryMapper.placeNameOf(note.address).orEmpty(),
                if (note.star == 1L) "1" else "0",
                LegacyEntryMapper.legacyLocationOf(note.locationX, note.locationY).orEmpty(),
                photos.joinToString(RS.toString()) { "${it.fragment}=${it.sha256 ?: MISSING}" }
            )
        return sha256(fields.joinToString(US.toString()))
    }

    fun restoredCanonical(entry: Entry, media: List<Media>): String {
        val fields =
            listOf(
                "v1",
                entry.legacyId?.toString().orEmpty(),
                entry.legacyCreateDate.orEmpty(),
                entry.date.toString(),
                timeText(entry),
                toPlainText(entry.bodyHtml),
                entry.plainText,
                (entry.mood?.legacyCode ?: -1).toString(),
                (entry.weather?.legacyCode ?: -1).toString(),
                entry.placeName.orEmpty(),
                if (entry.isFavorite) "1" else "0",
                entry.legacyLocation.orEmpty(),
                media.sortedBy { it.sortOrder }.joinToString(RS.toString()) {
                    "${it.legacyPath.orEmpty()}=${if (it.status == MediaStatus.NORMAL) it.sha256 else MISSING}"
                }
            )
        return sha256(fields.joinToString(US.toString()))
    }

    private fun timeText(entry: Entry): String =
        entry.time.hour.toString().padStart(2, '0') + ":" + entry.time.minute.toString().padStart(2, '0')

    private fun sha256(text: String): String = text.encodeUtf8().sha256().hex()
}
