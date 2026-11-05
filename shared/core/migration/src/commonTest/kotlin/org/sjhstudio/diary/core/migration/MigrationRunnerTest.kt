package org.sjhstudio.diary.core.migration

import androidx.room3.Room
import androidx.room3.RoomDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import okio.Path
import org.sjhstudio.diary.core.database.DiaryDatabase
import org.sjhstudio.diary.core.database.DiaryDatabaseConstructor
import org.sjhstudio.diary.core.database.asEntity
import org.sjhstudio.diary.core.database.asModel
import org.sjhstudio.diary.core.database.configured
import org.sjhstudio.diary.core.model.Entry
import org.sjhstudio.diary.core.model.MediaStatus
import org.sjhstudio.diary.core.model.MigratedSetting
import org.sjhstudio.diary.core.model.MigrationFailureKind
import org.sjhstudio.diary.core.model.MigrationOutcome
import org.sjhstudio.diary.core.model.MigrationStep
import org.sjhstudio.diary.core.testing.TestClock

// docs/migration.md "시험"과 contracts/migration-flow.md 단계(스펙 001 T037).
class MigrationRunnerTest {
    private class Env(val dir: TempDir) {
        val clock = TestClock()
        var ids = 0
        var free = Long.MAX_VALUE
        val legacy =
            LegacyFiles(
                noteDb = dir.root / "databases" / "note.db",
                prefXml = dir.root / "shared_prefs" / "pref.xml",
                pref2Xml = dir.root / "shared_prefs" / "pref2.xml"
            )
        val paths =
            MigrationPaths(
                legacy = legacy,
                snapshotDir = dir.root / "files" / "legacy-snapshot",
                migrationDir = dir.root / "files" / "migration",
                mediaDir = dir.root / "files" / "media",
                databasePath = dir.root / "databases" / "diary.db"
            )

        fun open(path: String): DiaryDatabase = Room
            .databaseBuilder(path, DiaryDatabaseConstructor::initialize)
            .configured()
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
            .build()

        fun runner(
            versionCode: Int = 37,
            makeThumbnail: (suspend (Path) -> Boolean)? = null,
            openDb: (String) -> DiaryDatabase = ::open,
            events: MigrationEvents = object : MigrationEvents {}
        ) = MigrationRunner(
            fileSystem = dir.fs,
            paths = paths,
            clock = clock,
            timeZone = TimeZone.of("Asia/Seoul"),
            newId = { "id-${ids++}" },
            freeBytes = { free },
            openDatabase = openDb,
            imageSize = { 100 to 50 },
            versionCode = versionCode,
            makeThumbnail = makeThumbnail,
            events = events
        )

        /** 검증에 들어설 때 [action]을 한다. 복사와 검증 사이에 무언가 어긋난 상황을 만든다. */
        fun onVerifying(action: () -> Unit) = object : MigrationEvents {
            override fun stepStarted(step: MigrationStep) {
                if (step == MigrationStep.VERIFYING) action()
            }
        }

        /** 사진 목록의 해시를 바꿔 그 사진이 든 일기의 지문이 어긋나게 한다. */
        fun breakFingerprints() {
            val list = paths.photosJson
            val text = dir.fs.read(list) { readUtf8() }
            dir.fs.write(list) { writeUtf8(text.replace(Regex("\"sha256\":\"[0-9a-f]{64}\""), "\"sha256\":\"${"0".repeat(64)}\"")) }
        }

        fun photo(name: String, bytes: ByteArray): Path = dir.write("cache/$name", bytes)

        fun sha(path: Path) = LegacySnapshotter(dir.fs, clock).sha256(path)
    }

