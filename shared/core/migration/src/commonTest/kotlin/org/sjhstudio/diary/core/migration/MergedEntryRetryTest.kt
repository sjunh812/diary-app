package org.sjhstudio.diary.core.migration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.sjhstudio.diary.core.database.asEntity
import org.sjhstudio.diary.core.model.Entry
import org.sjhstudio.diary.core.model.MigrationStep

/**
 * 이전을 미루거나 일부만 옮긴 뒤 다른 기기 백업을 합친 기기(`docs/migration.md` "미룬 이전과 합친 일기").
 * 번호가 겹쳐도 옛 일기를 건너뛰거나 합쳐 온 일기를 지우면 안 된다.
 */
class MergedEntryRetryTest {
    private val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 1, 2, 3)
    private val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 9, 9)

    /** 다른 기기에서 이전된 일기가 합쳐 왔다. 같은 옛 번호를 들고 있어도 이 기기의 일기와는 다른 일기다. */
    private fun MigrationEnv.mergedFromOtherPhone(legacyId: Int, createDate: String, text: String) = Entry(
        "other-$legacyId",
        LocalDate(2020, 5, 5),
        LocalTime(8, 0),
        "<p>$text</p>",
        text,
        null,
        null,
        null,
        false,
        clock.now(),
        clock.now(),
        legacyId = legacyId,
        legacyCreateDate = createDate
    )

    /** 일기 두 편을 이전하다 실패하고 "새 일기부터 쓰기"를 고른 기기. */
    private suspend fun MigrationEnv.deferredWithTwoOldEntries() {
        val picture = photo("a.jpg", jpeg)
        LegacyFixtures.create(
            legacy.noteDb,
            listOf(
                LegacyFixtures.Row("2024-01-01 10:00:00", contents = "첫째", picture = picture.toString()),
                LegacyFixtures.Row("2024-01-02 10:00:00", contents = "둘째")
            )
        ).close()
        assertIs<MigrationResult.Failed>(runner(events = failAtVerifying()).run())
        runner().defer()
    }

    @Test
    fun deferred_retry_moves_old_entries_even_when_a_merged_entry_has_the_same_number() = runTest {
        TempDir().use { dir ->
            val env = MigrationEnv(dir)
            env.deferredWithTwoOldEntries()
            val db = env.open(env.paths.databasePath.toString())
            try {
                val other = env.mergedFromOtherPhone(1, "2020-05-05 08:00:00", "다른 기기 일기")
                db.entryDao().insertAll(listOf(other.asEntity()))

                val result = assertIs<MigrationResult.Finished>(env.runner().retryDeferred(db))

                assertEquals(2, result.report.entriesMigrated)
                assertEquals(3, db.entryDao().countAll())
                assertEquals("첫째", db.entryDao().getByLegacyId(1)?.plainText)
                assertEquals("둘째", db.entryDao().getByLegacyId(2)?.plainText)
                // 다른 기기 일기는 내용 그대로 남고 번호만 내준다.
                val kept = assertNotNull(db.entryDao().get(other.id))
                assertEquals("다른 기기 일기", kept.plainText)
                assertNull(kept.legacyId)
                assertEquals("2020-05-05 08:00:00", kept.legacyCreateDate)
                assertEquals(1, result.report.photosFound)
            } finally {
                db.close()
            }
            assertEquals(MigrationStep.DONE, env.runner().state().step)
        }
    }

    /** 검증이 번호로 찾다가 다른 기기 일기를 지우면 안 된다. */
    @Test
    fun a_failed_check_during_a_deferred_retry_never_removes_a_merged_entry() = runTest {
        TempDir().use { dir ->
            val env = MigrationEnv(dir)
            env.deferredWithTwoOldEntries()
            val db = env.open(env.paths.databasePath.toString())
            try {
                val other = env.mergedFromOtherPhone(1, "2020-05-05 08:00:00", "다른 기기 일기")
                db.entryDao().insertAll(listOf(other.asEntity()))

                env.breakFingerprints()
                env.runner().retryDeferred(db)

                assertEquals("다른 기기 일기", db.entryDao().get(other.id)?.plainText)
            } finally {
                db.close()
            }
        }
    }

    /** 같은 옛 기록을 나눠 가진 두 기기: 같은 시각, 같은 글이면 같은 일기라서 두 번 넣지 않는다. */
    @Test
    fun deferred_retry_does_not_add_what_a_merged_backup_already_brought() = runTest {
        TempDir().use { dir ->
            val env = MigrationEnv(dir)
            env.deferredWithTwoOldEntries()
            val db = env.open(env.paths.databasePath.toString())
            try {
                val other = env.mergedFromOtherPhone(1, "2024-01-01 10:00:00", "첫째")
                val renumbered = env.mergedFromOtherPhone(7, "2024-01-02 10:00:00", "둘째")
                db.entryDao().insertAll(listOf(other.asEntity(), renumbered.asEntity()))

                assertIs<MigrationResult.Finished>(env.runner().retryDeferred(db))

                assertEquals(2, db.entryDao().countAll())
            } finally {
                db.close()
            }
            assertEquals(MigrationStep.DONE, env.runner().state().step)
        }
    }

    /** 같은 옛 일기를 다른 기기에서 고쳤으면 어느 쪽 글도 잃지 않는다. */
    @Test
    fun deferred_retry_keeps_both_when_the_merged_entry_was_edited_elsewhere() = runTest {
        TempDir().use { dir ->
            val env = MigrationEnv(dir)
            env.deferredWithTwoOldEntries()
            val db = env.open(env.paths.databasePath.toString())
            try {
                val other = env.mergedFromOtherPhone(1, "2024-01-01 10:00:00", "다른 기기에서 고친 글")
                db.entryDao().insertAll(listOf(other.asEntity()))

                assertIs<MigrationResult.Finished>(env.runner().retryDeferred(db))

                assertEquals(3, db.entryDao().countAll())
                assertEquals("다른 기기에서 고친 글", db.entryDao().get(other.id)?.plainText)
                assertEquals("첫째", db.entryDao().getByLegacyId(1)?.plainText)
            } finally {
                db.close()
            }
        }
    }

    /** 일부만 옮긴 기기에서도 번호가 같다고 빠진 일기를 옮긴 일기로 치지 않는다. 사용자가 고친 일기는 다시 넣지 않는다. */
    @Test
    fun partial_retry_tells_a_merged_entry_from_an_edited_one() = runTest {
        TempDir().use { dir ->
            val env = MigrationEnv(dir)
            val first = env.photo("a.jpg", jpeg)
            val second = env.photo("b.png", png)
            LegacyFixtures.create(
                env.legacy.noteDb,
                listOf(
                    LegacyFixtures.Row("2024-01-01 10:00:00", contents = "첫째", picture = first.toString()),
                    LegacyFixtures.Row("2024-01-02 10:00:00", contents = "둘째", picture = second.toString())
                )
            ).close()
            assertIs<MigrationResult.Finished>(env.runner(events = env.onVerifying { env.breakFingerprintOf(second) }).run())
            assertEquals(MigrationStep.PARTIAL, env.runner().state().step)
            env.restoreFingerprintOf(second)

            val db = env.open(env.paths.databasePath.toString())
            try {
                val moved = assertNotNull(db.entryDao().getByLegacyId(1))
                db.entryDao().insertAll(listOf(env.mergedFromOtherPhone(2, "2020-05-05 08:00:00", "다른 기기 일기").asEntity()))
                db.backupDao().updateEntry(moved.copy(plainText = "고친 글", bodyHtml = "<p>고친 글</p>"))

                assertEquals(1, env.runner(versionCode = 38).retryPartial(db))

                assertEquals(3, db.entryDao().countAll())
                assertEquals("고친 글", db.entryDao().getByLegacyId(1)?.plainText)
                assertEquals("둘째", db.entryDao().getByLegacyId(2)?.plainText)
            } finally {
                db.close()
            }
            assertEquals(MigrationStep.DONE, env.runner().state().step)
        }
    }
}
