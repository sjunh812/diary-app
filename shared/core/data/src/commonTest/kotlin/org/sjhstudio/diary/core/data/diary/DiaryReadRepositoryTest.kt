package org.sjhstudio.diary.core.data.diary

import androidx.paging.PagingSource
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import okio.fakefilesystem.FakeFileSystem
import org.sjhstudio.diary.core.data.photo.DefaultMediaRepository
import org.sjhstudio.diary.core.database.EntryEntity
import org.sjhstudio.diary.core.database.asModel
import org.sjhstudio.diary.core.model.DateRange
import org.sjhstudio.diary.core.model.MediaStatus
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.SearchFilter
import org.sjhstudio.diary.core.model.StatsPeriod
import org.sjhstudio.diary.core.testing.TestClock

// 달력, 찾기, 돌아보기, 휴지통, 사진 정리(스펙 002 T044 묶음).
class DiaryReadRepositoryTest {
    private val db = memoryDatabase()
    private val clock = TestClock(T0)
    private val entries = DefaultEntryRepository(db, MEDIA_DIR, clock)
    private val trash = DefaultTrashRepository(db, MEDIA_DIR, clock)
    private val stats = DefaultStatsRepository(db)

    @AfterTest
    fun tearDown() {
        db.close()
    }

    private suspend fun ids(filter: SearchFilter) = entries.search(filter).first().map { it.id }

    @Test
    fun timeline_is_newest_first_and_skips_trash() = runTest {
        db.seed(entry("old", date = "2026-09-01"), entry("new", date = "2026-10-02"), entry("gone", date = "2026-10-03", deletedAt = T0))

        val page = db.entryDao().visibleEntries().load(PagingSource.LoadParams.Refresh(null, 10, false))

        assertEquals(listOf("new", "old"), assertIs<PagingSource.LoadResult.Page<Int, *>>(page).data.map { (it as EntryEntity).id })
    }

    @Test
    fun favorites_only_keeps_starred_entries() = runTest {
        db.seed(entry("a", favorite = true), entry("b"))

        val page = db.entryDao().favoriteEntries().load(PagingSource.LoadParams.Refresh(null, 10, false))

        assertEquals(listOf("a"), assertIs<PagingSource.LoadResult.Page<Int, *>>(page).data.map { (it as EntryEntity).id })
    }

    @Test
    fun months_and_jump_position_follow_visible_entries() = runTest {
        db.seed(
            entry("a", date = "2026-10-20"),
            entry("b", date = "2026-10-01"),
            entry("c", date = "2026-09-15", favorite = true),
            entry("gone", date = "2026-08-01", deletedAt = T0)
        )

        assertEquals(setOf(YearMonth(2026, 10), YearMonth(2026, 9)), entries.months().first())
        assertEquals(0, entries.positionOf(YearMonth(2026, 10), favoritesOnly = false))
        assertEquals(2, entries.positionOf(YearMonth(2026, 9), favoritesOnly = false))
        assertEquals(0, entries.positionOf(YearMonth(2026, 9), favoritesOnly = true))
    }

    @Test
    fun favorite_changes_updated_time() = runTest {
        db.seed(entry("a"))
        clock.advance(1.hours)

        entries.setFavorite("a", true)

        val saved = db.entryDao().get("a")!!.asModel()
        assertTrue(saved.isFavorite)
        assertEquals(T0 + 1.hours, saved.updatedAt)
    }

    @Test
    fun day_lists_the_latest_entry_first() = runTest {
        db.seed(entry("a", time = "08:00"), entry("b", time = "21:00"), entry("c", date = "2026-10-30"))

        assertEquals(listOf("b", "a"), entries.day(LocalDate(2026, 10, 31)).first().map { it.id })
    }

    @Test
    fun calendar_uses_the_last_entry_of_the_day_and_counts_them() = runTest {
        db.seed(
            entry("a", time = "08:00", mood = Mood.SAD),
            entry("b", time = "21:00", mood = Mood.GOOD),
            entry("c", date = "2026-10-29", time = "09:00")
        )

        val month = entries.month(YearMonth(2026, 10)).first()

        assertEquals(Mood.GOOD, month[LocalDate(2026, 10, 31)]?.mood)
        assertEquals(2, month[LocalDate(2026, 10, 31)]?.count)
        assertNull(month[LocalDate(2026, 10, 29)]?.mood)
    }

    @Test
    fun same_time_uses_the_later_created_entry() = runTest {
        db.seed(entry("a", mood = Mood.SAD, createdAt = T0 + 1.hours), entry("b", mood = Mood.GOOD))

        assertEquals(Mood.SAD, entries.month(YearMonth(2026, 10)).first()[LocalDate(2026, 10, 31)]?.mood)
    }

    @Test
    fun trashed_entries_stay_out_of_everything_but_trash() = runTest {
        db.seed(entry("a", mood = Mood.GOOD))
        entries.moveToTrash("a")

        assertNull(entries.entry("a").first())
        assertTrue(entries.day(LocalDate(2026, 10, 31)).first().isEmpty())
        assertTrue(entries.month(YearMonth(2026, 10)).first().isEmpty())
        assertTrue(ids(SearchFilter(query = "글")).isEmpty())
        assertEquals(0, stats.moodCounts(StatsPeriod.All).first().total)
        assertEquals(listOf("a"), trash.items().first().map { it.row.id })
    }

    @Test
    fun restoring_brings_the_entry_back_and_marks_it_changed() = runTest {
        db.seed(entry("a"))
        entries.moveToTrash("a")
        clock.advance(1.hours)

        trash.restore("a")

        val saved = db.entryDao().get("a")!!.asModel()
        assertNull(saved.deletedAt)
        assertEquals(T0 + 1.hours, saved.updatedAt)
    }

