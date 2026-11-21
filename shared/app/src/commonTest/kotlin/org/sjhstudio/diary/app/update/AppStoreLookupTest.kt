package org.sjhstudio.diary.app.update

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlinx.serialization.SerializationException

class AppStoreLookupTest {
    @Test
    fun reads_the_version_and_ignores_other_fields() {
        val body = """{"resultCount":1,"results":[{"trackId":6835116731,"version":"2.0.2","releaseNotes":"고친 점"}]}"""
        assertEquals("2.0.2", AppStoreLookup.version(body))
    }

    @Test
    fun no_result_in_that_country_is_no_version() {
        assertNull(AppStoreLookup.version("""{"resultCount":0,"results":[]}"""))
        assertNull(AppStoreLookup.version("""{"resultCount":1,"results":[{"version":" "}]}"""))
    }

    @Test
    fun broken_body_fails_so_the_last_known_value_is_kept() {
        assertFailsWith<SerializationException> { AppStoreLookup.version("<html>") }
    }
}
