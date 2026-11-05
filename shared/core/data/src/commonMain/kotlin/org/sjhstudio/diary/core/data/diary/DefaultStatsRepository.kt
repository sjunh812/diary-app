package org.sjhstudio.diary.core.data.diary

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.database.StatsRow
import org.sjhstudio.diary.core.model.AdvancedPeriod
import org.sjhstudio.diary.core.model.AdvancedStats
import org.sjhstudio.diary.core.model.DayMood
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.MoodCounts
import org.sjhstudio.diary.core.model.StatsEntry
import org.sjhstudio.diary.core.model.StatsPeriod
import org.sjhstudio.diary.core.model.Weather
import org.sjhstudio.diary.core.model.advancedStats
import org.sjhstudio.diary.core.model.mostFrequent

class DefaultStatsRepository(private val db: DiaryDatabase) : StatsRepository {
    private val entries get() = db.entryDao()

    override fun moodCounts(period: StatsPeriod): Flow<MoodCounts> {
        val range = period.range
        return entries.moodCounts(range?.start?.toString(), range?.end?.toString()).map { rows ->
            val counts = Mood.entries.associateWith { 0 }.toMutableMap()
            var noMood = 0
            for (row in rows) {
                val mood = Mood.fromId(row.mood)
                if (mood == null) noMood += row.count else counts[mood] = row.count
            }
            MoodCounts(counts, noMood)
        }
    }

    override fun advanced(period: AdvancedPeriod, today: LocalDate): Flow<AdvancedStats> {
        val range = period.range(today)
        val before = period.compare(today)
        return entries.statsRows((before?.start ?: range?.start)?.toString(), range?.end?.toString()).map { rows ->
            val all = rows.map { it.toEntry() }
            if (before == null) {
                advancedStats(all)
            } else {
                advancedStats(all.filter { it.date >= range!!.start }, all.filter { it.date <= before.end })
            }
        }.flowOn(Dispatchers.Default)
    }

    private fun StatsRow.toEntry() = StatsEntry(
        date = LocalDate.parse(date),
        hour = time.substringBefore(':').toIntOrNull() ?: 0,
        mood = Mood.fromId(mood),
        weather = Weather.fromId(weather),
        place = placeName,
        photos = photos,
        length = length
    )

    override fun moodFlow(period: StatsPeriod): Flow<List<DayMood>> {
        val range = period.range
        return entries.dayMoods(range?.start?.toString() ?: FIRST_DAY, range?.end?.toString() ?: LAST_DAY).map { rows ->
            toDayMoods(rows).values.sortedBy { it.date }
        }.flowOn(Dispatchers.Default)
    }

    override fun yearPixels(year: Int): Flow<Map<LocalDate, Mood?>> =
        entries.dayMoods(LocalDate(year, 1, 1).toString(), LocalDate(year, 12, 31).toString()).map { rows ->
            toDayMoods(rows).mapValues { it.value.moods.mostFrequent() }
        }.flowOn(Dispatchers.Default)

    private companion object {
        const val FIRST_DAY = "0000-01-01"
        const val LAST_DAY = "9999-12-31"
    }
}
