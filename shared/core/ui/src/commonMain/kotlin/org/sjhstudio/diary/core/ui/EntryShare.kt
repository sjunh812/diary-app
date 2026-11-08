package org.sjhstudio.diary.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.model.Entry
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.Weather
import org.sjhstudio.diary.core.ui.resources.Res
import org.sjhstudio.diary.core.ui.resources.share_when

/**
 * 공유할 일기 글.
 * 첫 줄은 날짜와 시각, 둘째 줄은 기분·날씨·장소, 빈 줄 뒤에 본문이다.
 * 없는 값은 빼고, 셋 다 없으면 둘째 줄을 두지 않는다.
 * 사진은 넣지 않는다. 사용자가 공유를 눌러 직접 보내는 글이다.
 */
@Composable
fun rememberEntryShareText(): (Entry) -> String {
    val dates = rememberDiaryDateFormat()
    val moods = Mood.entries.associateWith { moodName(it) }
    val weathers = Weather.entries.associateWith { weatherName(it) }
    val whenTemplate = stringResource(Res.string.share_when)
    return remember(dates, moods, weathers, whenTemplate) {
        { entry -> entryShareText(entry, dates, moods, weathers, whenTemplate) }
    }
}

internal fun entryShareText(
    entry: Entry,
    dates: DiaryDateFormat,
    moods: Map<Mood, String>,
    weathers: Map<Weather, String>,
    whenTemplate: String
): String {
    val heading =
        whenTemplate
            .replace("%1\$s", dates.fullDate(entry.date))
            .replace("%2\$s", dates.weekday(entry.date))
            .replace("%3\$s", dates.time(entry.time))
    val details = listOfNotNull(entry.mood?.let(moods::get), entry.weather?.let(weathers::get), entry.placeName?.takeIf { it.isNotBlank() })
    return buildString {
        append(heading)
        if (details.isNotEmpty()) append('\n').append(details.joinToString(" · "))
        if (entry.plainText.isNotBlank()) append("\n\n").append(entry.plainText.trim())
    }
}
