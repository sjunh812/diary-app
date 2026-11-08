package org.sjhstudio.diary.core.migration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest

// 다시 시도를 연달아 누르거나 설정의 다시 옮기기와 겹쳐도 이전은 한 번만 돈다.
class OneAtATimeMigrationTest {
    private val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 1, 2, 3)

    @Test
    fun two_runs_at_once_migrate_only_once() = runTest {
        TempDir().use { dir ->
            val env = MigrationEnv(dir)
            val a = env.photo("a.jpg", jpeg)
            LegacyFixtures
                .create(
                    env.legacy.noteDb,
                    (1..40).map {
                        LegacyFixtures.Row("2024-03-01 21:${it.toString().padStart(2, '0')}:00", contents = "일기 $it", picture = "$a,")
                    }
                ).close()
            val migration = OneAtATimeMigration(env.runner())

            val results = coroutineScope { listOf(async { migration.run() }, async { migration.run() }).awaitAll() }

            assertEquals(1, results.count { it is MigrationResult.Finished })
            assertEquals(1, results.count { it is MigrationResult.AlreadyFinished })
            val db = env.open(env.paths.databasePath.toString())
            try {
                assertEquals(40, db.backupDao().entryRows())
            } finally {
                db.close()
            }
        }
    }
}
