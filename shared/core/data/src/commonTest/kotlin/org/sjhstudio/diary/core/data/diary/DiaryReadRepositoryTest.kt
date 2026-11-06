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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import okio.fakefilesystem.FakeFileSystem
import org.sjhstudio.diary.core.data.photo.DefaultMediaRepository
import org.sjhstudio.diary.core.database.EntryListRow
import org.sjhstudio.diary.core.database.MergeMarkEntity
import org.sjhstudio.diary.core.database.asModel
import org.sjhstudio.diary.core.model.Checklist
import org.sjhstudio.diary.core.model.DateRange
import org.sjhstudio.diary.core.model.EntryRow
import org.sjhstudio.diary.core.model.MediaStatus
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.SearchFilter
import org.sjhstudio.diary.core.model.StatsPeriod
import org.sjhstudio.diary.core.testing.TestClock

/** 찾은 줄을 쪽 나눔 없이 모두 읽는다. */
internal suspend fun DefaultEntryRepository.searchAll(filter: SearchFilter): List<EntryRow> =
    (searchPages(filter).load(PagingSource.LoadParams.Refresh(null, ALL_ROWS, false)) as PagingSource.LoadResult.Page).data

private const val ALL_ROWS = 100_000
private const val ROUNDS = 30

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

    private suspend fun ids(filter: SearchFilter) = entries.searchAll(filter).map { it.id }

    @Test
    fun timeline_is_newest_first_and_skips_trash() = runTest {
        db.seed(entry("old", date = "2026-09-01"), entry("new", date = "2026-10-02"), entry("gone", date = "2026-10-03", deletedAt = T0))

        val page = db.entryDao().visibleEntries().load(PagingSource.LoadParams.Refresh(null, 10, false))

        assertEquals(listOf("new", "old"), assertIs<PagingSource.LoadResult.Page<Int, *>>(page).data.map { (it as EntryListRow).id })
    }

    @Test
    fun checking_an_item_updates_text_and_edit_time() = runTest {
        db.seed(entry("a"))
        val stored = db.entryDao().get("a")!!
        db.entryDao().update(stored.copy(bodyHtml = "<p>${Checklist.OPEN} 우유</p>", plainText = "${Checklist.OPEN} 우유"))
        clock.advance(1.hours)

        entries.toggleCheck("a", 0)

        val after = db.entryDao().get("a")!!
        assertEquals("<p>${Checklist.DONE} 우유</p>", after.bodyHtml)
        assertEquals("${Checklist.DONE} 우유", after.plainText)
        assertTrue(after.updatedAt > stored.updatedAt)
    }

    /** 읽고 고쳐 쓰는 사이에 다른 체크가 끼어들면 앞의 체크가 사라진다. */
    @Test
    fun checking_two_items_at_once_keeps_both() = runTest {
        db.seed(entry("a"))
        val open = "<p>${Checklist.OPEN} 우유</p><p>${Checklist.OPEN} 빵</p>"
        repeat(ROUNDS) {
            val stored = db.entryDao().get("a")!!
            db.entryDao().update(stored.copy(bodyHtml = open))

            withContext(Dispatchers.Default) { listOf(0, 1).map { async { entries.toggleCheck("a", it) } }.awaitAll() }

            assertEquals("<p>${Checklist.DONE} 우유</p><p>${Checklist.DONE} 빵</p>", db.entryDao().get("a")!!.bodyHtml)
        }
    }

    @Test
    fun timeline_page_reads_photos_once_for_the_whole_page() = runTest {
        db.seed(entry("a", date = "2026-09-01"), entry("b", date = "2026-10-02"))
        val source = db.entryDao().visibleEntries()
        var calls = 0
        val rows =
            EntryRowPagingSource(source) { page ->
                calls++
                page.map { it.toRow(emptyList(), MEDIA_DIR) }
            }

        val page = rows.load(PagingSource.LoadParams.Refresh(null, 10, false))

        assertEquals(listOf("b", "a"), assertIs<PagingSource.LoadResult.Page<Int, *>>(page).data.map { (it as EntryRow).id })
        assertEquals(1, calls)
        source.invalidate()
        assertTrue(rows.invalid)
    }

    private suspend fun keepBoth(a: String, b: String) {
        db.seed(entry(a), entry(b))
        db.backupDao().mark(listOf(MergeMarkEntity(a, "kept_both", b, "drive", 1), MergeMarkEntity(b, "kept_both", a, "drive", 1)))
    }

    @Test
    fun kept_both_entry_points_at_its_pair() = runTest {
        keepBoth("a", "b")

        assertEquals("b", entries.entry("a").first()?.keptBothWith)
        assertEquals(true, entries.day(LocalDate.parse("2026-10-31")).first().all { it.keptBoth })
    }

    @Test
    fun closing_the_mark_clears_both_sides() = runTest {
        keepBoth("a", "b")

        entries.clearKeptBoth("a")

        assertEquals(null, entries.entry("b").first()?.keptBothWith)
        assertEquals(2, entries.day(LocalDate.parse("2026-10-31")).first().size)
    }

    @Test
    fun trashing_one_side_clears_both_marks() = runTest {
        keepBoth("a", "b")

        entries.moveToTrash("b")

        assertEquals(null, entries.entry("a").first()?.keptBothWith)
    }

    @Test
    fun favorites_only_keeps_starred_entries() = runTest {
        db.seed(entry("a", favorite = true), entry("b"))

        val page = db.entryDao().favoriteEntries().load(PagingSource.LoadParams.Refresh(null, 10, false))

        assertEquals(listOf("a"), assertIs<PagingSource.LoadResult.Page<Int, *>>(page).data.map { (it as EntryListRow).id })
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
    fun recent_places_are_unique_and_newest_first() = runTest {
        db.seed(
            entry("a", date = "2026-10-01", placeName = "망원동"),
            entry("b", date = "2026-10-20", placeName = "연남동"),
            entry("c", date = "2026-10-05", placeName = "망원동"),
            entry("d", date = "2026-10-30", placeName = "제주", deletedAt = T0),
            entry("e", date = "2026-10-31")
        )

        assertEquals(listOf("연남동", "망원동"), db.entryDao().recentPlaces(6))
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
        assertEquals(Mood.SAD, month[LocalDate(2026, 10, 31)]?.previousMood)
        assertNull(month[LocalDate(2026, 10, 29)]?.mood)
        assertNull(month[LocalDate(2026, 10, 29)]?.previousMood)
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

        assertTrue(entries.searchAll(SearchFilter(query = "바다")).single().preview.contains("바다"))
    }

    @Test
    fun list_preview_matches_the_whole_text() = runTest {
        // 잘라 읽어도 미리보기는 같아야 한다.
        // 이모지가 끊기는 자리도 본다.
        val long = "🌊".repeat(100) + "오늘은\n\n바다에 갔다 ".repeat(200)
        db.seed(entry("a", text = long))

        val page = db.entryDao().visibleEntries().load(PagingSource.LoadParams.Refresh(null, 10, false))
        val row = assertIs<PagingSource.LoadResult.Page<Int, EntryListRow>>(page).data.single()

        assertTrue(row.plainText.length < long.length)
        assertEquals(preview(long, ""), preview(row.plainText, ""))
        assertEquals(db.entryDao().get("a")!!.toRow(emptyList(), MEDIA_DIR), row.toRow(emptyList(), MEDIA_DIR))
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
    fun mood_flow_keeps_every_mood_of_the_day() = runTest {
        // 마지막 일기에 기분이 없어도 그날의 다른 기분은 통계가 세어야 한다.
        db.seed(entry("a", time = "09:00", mood = Mood.SICK), entry("b", time = "10:00", mood = Mood.MEH), entry("c", time = "11:00"))

        val day = stats.moodFlow(StatsPeriod.Month(YearMonth(2026, 10))).first().single()

        assertEquals(null, day.mood)
        assertEquals(listOf(Mood.SICK, Mood.MEH), day.moods)
        assertEquals(mapOf(LocalDate(2026, 10, 31) to Mood.MEH), stats.yearPixels(2026).first())
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
