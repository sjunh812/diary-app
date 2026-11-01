package org.sjhstudio.diary.core.database

import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format.char
import org.sjhstudio.diary.core.model.Entry
import org.sjhstudio.diary.core.model.Media
import org.sjhstudio.diary.core.model.MediaStatus
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.Weather

// 표의 글자·숫자 값과 모델 사이를 오간다.
// 저장 형식(data-model.md)을 여기 한곳에서만 다룬다.

/** `HH:mm`. 초는 쓰지 않는다. */
private val TIME_FORMAT =
    LocalTime.Format {
        hour()
        char(':')
        minute()
    }

fun EntryEntity.asModel(): Entry = Entry(
    id = id,
    date = LocalDate.parse(date),
    time = LocalTime.parse(time, TIME_FORMAT),
    bodyHtml = bodyHtml,
    plainText = plainText,
    mood = Mood.fromId(mood),
    weather = Weather.fromId(weather),
    placeName = placeName,
    isFavorite = isFavorite,
    createdAt = Instant.fromEpochMilliseconds(createdAt),
    updatedAt = Instant.fromEpochMilliseconds(updatedAt),
    deletedAt = deletedAt?.let(Instant::fromEpochMilliseconds),
    legacyId = legacyId,
    legacyCreateDate = legacyCreateDate,
    legacyLocation = legacyLocation
)

fun Entry.asEntity(): EntryEntity = EntryEntity(
    id = id,
    date = date.toString(),
    time = TIME_FORMAT.format(time),
    bodyHtml = bodyHtml,
    plainText = plainText,
    mood = mood?.id,
    weather = weather?.id,
    placeName = placeName?.takeIf { it.isNotEmpty() },
    isFavorite = isFavorite,
    createdAt = createdAt.toEpochMilliseconds(),
    updatedAt = updatedAt.toEpochMilliseconds(),
    deletedAt = deletedAt?.toEpochMilliseconds(),
    legacyId = legacyId,
    legacyCreateDate = legacyCreateDate,
    legacyLocation = legacyLocation
)

fun MediaEntity.asModel(): Media = Media(
    id = id,
    entryId = entryId,
    sortOrder = sortOrder,
    // 모르는 상태는 화면에서 "원래 기기에 있는 사진"으로 보이게 한다. 줄은 그대로 둔다.
    status = MediaStatus.fromId(status) ?: MediaStatus.MISSING,
    fileName = fileName,
    sha256 = sha256,
    width = width,
    height = height,
    byteSize = byteSize,
    legacyPath = legacyPath,
    createdAt = Instant.fromEpochMilliseconds(createdAt)
)

fun Media.asEntity(): MediaEntity = MediaEntity(
    id = id,
    entryId = entryId,
    sortOrder = sortOrder,
    status = status.id,
    fileName = fileName,
    sha256 = sha256,
    width = width,
    height = height,
    byteSize = byteSize,
    legacyPath = legacyPath,
    createdAt = createdAt.toEpochMilliseconds()
)