    @Test
    fun search_finds_body_and_place() = runTest {
        db.seed(entry("a", text = "바다를 봤다"), entry("b", text = "산", placeName = "바다 앞 카페"), entry("c", text = "집"))

        assertEquals(setOf("a", "b"), ids(SearchFilter(query = "바다")).toSet())
    }

    @Test
    fun search_treats_wildcards_as_letters() = runTest {
        db.seed(entry("a", text = "100% 좋았다"), entry("b", text = "1000 좋았다"), entry("c", text = "a_b"), entry("d", text = "axb"))

        assertEquals(listOf("a"), ids(SearchFilter(query = "100%")))
        assertEquals(listOf("c"), ids(SearchFilter(query = "a_b")))
    }

    @Test
    fun search_combines_conditions() = runTest {
        db.seed(
            entry("a", date = "2026-10-01", mood = Mood.GOOD, favorite = true),
            entry("b", date = "2026-10-02", mood = Mood.SAD),
            entry("c", date = "2026-09-01", mood = Mood.GOOD)
        )
        db.seedPhotos(photo("p", "a", 0))
        val october = DateRange(LocalDate(2026, 10, 1), LocalDate(2026, 10, 31))

        assertEquals(listOf("a"), ids(SearchFilter(moods = setOf(Mood.GOOD), period = october)))
        assertEquals(listOf("a"), ids(SearchFilter(hasPhoto = true)))
        assertEquals(listOf("a"), ids(SearchFilter(favoritesOnly = true)))
    }

    @Test
    fun search_preview_keeps_a_late_match_visible() = runTest {
        val long = "가".repeat(300) + "바다" + "나".repeat(10)
        db.seed(entry("a", text = long))

        assertTrue(entries.search(SearchFilter(query = "바다")).first().single().preview.contains("바다"))
    }

    @Test
    fun missing_photos_show_as_null_paths() = runTest {
        db.seed(entry("a"))
        db.seedPhotos(photo("p", "a", 0), photo("gone", "a", 1, MediaStatus.MISSING))

        assertEquals(listOf("$MEDIA_DIR/p.jpg", null), entries.day(LocalDate(2026, 10, 31)).first().single().photos)
    }

    @Test
    fun stats_count_moods_and_no_mood_inside_the_period() = runTest {
        db.seed(
            entry("a", mood = Mood.GOOD),
            entry("b", mood = Mood.GOOD),
            entry("c", mood = Mood.SAD),
            entry("d"),
            entry("e", date = "2026-09-30", mood = Mood.GOOD)
        )

        val counts = stats.moodCounts(StatsPeriod.Month(YearMonth(2026, 10))).first()

        assertEquals(2, counts.counts[Mood.GOOD])
        assertEquals(1, counts.noMood)
        assertEquals(4, counts.total)
        assertEquals(setOf(Mood.GOOD), counts.top)
    }

    @Test
    fun year_pixels_map_each_day() = runTest {
        db.seed(entry("a", mood = Mood.GOOD), entry("b", date = "2025-01-01", mood = Mood.SAD))

        assertEquals(mapOf(LocalDate(2026, 10, 31) to Mood.GOOD), stats.yearPixels(2026).first())
    }

    @Test
    fun trash_counts_days_left_and_purges_after_thirty_days() = runTest {
        db.seed(entry("old", deletedAt = T0 - 31.days), entry("new", deletedAt = T0 - 29.days + 1.hours))
        db.seed(entry("keep"))

        assertEquals(listOf(2, 1), trash.items().first().map { it.daysLeft })
        trash.purgeExpired(T0)

        assertNull(db.entryDao().get("old"))
        assertTrue(db.entryDao().get("new") != null)
        assertTrue(db.entryDao().get("keep") != null)
    }

    @Test
    fun days_left_starts_at_thirty() {
        assertEquals(30, daysLeft(T0, T0))
        assertEquals(1, daysLeft(T0, T0 + 30.days - 1.hours))
    }

    @Test
    fun delete_forever_touches_only_trashed_entries() = runTest {
        db.seed(entry("a", deletedAt = T0), entry("b"))

        trash.deleteForever(listOf("a", "b"))

        assertNull(db.entryDao().get("a"))
        assertTrue(db.entryDao().get("b") != null)
    }

    @Test
    fun empty_removes_all_trash_and_nothing_else() = runTest {
        db.seed(entry("a", deletedAt = T0), entry("b", deletedAt = T0), entry("c"))

        trash.empty()

        assertEquals(1, db.entryDao().countAll())
    }

    @Test
    fun cleaning_keeps_used_recent_and_trashed_photos() = runTest {
        val files = FakeFileSystem().apply { createDirectories(MEDIA_DIR) }
        db.seed(entry("trashed", deletedAt = T0))
        db.seedPhotos(photo("kept", "trashed", 0))
        for (name in listOf("kept.jpg", "stale.jpg")) files.write(MEDIA_DIR / name) { writeUtf8("x") }
        val written = Instant.fromEpochMilliseconds(files.metadata(MEDIA_DIR / "stale.jpg").lastModifiedAtMillis!!)
        val cleaner = DefaultMediaRepository(db, files, MEDIA_DIR, THUMB_DIR)

        // 방금 고른 사진은 아직 잇기 전일 수 있어 남긴다.
        cleaner.cleanUnused(written + 1.hours)
        assertEquals(setOf("kept.jpg", "stale.jpg"), files.list(MEDIA_DIR).map { it.name }.toSet())

        cleaner.cleanUnused(written + 2.days)
        assertEquals(setOf("kept.jpg"), files.list(MEDIA_DIR).map { it.name }.toSet())
    }
}
