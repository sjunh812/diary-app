package org.sjhstudio.diary.core.data.diary

import androidx.room3.Room
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import org.sjhstudio.diary.core.data.photo.PhotoStore
import org.sjhstudio.diary.core.data.photo.ThumbnailEncoder
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.database.DiaryDatabaseConstructor
import org.sjhstudio.diary.core.database.asEntity
import org.sjhstudio.diary.core.database.configured
import org.sjhstudio.diary.core.model.Draft
import org.sjhstudio.diary.core.model.DraftKey
import org.sjhstudio.diary.core.model.Entry
import org.sjhstudio.diary.core.model.Media
import org.sjhstudio.diary.core.model.MediaStatus
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.platform.ImageSize

internal val MEDIA_DIR: Path = "/files/media".toPath()
internal val THUMB_DIR: Path = "/files/thumbs".toPath()
internal val T0: Instant = Instant.parse("2026-11-01T00:00:00Z")

internal fun memoryDatabase(): DiaryDatabase = Room.inMemoryDatabaseBuilder(DiaryDatabaseConstructor::initialize).configured().build()

internal fun entry(
    id: String,
    date: String = "2026-10-31",
    time: String = "21:40",
    text: String = "글 $id",
    mood: Mood? = null,
    deletedAt: Instant? = null,
    favorite: Boolean = false,
    createdAt: Instant = T0,
    placeName: String? = null
) = Entry(
    id = id,
    date = LocalDate.parse(date),
    time = LocalTime.parse(time),
    bodyHtml = "<p>$text</p>",
    plainText = text,
    mood = mood,
    weather = null,
    placeName = placeName,
    isFavorite = favorite,
    createdAt = createdAt,
    updatedAt = createdAt,
    deletedAt = deletedAt
)

internal fun photo(id: String, entryId: String, order: Int, status: MediaStatus = MediaStatus.NORMAL, legacyPath: String? = null) = Media(
    id = id,
    entryId = entryId,
    sortOrder = order,
    status = status,
    fileName = if (status == MediaStatus.NORMAL) "$id.jpg" else null,
    sha256 = if (status == MediaStatus.NORMAL) id else null,
    width = 100,
    height = 100,
    byteSize = 10,
    legacyPath = legacyPath,
    createdAt = T0
)

internal suspend fun DiaryDatabase.seed(vararg entries: Entry) = entryDao().insertAll(entries.map { it.asEntity() })

internal suspend fun DiaryDatabase.seedPhotos(vararg media: Media) = mediaDao().insertAll(media.map { it.asEntity() })

internal fun newDraft(body: String = "<p>오늘</p>", mood: Mood? = null) = Draft(
    key = DraftKey.New,
    date = LocalDate(2026, 11, 1),
    time = LocalTime(21, 0),
    bodyHtml = body,
    mood = mood,
    weather = null,
    placeName = null,
    isFavorite = false,
    weatherAutoFilled = false,
    updatedAt = T0
)

/** 그림 대신 받은 바이트를 그대로 복사한다. 빈 바이트는 읽을 수 없는 그림으로 본다. */
internal fun copyEncoder(fileSystem: FileSystem) = ThumbnailEncoder { source, target, _, _ ->
    val bytes = fileSystem.read(source.toPath()) { readByteArray() }
    if (bytes.isEmpty()) {
        false
    } else {
        fileSystem.write(target.toPath()) { write(bytes) }
        true
    }
}

internal fun photoStore(fileSystem: FakeFileSystem = FakeFileSystem()) =
    PhotoStore(fileSystem, MEDIA_DIR, copyEncoder(fileSystem), { ImageSize(10, 20) })
