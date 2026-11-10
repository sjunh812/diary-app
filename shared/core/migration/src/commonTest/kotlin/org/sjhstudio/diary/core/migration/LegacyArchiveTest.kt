package org.sjhstudio.diary.core.migration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.sjhstudio.diary.core.testing.TestClock

// 예전 일기 보기: 사본을 읽기 전용으로 일기 날짜 최근 순 60편씩(FR-009)
class LegacyArchiveTest {
    private fun paths(dir: TempDir): MigrationPaths {
        val legacy = LegacyFiles(
            dir.root / "databases" / "note.db",
            dir.root / "shared_prefs" / "pref.xml",
            dir.root / "shared_prefs" / "pref2.xml"
        )
        return MigrationPaths(legacy, dir.root / "snap", dir.root / "migration", dir.root / "media", dir.root / "databases" / "diary.db")
    }

    @Test
    fun pages_newest_first_and_finds_photos() = runTest {
        TempDir().use { dir ->
            val paths = paths(dir)
            val photo = dir.write("cache/a.jpg", byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()))
            val rows = LegacyFixtures.manyRows(130).toMutableList()
            rows[129] = rows[129].copy(picture = "$photo,${dir.root}/cache/gone.jpg,", star = 1, address = " 망원동 ")
            LegacyFixtures.create(paths.snapshotDir / "note.db", rows).close()
            val archive = SnapshotLegacyArchive(dir.fs, paths, TestClock(), TimeZone.of("Asia/Seoul"))

            val first = archive.page(after = null)
            val second = archive.page(after = first.last())
            val third = archive.page(after = second.last())

            // manyRows는 _id가 커질수록 날짜도 늦어 날짜 최근 순이 _id 역순과 같음
            assertEquals((130L downTo 71L).toList(), first.map { it.id })
            assertEquals(60, second.size)
            assertEquals(10, third.size)
            val newest = first.first()
            assertEquals(listOf(photo.toString(), null), newest.photos)
            assertTrue(newest.favorite)
            assertEquals("망원동", newest.placeName)
            assertEquals(LocalDate(2000, 3, 15), first.last().date)
            assertNull(archive.page(after = third.last()).firstOrNull())
        }
    }

    @Test
    fun orders_by_diary_date_not_by_insertion() = runTest {
        TempDir().use { dir ->
            val paths = paths(dir)
            // 최근 날짜를 먼저 쓰고 지난 날짜를 나중에 골라 쓴 기기
            LegacyFixtures
                .create(
                    paths.snapshotDir / "note.db",
                    listOf("2024-10-31 21:30:00", "2024-10-30 23:59:457", "2024-11-02 08:00:00", "2024-10-29 15:10:22").map {
                        LegacyFixtures.Row(it)
                    }
                ).close()
            val archive = SnapshotLegacyArchive(dir.fs, paths, TestClock(), TimeZone.of("Asia/Seoul"))

            val first = archive.page(after = null, limit = 2)
            val rest = archive.page(after = first.last(), limit = 2)

            assertEquals(listOf(3L, 1L, 2L, 4L), (first + rest).map { it.id })
        }
    }
}
