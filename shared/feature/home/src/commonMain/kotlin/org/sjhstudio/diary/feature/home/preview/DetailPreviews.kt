package org.sjhstudio.diary.feature.home.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.Entry
import org.sjhstudio.diary.core.model.EntryDetail
import org.sjhstudio.diary.core.model.EntryPhoto
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.feature.home.Body

private val Written = Instant.parse("2026-10-31T12:40:00Z")

private val Sample = Entry(
    id = "a",
    date = LocalDate(2026, 10, 31),
    time = LocalTime(21, 40),
    bodyHtml = "<p>퇴근길에 한강까지 걸었다.</p>",
    plainText = "퇴근길에 한강까지 걸었다.",
    mood = Mood.entries.first(),
    weather = null,
    placeName = "망원동",
    isFavorite = false,
    createdAt = Written,
    updatedAt = Written
)

@Composable
private fun Detail(dark: Boolean, detail: EntryDetail) = DiaryTheme(darkTheme = dark) {
    Body(detail, DiaryTheme.typography.body, onPhoto = {})
}

@Preview(locale = "ko")
@Composable
private fun KeptBothLight() = Detail(false, EntryDetail(Sample, emptyList(), keptBothWith = "b"))

@Preview(locale = "ko")
@Composable
private fun KeptBothDark() = Detail(true, EntryDetail(Sample, emptyList(), keptBothWith = "b"))

@Preview(locale = "ko")
@Composable
private fun PendingPhotoLight() = Detail(false, EntryDetail(Sample, listOf(EntryPhoto("m", null, null, null, pending = true))))
