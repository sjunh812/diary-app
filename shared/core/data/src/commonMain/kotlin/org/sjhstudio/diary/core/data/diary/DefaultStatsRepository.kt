package org.sjhstudio.diary.core.data.diary

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.model.AdvancedStats
import org.sjhstudio.diary.core.model.DayMood
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.MoodCounts
import org.sjhstudio.diary.core.model.StatsEntry
import org.sjhstudio.diary.core.model.StatsPeriod
import org.sjhstudio.diary.core.model.Weather
import org.sjhstudio.diary.core.model.advancedStats

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

    override fun advanced(period: StatsPeriod): Flow<AdvancedStats> {
        val range = period.range
        return entries.statsRows(range?.start?.toString(), range?.end?.toString()).map { rows ->
            advancedStats(
                rows.map { row ->
                    StatsEntry(
                        date = LocalDate.parse(row.date),
                        hour = row.time.substringBefore(':').toIntOrNull() ?: 0,
                        mood = Mood.fromId(row.mood),
                        weather = Weather.entries.firstOrNull { it.id == row.weather },
                        place = row.placeName
                    )
                }
            )
        }
    }

    override fun moodFlow(period: StatsPeriod): Flow<List<DayMood>> {
        val range = period.range
        return entries.dayMoods(range?.start?.toString() ?: FIRST_DAY, range?.end?.toString() ?: LAST_DAY).map { rows ->
            toDayMoods(rows).values.sortedBy { it.date }
        }
    }

    override fun yearPixels(year: Int): Flow<Map<LocalDate, Mood?>> =
        entries.dayMoods(LocalDate(year, 1, 1).toString(), LocalDate(year, 12, 31).toString()).map { rows ->
            toDayMoods(rows).mapValues { it.value.mood }
        }

    private companion object {
        const val FIRST_DAY = "0000-01-01"
        const val LAST_DAY = "9999-12-31"
    }
}
