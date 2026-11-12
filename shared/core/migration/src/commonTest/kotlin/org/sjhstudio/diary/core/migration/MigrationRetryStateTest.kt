package org.sjhstudio.diary.core.migration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.sjhstudio.diary.core.model.MigrationOutcome
import org.sjhstudio.diary.core.model.MigrationStep

/** 끝나지 못한 이전이 다음 기회와 사진 사본을 잃지 않는지 */
class MigrationRetryStateTest {
    private val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 1, 2, 3)

    /** 다시 읽기가 끝나지 못하면 같은 버전에서 다음에 켤 때 다시 읽음 */
    @Test
    fun a_partial_retry_that_did_not_finish_runs_again_on_the_same_version() = runTest {
        TempDir().use { dir ->
            val env = MigrationEnv(dir)
            val healthy = dir.root / "healthy.db"
            LegacyFixtures.create(healthy, LegacyFixtures.manyRows(2_000, contentLength = 300)).close()
            dir.fs.createDirectories(env.legacy.noteDb.parent!!)
            dir.fs.copy(healthy, env.legacy.noteDb)
            LegacyFixtures.corruptMiddlePages(env.legacy.noteDb)
            val first = assertIs<MigrationResult.Finished>(env.runner().run())
            assertEquals(MigrationOutcome.PARTIAL, first.report.outcome)
            val snapshot = env.paths.snapshotDir / "note.db"
            val db = env.open(env.paths.databasePath.toString())
            try {
                LegacyFixtures.corruptHeader(snapshot)
                assertEquals(0, env.runner(versionCode = 38).retryPartial(db))

                dir.fs.delete(snapshot)
                dir.fs.copy(healthy, snapshot)
                assertEquals(first.report.entriesUnreadable, env.runner(versionCode = 38).retryPartial(db))
                assertEquals(2_000, db.entryDao().countAll())
                assertNull(env.runner(versionCode = 38).retryPartial(db))
            } finally {
                db.close()
            }
        }
    }

    /** 실패한 채 앱이 열려도 옮겨 둔 사진 사본을 정리가 지우면 안 됨 */
    @Test
    fun a_failed_migration_keeps_photo_copies() = runTest {
        TempDir().use { dir ->
            val env = MigrationEnv(dir)
            val picture = env.photo("a.jpg", jpeg)
            LegacyFixtures.create(
                env.legacy.noteDb,
                listOf(LegacyFixtures.Row("2024-01-01 10:00:00", picture = picture.toString()))
            ).close()

            assertIs<MigrationResult.Failed>(env.runner(events = env.failAtVerifying()).run())

            assertEquals(MigrationStep.FAILED, env.runner().state().step)
            assertTrue(env.runner().keepsPhotoCopies())
        }
    }
}
