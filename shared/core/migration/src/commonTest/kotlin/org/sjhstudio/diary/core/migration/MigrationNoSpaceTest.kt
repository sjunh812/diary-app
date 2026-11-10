package org.sjhstudio.diary.core.migration

import androidx.room3.Room
import androidx.room3.RoomDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import okio.FileSystem
import okio.ForwardingFileSystem
import okio.IOException
import okio.Path
import okio.Sink
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.database.DiaryDatabaseConstructor
import org.sjhstudio.diary.core.database.configured
import org.sjhstudio.diary.core.model.MigrationFailureKind
import org.sjhstudio.diary.core.testing.TestClock

// 저장 공간이 꽉 찬 기기의 이전. 앱이 꺼지지 않고 공간 부족을 알려야 함
class MigrationNoSpaceTest {
    private class Env(val dir: TempDir) {
        var free = Long.MAX_VALUE
        var ids = 0
        val paths =
            MigrationPaths(
                legacy =
                    LegacyFiles(
                        noteDb = dir.root / "databases" / "note.db",
                        prefXml = dir.root / "shared_prefs" / "pref.xml",
                        pref2Xml = dir.root / "shared_prefs" / "pref2.xml"
                    ),
                snapshotDir = dir.root / "files" / "legacy-snapshot",
                migrationDir = dir.root / "files" / "migration",
                mediaDir = dir.root / "files" / "media",
                databasePath = dir.root / "databases" / "diary.db"
            )

        /** 새 파일은 못 씀. 원본 읽기는 됨 */
        val fullDisk: FileSystem =
            object : ForwardingFileSystem(dir.fs) {
                override fun sink(file: Path, mustCreate: Boolean): Sink = throw IOException("ENOSPC")
            }

        fun runner(fileSystem: FileSystem = dir.fs) = MigrationRunner(
            fileSystem = fileSystem,
            paths = paths,
            clock = TestClock(),
            timeZone = TimeZone.of("Asia/Seoul"),
            newId = { "id-${ids++}" },
            freeBytes = { free },
            openDatabase = ::open,
            imageSize = { 100 to 50 }
        )

        private fun open(path: String): DiaryDatabase = Room
            .databaseBuilder(path, DiaryDatabaseConstructor::initialize)
            .configured()
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
            .build()
    }

    private fun Env.oneEntry() = LegacyFixtures.create(paths.legacy.noteDb, listOf(LegacyFixtures.Row("2024-01-01 10:00:00"))).close()

    /** 공간이 0인 기기: 상태 파일도 못 쓰지만 꺼지지 않고 얼마나 더 필요한지 알림 */
    @Test
    fun a_full_disk_on_the_first_step_says_so_instead_of_crashing() = runTest {
        TempDir().use { dir ->
            val env = Env(dir)
            env.oneEntry()
            env.free = 0

            val failed = assertIs<MigrationResult.Failed>(env.runner(env.fullDisk).run())

            assertEquals(MigrationFailureKind.NO_SPACE, failed.kind)
            assertTrue(checkNotNull(failed.neededBytes) > 0)
        }
    }

    /** 여유가 있다고 읽혔는데 막상 못 쓸 때도 앱이 꺼지지 않음 */
    @Test
    fun a_write_failure_with_room_left_fails_softly() = runTest {
        TempDir().use { dir ->
            val env = Env(dir)
            env.oneEntry()

            val failed = assertIs<MigrationResult.Failed>(env.runner(env.fullDisk).run())

            assertEquals(MigrationFailureKind.UNKNOWN, failed.kind)
        }
    }

    /** 공간을 잰 뒤 시작하자마자 찼어도 까닭은 공간 부족 */
    @Test
    fun running_out_after_the_check_is_still_no_space() = runTest {
        TempDir().use { dir ->
            val env = Env(dir)
            env.oneEntry()
            var checks = 0
            val runner =
                MigrationRunner(
                    fileSystem = env.fullDisk,
                    paths = env.paths,
                    clock = TestClock(),
                    timeZone = TimeZone.of("Asia/Seoul"),
                    newId = { "id" },
                    freeBytes = { if (checks++ == 0) Long.MAX_VALUE else 0 },
                    openDatabase = { error("열지 않는다") },
                    imageSize = { 100 to 50 }
                )

            val failed = assertIs<MigrationResult.Failed>(runner.run())

            assertEquals(MigrationFailureKind.NO_SPACE, failed.kind)
        }
    }

    /** 공간을 비우고 다시 확인하면 끝까지 옮김 */
    @Test
    fun retry_after_freeing_space_finishes() = runTest {
        TempDir().use { dir ->
            val env = Env(dir)
            env.oneEntry()
            env.free = 0
            assertIs<MigrationResult.Failed>(env.runner(env.fullDisk).run())

            env.free = Long.MAX_VALUE

            assertIs<MigrationResult.Finished>(env.runner().run())
        }
    }

    /** 공간이 없어 사본을 만들기 전에 멈췄으면 예전 일기 보기에 볼 것이 없음 */
    @Test
    fun stopping_before_the_copy_leaves_nothing_to_read() = runTest {
        TempDir().use { dir ->
            val env = Env(dir)
            env.oneEntry()
            env.free = 0
            val runner = env.runner()

            runner.run()

            assertFalse(runner.hasLegacyCopy())
        }
    }

    @Test
    fun a_finished_copy_can_be_read() = runTest {
        TempDir().use { dir ->
            val env = Env(dir)
            env.oneEntry()
            val runner = env.runner()

            runner.run()

            assertTrue(runner.hasLegacyCopy())
        }
    }
}
