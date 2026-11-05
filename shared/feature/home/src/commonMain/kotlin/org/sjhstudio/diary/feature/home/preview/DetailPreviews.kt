package org.sjhstudio.diary.feature.home.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import org.sjhstudio.diary.feature.home.EntryMissing

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

@Preview(name = "없는 일기 · 라이트", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun MissingLight() = Missing(dark = false)

@Preview(name = "없는 일기 · 다크", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun MissingDark() = Missing(dark = true)

@Composable
private fun Missing(dark: Boolean) = DiaryTheme(darkTheme = dark) {
    Box(Modifier.fillMaxSize().background(DiaryTheme.colors.canvas)) { EntryMissing() }
}
