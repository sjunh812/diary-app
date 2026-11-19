package org.sjhstudio.diary.core.migration

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import okio.IOException
import org.sjhstudio.diary.core.datastore.SettingsStore
import org.sjhstudio.diary.core.model.PreviewLength
import org.sjhstudio.diary.core.testing.InMemoryDataStore

// 옛 "일기 3줄 보기" 잇기(스펙 008 US3)
class LegacyListPreviewTest {
    private val backing = InMemoryDataStore<Preferences>(emptyPreferences())
    private val store = SettingsStore(backing)
    private var kept = 0

    private fun pref(vararg lines: String) =
        """
        <?xml version='1.0' encoding='utf-8' standalone='yes' ?>
        <map>
        ${lines.joinToString("\n")}
        </map>
        """.trimIndent()

    private suspend fun TempDir.run(xml: String?): Boolean {
        val files = LegacyFiles(root / "databases/note.db", root / "shared_prefs/pref.xml", root / "shared_prefs/pref2.xml")
        if (xml != null) write("shared_prefs/pref.xml", xml)
        return LegacyListPreview(fs, files, store) { kept++ }.run()
    }

    private suspend fun list() = store.settings.first().list

    @Test
    fun three_lines_off_turns_full_preview_on_and_tells_once() = runTest {
        TempDir().use { dir ->
            val xml = pref("""<boolean name="skip_note" value="false" />""")
            assertTrue(dir.run(xml))
            assertEquals(PreviewLength.FULL, list().previewLength)
            assertTrue(list().legacyChecked)
            assertFalse(list().previewChosen)
            assertEquals(1, kept)
            // 원본은 그대로
            assertEquals(xml, dir.fs.read(dir.root / "shared_prefs/pref.xml") { readUtf8() })
        }
    }

    @Test
    fun three_lines_on_keeps_default() = runTest {
        TempDir().use { dir ->
            assertFalse(dir.run(pref("""<boolean name="skip_note" value="true" />""")))
            assertEquals(PreviewLength.THREE_LINES, list().previewLength)
            assertTrue(list().legacyChecked)
            assertEquals(0, kept)
        }
    }

    @Test
    fun missing_key_or_file_keeps_default() = runTest {
        TempDir().use { dir ->
            assertFalse(dir.run(pref("""<int name="font_key" value="3" />""")))
            assertEquals(PreviewLength.THREE_LINES, list().previewLength)
        }
        TempDir().use { dir ->
            assertFalse(dir.run(null))
            assertTrue(list().legacyChecked)
            assertEquals(0, kept)
        }
    }

    @Test
    fun broken_file_keeps_default() = runTest {
        TempDir().use { dir ->
            assertFalse(dir.run("<map><boolean name=\"skip_note\" value=\"fal"))
            assertEquals(PreviewLength.THREE_LINES, list().previewLength)
            assertEquals(0, kept)
        }
    }

    @Test
    fun runs_only_once() = runTest {
        TempDir().use { dir ->
            val xml = pref("""<boolean name="skip_note" value="false" />""")
            assertTrue(dir.run(xml))
            store.update { it.copy(list = it.list.copy(previewLength = PreviewLength.THREE_LINES)) }
            assertFalse(dir.run(xml))
            assertEquals(PreviewLength.THREE_LINES, list().previewLength)
            assertEquals(1, kept)
        }
    }

    @Test
    fun chosen_length_is_not_overwritten() = runTest {
        TempDir().use { dir ->
            store.update { it.copy(list = it.list.copy(previewLength = PreviewLength.THREE_LINES, previewChosen = true)) }
            assertFalse(dir.run(pref("""<boolean name="skip_note" value="false" />""")))
            assertEquals(PreviewLength.THREE_LINES, list().previewLength)
            assertTrue(list().legacyChecked)
            assertEquals(0, kept)
        }
    }

    @Test
    fun a_failed_read_does_not_undo_a_chosen_length() = runTest {
        TempDir().use { dir ->
            store.update {
                it.copy(list = it.list.copy(previewLength = PreviewLength.THREE_LINES, previewChosen = true, legacyChecked = true))
            }
            val files = LegacyFiles(dir.root / "databases/note.db", dir.root / "shared_prefs/pref.xml", dir.root / "shared_prefs/pref2.xml")
            dir.write("shared_prefs/pref.xml", pref("""<boolean name="skip_note" value="false" />"""))
            // 읽기만 한 번 실패해 기본값이 보이는 저장소
            val flaky = SettingsStore(FirstReadFails(backing))

            assertFalse(LegacyListPreview(dir.fs, files, flaky) { kept++ }.run())
            assertEquals(PreviewLength.THREE_LINES, list().previewLength)
            assertEquals(0, kept)
        }
    }

    @Test
    fun an_unreadable_old_file_is_checked_again_next_time() = runTest {
        TempDir().use { dir ->
            // 같은 자리에 폴더가 있어 읽으면 실패
            dir.fs.createDirectories(dir.root / "shared_prefs/pref.xml")

            assertFalse(dir.run(null))
            assertFalse(list().legacyChecked)
        }
    }

    private class FirstReadFails(private val inner: DataStore<Preferences>) : DataStore<Preferences> {
        private var failed = false
        override val data: Flow<Preferences> = flow {
            if (!failed) {
                failed = true
                throw IOException("읽기 실패")
            }
            emitAll(inner.data)
        }

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences) = inner.updateData(transform)
    }
}
