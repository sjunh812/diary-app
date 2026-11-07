package org.sjhstudio.diary.core.migration

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.sjhstudio.diary.core.datastore.SettingsStore
import org.sjhstudio.diary.core.model.AppSettings
import org.sjhstudio.diary.core.model.DiaryFont
import org.sjhstudio.diary.core.model.MigrationStep
import org.sjhstudio.diary.core.model.ThemeMode
import org.sjhstudio.diary.core.testing.InMemoryDataStore

// 옛 설정 잇기를 언제 하는지(스펙 001 T067).
class LegacySettingsBootstrapTest {
    private val store = SettingsStore(InMemoryDataStore<Preferences>(emptyPreferences()))

    private suspend fun TempDir.bootstrap(step: MigrationStep = MigrationStep.NOT_STARTED): Boolean {
        val files = LegacyFiles(root / "databases/note.db", root / "shared_prefs/pref.xml", root / "shared_prefs/pref2.xml")
        return LegacySettingsBootstrap(fs, files, { step }, store).run()
    }

    @Test
    fun copies_settings_before_migration_and_keeps_originals() = runTest {
        TempDir().use { dir ->
            val pref = "<map>\n    <int name=\"font_key\" value=\"5\" />\n    <int name=\"mode_key\" value=\"2\" />\n</map>"
            val pref2 =
                listOf(
                    "<map>",
                    "    <boolean name=\"is_alarm_key\" value=\"true\" />",
                    "    <int name=\"hour_key\" value=\"7\" />",
                    "    <int name=\"minute_key\" value=\"5\" />",
                    "</map>"
                ).joinToString("\n")
            dir.write("shared_prefs/pref.xml", pref)
            dir.write("shared_prefs/pref2.xml", pref2)

            assertTrue(dir.bootstrap())
            val settings = store.settings.first()
            assertEquals(DiaryFont.NANUM_CHEER, settings.font)
            assertEquals("미니콩다방체", settings.fontReplacedFrom)
            assertEquals(ThemeMode.DARK, settings.themeMode)
            assertEquals(7 * 60 + 5, settings.reminderMinuteOfDay)
            assertEquals(pref, dir.fs.read(dir.root / "shared_prefs/pref.xml") { readUtf8() })
        }
    }

    /** 이전을 시작한 뒤에는 새 앱에서 바꾼 설정을 옛 파일로 덮지 않는다. */
    @Test
    fun does_nothing_after_migration_started_or_on_new_install() = runTest {
        TempDir().use { dir ->
            dir.write("shared_prefs/pref.xml", "<map>\n    <int name=\"mode_key\" value=\"2\" />\n</map>")
            for (step in MigrationStep.entries - MigrationStep.NOT_STARTED) assertFalse(dir.bootstrap(step), step.id)
            assertEquals(AppSettings(), store.settings.first())
        }
        TempDir().use { dir -> assertFalse(dir.bootstrap()) }
    }
}
