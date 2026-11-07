package org.sjhstudio.diary.feature.trash.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.EntryRow
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.TrashedEntry
import org.sjhstudio.diary.core.model.Weather
import org.sjhstudio.diary.feature.trash.TrashScreen

private val Items =
    listOf(
        TrashedEntry(
            EntryRow(
                "1",
                LocalDate(
                    2026,
                    10,
                    31
                ),
                LocalTime(8, 10), "아침 회의가 길었다.", Mood.TIRED, Weather.CLOUDY, "회사 근처", false, emptyList()
            ),
            Instant.fromEpochSeconds(0),
            29
        ),
        TrashedEntry(
            EntryRow("2", LocalDate(2026, 10, 2), LocalTime(22, 30), "별일 없던 하루.", Mood.MEH, null, null, false, emptyList()),
            Instant.fromEpochSeconds(0),
            1
        )
    )

@Preview(name = "휴지통 · 라이트", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun TrashLight() = DiaryTheme(darkTheme = false) { TrashScreen(Items, {}, {}, {}) }

@Preview(name = "휴지통 · 다크", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun TrashDark() = DiaryTheme(darkTheme = true) { TrashScreen(Items, {}, {}, {}) }

@Preview(name = "휴지통 · 빈 상태", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun TrashEmpty() = DiaryTheme(darkTheme = false) { TrashScreen(emptyList(), {}, {}, {}) }
