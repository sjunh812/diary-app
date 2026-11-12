package org.sjhstudio.diary.core.migration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.sjhstudio.diary.core.model.MigrationStep

class MigrationCommitTest {
    /** 끝났다고 적지 못해 앱이 그대로 열림. 그 사이 고친 일기를 다음 실행이 지우면 안 됨 */
    @Test
    fun an_entry_edited_before_the_finish_was_recorded_survives() = runTest {
        TempDir().use { dir ->
            val env = MigrationEnv(dir)
            LegacyFixtures.create(env.legacy.noteDb, LegacyFixtures.manyRows(3)).close()
            assertIs<MigrationResult.Finished>(env.runner().run())
            val db = env.open(env.paths.databasePath.toString())
            val entry = checkNotNull(db.entryDao().getByLegacyId(1))
            db.entryDao().update(entry.copy(plainText = "고친 글", bodyHtml = "<p>고친 글</p>"))
            db.close()
            val store = MigrationStateStore(dir.fs, env.paths.migrationDir)
            store.writeState(store.readState().copy(step = MigrationStep.COMMITTING))

            env.runner().run()

            val again = env.open(env.paths.databasePath.toString())
            assertEquals(3, again.entryDao().countAll())
            assertEquals("고친 글", again.entryDao().get(entry.id)?.plainText)
            again.close()
        }
    }

    /** 빈 채 열렸던 옛 DB의 WAL이 남아 있으면 새 DB 위에 다시 적용될 수 있음 */
    @Test
    fun leftover_wal_of_the_old_empty_database_is_removed_before_the_swap() = runTest {
        TempDir().use { dir ->
            val env = MigrationEnv(dir)
            LegacyFixtures.create(env.legacy.noteDb, LegacyFixtures.manyRows(3)).close()
            val wal = dir.write("databases/diary.db-wal", byteArrayOf(1, 2, 3))
            val shm = dir.write("databases/diary.db-shm", byteArrayOf(1, 2, 3))

            assertIs<MigrationResult.Finished>(env.runner().run())

            assertFalse(dir.fs.exists(wal))
            assertFalse(dir.fs.exists(shm))
            val db = env.open(env.paths.databasePath.toString())
            assertEquals(3, db.entryDao().countAll())
            db.close()
        }
    }

    /** 이름을 바꾼 뒤 끊긴 DB를 열지 못함. 일기가 있는지 모르니 지우지 않음 */
    @Test
    fun a_committed_database_that_cannot_be_opened_is_not_deleted() = runTest {
        TempDir().use { dir ->
            val env = MigrationEnv(dir)
            LegacyFixtures.create(env.legacy.noteDb, LegacyFixtures.manyRows(3)).close()
            assertIs<MigrationResult.Finished>(env.runner().run())
            val store = MigrationStateStore(dir.fs, env.paths.migrationDir)
            store.writeState(store.readState().copy(step = MigrationStep.COMMITTING))

            val result = env.runner(openDb = { error("열지 못함") }).run()

            assertIs<MigrationResult.Failed>(result)
            assertTrue(dir.fs.exists(env.paths.databasePath))
        }
    }
}