    private val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 1, 2, 3)
    private val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 9, 9)

    /** 이전 뒤 사용자가 새로 쓴 일기. */
    private fun Env.written() = Entry(
        "mine",
        LocalDate(
            2026,
            11,
            2
        ),
        LocalTime(9, 0), "<p>새 일기</p>", "새 일기", null, null, null, false, clock.now(), clock.now()
    )

    private fun Env.writePrefs() {
        dir.write(
            "shared_prefs/pref.xml",
            "<map>\n    <string name=\"password\">1234</string>\n    <int name=\"font_key\" value=\"1\" />\n</map>\n"
        )
    }

    @Test
    fun fresh_install_has_nothing_to_migrate_and_writes_no_state() = runTest {
        TempDir().use { dir ->
            val env = Env(dir)
            assertIs<MigrationResult.NoLegacy>(env.runner().run())
            assertFalse(dir.fs.exists(env.paths.migrationDir / "state.json"))
            assertFalse(env.runner().needsMigration())
        }
    }

    @Test
    fun migrates_entries_and_photos_and_keeps_originals_unchanged() = runTest {
        TempDir().use { dir ->
            val env = Env(dir)
            val a = env.photo("a.jpg", jpeg)
            val b = env.photo("b", png)
            env.writePrefs()
            LegacyFixtures
                .create(
                    env.legacy.noteDb,
                    listOf(
                        LegacyFixtures.Row(
                            "2024-03-01 21:30:00",
                            contents = "첫 일기\n\n둘째 문단",
                            mood = 7,
                            picture = "$a,${dir.root}/cache/gone.jpg,"
                        ),
                        LegacyFixtures.Row("2024-03-02 23:59:457", contents = "같은 사진 두 번", picture = "$b, $b ,"),
                        LegacyFixtures.Row("2024-03-03 08:00:00", contents = "", star = 1)
                    )
                ).close()
            val originals = listOf(env.legacy.noteDb, env.legacy.prefXml, a, b)
            val before = originals.map(env::sha)

            val result = env.runner().run()

            val finished = assertIs<MigrationResult.Finished>(result)
            assertTrue(finished.showResult)
            with(finished.report) {
                assertEquals(3, entriesMigrated)
                assertEquals(0, entriesUnreadable)
                assertEquals(3, photosFound)
                assertEquals(1, photosMissing)
                assertEquals(1, dateDiffers)
                assertEquals(MigrationOutcome.DONE, outcome)
                // 옛 설정 파일에는 글꼴만 있었다.
                assertEquals(listOf(MigratedSetting.FONT), settingsMigrated)
            }
            // 원본은 바이트 하나 바뀌지 않는다(SC-002).
            assertEquals(before, originals.map(env::sha))
            assertEquals(MigrationStep.DONE, env.runner().state().step)
            assertFalse(dir.fs.exists(env.paths.newDatabase))
            // 임시 DB 자리에는 상태·결과 파일만 남는다(잠금·저널 파일까지 치운다).
            assertEquals(setOf("state.json", "report.json"), dir.fs.list(env.paths.migrationDir).map { it.name }.toSet())
            assertTrue(dir.fs.exists(env.paths.snapshotDir / "snapshot.json"))
            assertEquals(4, dir.fs.read(env.paths.photosJson) { readUtf8() }.split("\"path\"").size - 1)
            // 같은 파일은 하나만 둔다.
            assertEquals(setOf("${env.sha(a)}.jpg", "${env.sha(b)}.png"), dir.fs.list(env.paths.mediaDir).map { it.name }.toSet())

            val db = env.open(env.paths.databasePath.toString())
            try {
                val first = db.entryDao().getByLegacyId(1)!!.asModel()
                assertEquals(LocalDate(2024, 3, 1), first.date)
                assertEquals("<p>첫 일기</p><p>둘째 문단</p>", first.bodyHtml)
                val media = db.mediaDao().forEntry(first.id).map { it.asModel() }
                assertEquals(listOf(MediaStatus.NORMAL, MediaStatus.MISSING), media.map { it.status })
                assertEquals(listOf(a.toString(), "${dir.root}/cache/gone.jpg"), media.map { it.legacyPath })
                assertEquals(100, media[0].width)
                val second = db.entryDao().getByLegacyId(2)!!.asModel()
                assertEquals(LocalTime(23, 59), second.time)
                assertEquals(2, db.mediaDao().forEntry(second.id).count { it.status == "normal" })
                assertTrue(db.entryDao().getByLegacyId(3)!!.asModel().isFavorite)
            } finally {
                db.close()
            }
            assertIs<MigrationResult.AlreadyFinished>(env.runner().run())
        }
    }

    @Test
    fun zero_entries_finishes_without_a_result_screen() = runTest {
        TempDir().use { dir ->
            val env = Env(dir)
            LegacyFixtures.create(env.legacy.noteDb, emptyList()).close()
            val result = assertIs<MigrationResult.Finished>(env.runner().run())
            assertFalse(result.showResult)
            assertEquals(MigrationStep.DONE, env.runner().state().step)
        }
    }

    @Test
    fun interrupted_steps_start_over_and_leftovers_are_removed() = runTest {
        for (step in listOf(MigrationStep.SNAPSHOTTING, MigrationStep.COPYING, MigrationStep.VERIFYING)) {
            TempDir().use { dir ->
                val env = Env(dir)
                LegacyFixtures.create(env.legacy.noteDb, LegacyFixtures.manyRows(450)).close()
                // 그 단계에서 프로세스가 죽은 상태를 만든다.
                MigrationStateStore(dir.fs, env.paths.migrationDir).writeState(
                    org.sjhstudio.diary.core.model.MigrationState(step = step, attempt = 1)
                )
                dir.write("files/migration/diary-new.db", "반쯤 쓴 DB")

                val result = assertIs<MigrationResult.Finished>(env.runner().run(), "끊긴 단계 $step")
                assertEquals(450, result.report.entriesMigrated)
                assertEquals(2, env.runner().state().attempt)
                assertFalse(dir.fs.exists(env.paths.newDatabase))
            }
        }
    }

    @Test
    fun interrupted_after_rename_only_verifies_again() = runTest {
        TempDir().use { dir ->
            val env = Env(dir)
            LegacyFixtures.create(env.legacy.noteDb, LegacyFixtures.manyRows(30)).close()
            assertIs<MigrationResult.Finished>(env.runner().run())
            val store = MigrationStateStore(dir.fs, env.paths.migrationDir)
            store.writeState(store.readState().copy(step = MigrationStep.COMMITTING))

            val result = assertIs<MigrationResult.Finished>(env.runner().run())

            assertEquals(30, result.report.entriesMigrated)
            assertEquals(MigrationStep.DONE, env.runner().state().step)
        }
    }

    @Test
    fun not_enough_space_does_not_start() = runTest {
        TempDir().use { dir ->
            val env = Env(dir)
            val photo = env.photo("a.jpg", jpeg)
            LegacyFixtures.create(env.legacy.noteDb, listOf(LegacyFixtures.Row("2024-01-01 10:00:00", picture = "$photo,"))).close()
            env.free = 10

            val failed = assertIs<MigrationResult.Failed>(env.runner().run())

            assertEquals(MigrationFailureKind.NO_SPACE, failed.kind)
            assertTrue((failed.neededBytes ?: 0) > 50L * 1024 * 1024)
            assertEquals(MigrationStep.FAILED, env.runner().state().step)
            assertFalse(dir.fs.exists(env.paths.databasePath))
            assertFalse(dir.fs.exists(env.paths.mediaDir))
            assertTrue(env.runner().needsMigration())
        }
    }

    @Test
    fun photos_wait_when_copying_would_leave_the_cache_at_risk() = runTest {
        TempDir().use { dir ->
            val env = Env(dir)
            val photo = env.photo("a.jpg", jpeg)
            LegacyFixtures.create(env.legacy.noteDb, listOf(LegacyFixtures.Row("2024-01-01 10:00:00", picture = "$photo,"))).close()
            // 복사할 자리는 충분하지만 OS가 캐시를 지우기 시작하는 선에 가깝다.
            env.free = 500L * 1024 * 1024

            val failed = assertIs<MigrationResult.Failed>(env.runner().run())

            assertEquals(MigrationFailureKind.NO_SPACE, failed.kind)
            assertTrue(dir.fs.exists(photo))
        }
    }

    @Test
    fun failure_keeps_photo_copies_and_snapshot() = runTest {
        TempDir().use { dir ->
            val env = Env(dir)
            val photo = env.photo("a.jpg", jpeg)
            LegacyFixtures.create(env.legacy.noteDb, listOf(LegacyFixtures.Row("2024-01-01 10:00:00", picture = "$photo,"))).close()
            // 쓰기가 실패하는 DB: 열자마자 닫는다.
            val failed = assertIs<MigrationResult.Failed>(env.runner(openDb = { path -> env.open(path).also { it.close() } }).run())

            assertEquals(MigrationFailureKind.WRITE_ERROR, failed.kind)
            assertTrue(dir.fs.exists(env.paths.mediaDir / "${env.sha(photo)}.jpg"))
            assertTrue(dir.fs.exists(env.paths.snapshotDir / "note.db"))
            assertFalse(dir.fs.exists(env.paths.newDatabase))
            assertTrue(dir.fs.exists(env.legacy.noteDb))
        }
    }

    @Test
    fun partial_reads_again_once_after_an_update_and_keeps_new_entries() = runTest {
        TempDir().use { dir ->
            val env = Env(dir)
            val healthy = dir.root / "healthy.db"
            LegacyFixtures.create(healthy, LegacyFixtures.manyRows(2_000, contentLength = 300)).close()
            dir.fs.createDirectories(env.legacy.noteDb.parent!!)
            dir.fs.copy(healthy, env.legacy.noteDb)
            LegacyFixtures.corruptMiddlePages(env.legacy.noteDb)

            val first = assertIs<MigrationResult.Finished>(env.runner().run())
            assertEquals(MigrationOutcome.PARTIAL, first.report.outcome)
            assertTrue(first.report.entriesUnreadable > 0)
            assertEquals(MigrationStep.PARTIAL, env.runner().state().step)

            val db = env.open(env.paths.databasePath.toString())
            try {
                // 이전 뒤 사용자가 새로 쓴 일기
                val written = env.written()
                db.entryDao().insertAll(listOf(written.asEntity()))

                // 같은 버전에서는 같은 파일을 같은 방법으로 읽어 결과가 같으므로 다시 읽지 않는다.
                assertNull(env.runner().retryPartial(db))

                // 새 버전이 더 잘 읽는다고 치고 사본을 멀쩡한 파일로 바꾼다.
                dir.fs.delete(env.paths.snapshotDir / "note.db")
                dir.fs.copy(healthy, env.paths.snapshotDir / "note.db")
                val added = env.runner(versionCode = 38).retryPartial(db)

                assertEquals(first.report.entriesUnreadable, added)
                assertEquals(2_001, db.entryDao().countAll())
                assertTrue(db.entryDao().get("mine") != null)
                assertEquals(MigrationStep.DONE, env.runner().state().step)
                assertNull(env.runner(versionCode = 39).retryPartial(db))
            } finally {
                db.close()
            }
        }
    }

    /** 몇 편이 어긋나도 나머지는 옮기고 앱을 쓰게 한다. 뺀 일기는 업데이트 뒤 다시 읽는다(2026-11-04 사용자 결정). */
    @Test
    fun an_entry_that_fails_checking_is_left_out_and_the_rest_moves() = runTest {
        TempDir().use { dir ->
            val env = Env(dir)
            val picture = env.photo("a.jpg", jpeg)
            LegacyFixtures.create(
                env.legacy.noteDb,
                listOf(
                    LegacyFixtures.Row("2024-01-01 10:00:00", contents = "사진 없는 일기"),
                    LegacyFixtures.Row("2024-01-02 10:00:00", contents = "사진 있는 일기", picture = picture.toString())
                )
            ).close()

            val result = assertIs<MigrationResult.Finished>(env.runner(events = env.onVerifying(env::breakFingerprints)).run())

            assertEquals(MigrationOutcome.PARTIAL, result.report.outcome)
            assertEquals(1, result.report.entriesMigrated)
            assertEquals(1, result.report.entriesUnreadable)
            assertEquals(0, result.report.photosFound)
            assertEquals(MigrationStep.PARTIAL, env.runner().state().step)
            assertTrue(dir.fs.exists(env.paths.databasePath))
            val db = env.open(env.paths.databasePath.toString())
            try {
                assertEquals(1, db.entryDao().countAll())
                // 다음 버전은 뺀 일기를 다시 읽어 옮긴다.
                assertEquals(1, env.runner(versionCode = 38).retryPartial(db))
                assertEquals(2, db.entryDao().countAll())
            } finally {
                db.close()
            }
            assertEquals(MigrationStep.DONE, env.runner().state().step)
        }
    }

    /** 옮길 수 있는 일기가 한 편도 없으면 빈 앱으로 넘기지 않고 알린다. */
    @Test
    fun nothing_moved_is_a_failure() = runTest {
        TempDir().use { dir ->
            val env = Env(dir)
            val picture = env.photo("a.jpg", jpeg)
            LegacyFixtures.create(
                env.legacy.noteDb,
                listOf(LegacyFixtures.Row("2024-01-01 10:00:00", picture = picture.toString()))
            ).close()

            val failed = assertIs<MigrationResult.Failed>(env.runner(events = env.onVerifying(env::breakFingerprints)).run())

            assertEquals(MigrationFailureKind.MISMATCH, failed.kind)
            assertTrue(dir.fs.exists(env.paths.snapshotDir / "note.db"))
        }
    }

    /** 새 일기부터 쓰기: 앱을 쓰다가 다시 시도하면 그 사이 쓴 일기를 두고 예전 일기를 더한다. */
    @Test
    fun deferred_migration_adds_old_entries_and_keeps_new_ones() = runTest {
        TempDir().use { dir ->
            val env = Env(dir)
            val picture = env.photo("a.jpg", jpeg)
            LegacyFixtures.create(
                env.legacy.noteDb,
                listOf(LegacyFixtures.Row("2024-01-01 10:00:00", contents = "예전 일기", picture = picture.toString()))
            ).close()
            assertIs<MigrationResult.Failed>(env.runner(events = env.onVerifying(env::breakFingerprints)).run())
            env.runner().defer()
            assertEquals(false, env.runner().needsMigration())
            assertIs<MigrationResult.AlreadyFinished>(env.runner().run())

            val db = env.open(env.paths.databasePath.toString())
            try {
                db.entryDao().insertAll(listOf(env.written().asEntity()))

                val result = assertIs<MigrationResult.Finished>(env.runner().retryDeferred(db))

                assertEquals(1, result.report.entriesMigrated)
                assertEquals(1, result.report.photosFound)
                assertEquals(2, db.entryDao().countAll())
                assertTrue(db.entryDao().get("mine") != null)
            } finally {
                db.close()
            }
            assertEquals(MigrationStep.DONE, env.runner().state().step)
        }
    }

    /** 다시 시도가 실패해도 앱은 그대로 쓰고, 다음 업데이트 때 저절로 다시 해 본다. */
    @Test
    fun deferred_retry_failure_stays_deferred_and_tries_after_an_update() = runTest {
        TempDir().use { dir ->
            val env = Env(dir)
            val picture = env.photo("a.jpg", jpeg)
            LegacyFixtures.create(
                env.legacy.noteDb,
                listOf(LegacyFixtures.Row("2024-01-01 10:00:00", picture = picture.toString()))
            ).close()
            assertIs<MigrationResult.Failed>(env.runner(events = env.onVerifying(env::breakFingerprints)).run())
            env.runner().defer()
            val db = env.open(env.paths.databasePath.toString())
            try {
                assertIs<MigrationResult.AlreadyFinished>(env.runner().retryDeferred(db, onlyAfterUpdate = true))

                env.free = 0
                assertIs<MigrationResult.Failed>(env.runner().retryDeferred(db))
                assertEquals(MigrationStep.DEFERRED, env.runner().state().step)
                assertEquals(0, db.entryDao().countAll())

                env.free = Long.MAX_VALUE
                assertIs<MigrationResult.Finished>(env.runner(versionCode = 38).retryDeferred(db, onlyAfterUpdate = true))
            } finally {
                db.close()
            }
        }
    }

    /** 사진 파일이 깨져도 일기는 옮기고 그 사진만 찾을 수 없는 사진이 된다. 끊겨서 다시 검증해도 일기를 빼지 않는다. */
    @Test
    fun a_broken_photo_file_keeps_the_entry() = runTest {
        TempDir().use { dir ->
            val env = Env(dir)
            val picture = env.photo("a.jpg", jpeg)
            LegacyFixtures.create(
                env.legacy.noteDb,
                listOf(LegacyFixtures.Row("2024-01-01 10:00:00", picture = picture.toString()))
            ).close()
            val broken = env.onVerifying {
                dir.fs.list(env.paths.mediaDir).forEach { file -> dir.fs.write(file) { writeUtf8("깨짐") } }
            }

            val result = assertIs<MigrationResult.Finished>(env.runner(events = broken).run())

            assertEquals(MigrationOutcome.DONE, result.report.outcome)
            assertEquals(1, result.report.entriesMigrated)
            assertEquals(0, result.report.photosFound)
            assertEquals(1, result.report.photosMissing)
            val store = MigrationStateStore(dir.fs, env.paths.migrationDir)
            store.writeState(store.readState().copy(step = MigrationStep.COMMITTING))
            assertEquals(1, assertIs<MigrationResult.Finished>(env.runner().run()).report.entriesMigrated)
        }
    }

    /** 단계마다 상태를 적은 뒤 알린다. 개발 빌드의 강제 종료 훅이 이 알림에서 끊는다(T049). */
    @Test
    fun steps_are_announced_in_order() = runTest {
        TempDir().use { dir ->
            val env = Env(dir)
            LegacyFixtures.create(env.legacy.noteDb, listOf(LegacyFixtures.Row("2024-01-01 10:00:00"))).close()
            val steps = mutableListOf<Pair<MigrationStep, MigrationStep>>()
            val runner =
                MigrationRunner(
                    fileSystem = dir.fs,
                    paths = env.paths,
                    clock = env.clock,
                    timeZone = TimeZone.of("Asia/Seoul"),
                    newId = { "id" },
                    freeBytes = { Long.MAX_VALUE },
                    openDatabase = env::open,
                    imageSize = { 100 to 50 },
                    events =
                        object : MigrationEvents {
                            override fun stepStarted(step: MigrationStep) {
                                steps += step to MigrationStateStore(dir.fs, env.paths.migrationDir).readState().step
                            }
                        }
                )
            assertIs<MigrationResult.Finished>(runner.run())
            val expected = listOf(MigrationStep.SNAPSHOTTING, MigrationStep.COPYING, MigrationStep.VERIFYING, MigrationStep.COMMITTING)
            assertEquals(expected.map { it to it }, steps)
        }
    }

    /** 썸네일을 못 만들어도 이전은 끝나고 수만 센다. 한 번에 3개를 넘게 만들지 않는다(FR-037, R9). */
    @Test
    fun thumbnail_failures_are_counted_not_fatal() = runTest {
        TempDir().use { dir ->
            val env = Env(dir)
            val names = (1..8).map { "p$it.jpg" }
            val rows = names.mapIndexed { i, name ->
                LegacyFixtures.Row(
                    "2024-01-0${i % 9 + 1} 10:00:00",
                    picture = "${env.photo(
                        name,
                        jpeg + byteArrayOf(i.toByte())
                    )},"
                )
            }
            LegacyFixtures.create(env.legacy.noteDb, rows).close()
            // 이전은 IO 스레드에서 돌아 썸네일 만들기가 정말로 나란히 불린다. 세는 값은 잠금 안에서만 바꾼다.
            val lock = Mutex()
            var running = 0
            var most = 0
            val made = mutableListOf<String>()
            val result =
                env.runner(makeThumbnail = { media ->
                    lock.withLock {
                        running++
                        most = maxOf(most, running)
                    }
                    delay(20)
                    lock.withLock {
                        running--
                        made += media.name
                        made.size % 4 != 0
                    }
                }).run()
            val report = assertIs<MigrationResult.Finished>(result).report
            assertEquals(MigrationOutcome.DONE, report.outcome)
            assertEquals(8, made.size)
            assertEquals(2, report.thumbnailsFailed)
            assertTrue(most in 1..3)
            assertTrue(made.all { it.endsWith(".jpg") && it.length > 10 })
        }
    }

    @Test
    fun follow_up_does_not_open_the_diary_database_before_migration() = runTest {
        TempDir().use { dir ->
            val env = Env(dir)
            LegacyFixtures.create(env.legacy.noteDb, listOf(LegacyFixtures.Row("2024-01-01 10:00:00"))).close()
            val result = MigrationFollowUp(env.runner()) { error("이전 전에는 일기 DB를 열지 않는다") }.run()
            assertEquals(FollowUpResult(), result)
            assertFalse(dir.fs.exists(env.paths.databasePath))
        }
    }

    @Test
    fun follow_up_finds_a_photo_that_came_back() = runTest {
        TempDir().use { dir ->
            val env = Env(dir)
            val later = dir.root / "cache" / "later.jpg"
            LegacyFixtures.create(env.legacy.noteDb, listOf(LegacyFixtures.Row("2024-01-01 10:00:00", picture = "$later,"))).close()
            assertIs<MigrationResult.Finished>(env.runner().run())
            val db = env.open(env.paths.databasePath.toString())
            try {
                env.photo("later.jpg", jpeg)
                assertEquals(FollowUpResult(entriesRecovered = 0, photosFound = 1), MigrationFollowUp(env.runner()) { db }.run())
            } finally {
                db.close()
            }
        }
    }

    @Test
    fun missing_photo_is_found_again_at_most_once_a_day() = runTest {
        TempDir().use { dir ->
            val env = Env(dir)
            val later = dir.root / "cache" / "later.jpg"
            LegacyFixtures.create(env.legacy.noteDb, listOf(LegacyFixtures.Row("2024-01-01 10:00:00", picture = "$later,"))).close()
            assertEquals(1, assertIs<MigrationResult.Finished>(env.runner().run()).report.photosMissing)
            val db = env.open(env.paths.databasePath.toString())
            try {
                assertEquals(0, env.runner().recheckMissingPhotos(db))
                env.photo("later.jpg", jpeg)
                assertNull(env.runner().recheckMissingPhotos(db))

                env.clock.advance(24.hours)
                assertEquals(1, env.runner().recheckMissingPhotos(db))

                val media = db.mediaDao().missingWithLegacyPath()
                assertTrue(media.isEmpty())
                assertEquals(1, env.runner().report()!!.photosFound)
            } finally {
                db.close()
            }
        }
    }
}
