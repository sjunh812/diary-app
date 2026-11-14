package org.sjhstudio.diary.core.backup

import androidx.sqlite.SQLiteStatement
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import okio.Path
import org.sjhstudio.diary.core.database.EntryEntity
import org.sjhstudio.diary.core.database.MediaEntity

/**
 * 받은 스냅샷을 읽기 전용으로 열어 일기와 사진 줄을 읽음
 * 버전이 달라도 읽는 열은 여기 한곳에서 맞춤(FR-035)
 */
object SnapshotReader {
    fun read(path: Path): List<MergeEntry> {
        val connection = BundledSQLiteDriver().open(path.toString(), READ_ONLY)
        try {
            val media =
                connection.prepare(
                    MEDIA_COLUMNS
                ).use { statement ->
                    buildList { while (statement.step()) add(statement.media()) }
                }.groupBy { it.entryId }
            return connection.prepare(
                "SELECT id, date, time, body_html, plain_text, mood, weather, place_name, is_favorite, " +
                    "created_at, updated_at, deleted_at, " +
                    "legacy_id, legacy_create_date, legacy_location FROM entry"
            ).use { statement ->
                buildList {
                    while (statement.step()) {
                        val entry = statement.entry()
                        add(MergeEntry(entry, media[entry.id].orEmpty()))
                    }
                }
            }
        } finally {
            connection.close()
        }
    }

    private fun SQLiteStatement.text(index: Int) = if (isNull(index)) null else getText(index)

    private fun SQLiteStatement.int(index: Int) = if (isNull(index)) null else getInt(index)

    private fun SQLiteStatement.long(index: Int) = if (isNull(index)) null else getLong(index)

    private fun SQLiteStatement.media() = MediaEntity(
        getText(0), getText(1), getInt(2), getText(3), text(4), text(5), int(6), int(7), long(8), text(9), getLong(10)
    )

    private fun SQLiteStatement.entry() = EntryEntity(
        id = getText(0),
        date = getText(1),
        time = getText(2),
        bodyHtml = getText(3),
        plainText = getText(4),
        mood = text(5),
        weather = text(6),
        placeName = text(7),
        isFavorite = getInt(8) != 0,
        createdAt = getLong(9),
        updatedAt = getLong(10),
        deletedAt = long(11),
        legacyId = int(12),
        legacyCreateDate = text(13),
        legacyLocation = text(14)
    )

    private const val MEDIA_COLUMNS =
        "SELECT id, entry_id, sort_order, status, file_name, sha256, width, height, byte_size, legacy_path, created_at FROM media"

    // SQLITE_OPEN_READONLY
    private const val READ_ONLY = 0x00000001
}
