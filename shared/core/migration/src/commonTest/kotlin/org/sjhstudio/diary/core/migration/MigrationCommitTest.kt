package org.sjhstudio.diary.core.migration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import org.sjhstudio.diary.core.model.MigrationStep

class MigrationCommitTest {
    /** 끝났다고 적지 못해 앱이 그대로 열렸다. 그 사이 고친 일기를 다음 실행이 지우면 안 된다. */
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
}
