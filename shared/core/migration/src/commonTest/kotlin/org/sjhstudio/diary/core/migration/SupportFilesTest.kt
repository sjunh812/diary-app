package org.sjhstudio.diary.core.migration

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import org.sjhstudio.diary.core.testing.TestClock

class SupportFilesTest {
    /** 메일 앱이 첨부를 읽을 시간을 두고, 하루가 지나면 지움(FR-010a) */
    @Test
    fun bundle_is_kept_for_a_day_then_deleted() {
        TempDir().use { dir ->
            val clock = TestClock()
            val files = SupportFiles(dir.fs, dir.root / "migration", clock)
            val folder = files.prepare()
            dir.fs.write(folder / "bundle.zip") { writeUtf8("zip") }

            clock.advance(23.hours)
            assertFalse(files.deleteExpired())
            assertTrue(dir.fs.exists(folder / "bundle.zip"))

            clock.advance(1.hours)
            assertTrue(files.deleteExpired())
            assertFalse(dir.fs.exists(folder))
        }
    }

    /** 만든 시각을 모르는 묶음은 남겨 두지 않음 */
    @Test
    fun bundle_without_stamp_is_deleted() {
        TempDir().use { dir ->
            val files = SupportFiles(dir.fs, dir.root / "migration", TestClock())
            dir.write("migration/support/bundle.zip", "zip")
            assertTrue(files.deleteExpired())
            assertFalse(dir.fs.exists(dir.root / "migration" / "support"))
        }
    }

    @Test
    fun nothing_to_delete() {
        TempDir().use { dir ->
            assertFalse(SupportFiles(dir.fs, dir.root / "migration", TestClock()).deleteExpired())
        }
    }
}
