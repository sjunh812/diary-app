package org.sjhstudio.diary.core.migration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// 옛 설정 XML 읽기와 비밀번호 줄 빼기(스펙 001 T036).
class LegacyPrefsReaderTest {
    private val pref =
        """
        <?xml version='1.0' encoding='utf-8' standalone='yes' ?>
        <map>
            <boolean name="ask_location" value="false" />
            <int name="font_key" value="5" />
            <int name="mode_key" value="2" />
            <string name="password">1234</string>
            <boolean name="use_pw" value="true" />
            <boolean name="diary&amp;me@gmail.com" value="true" />
            <string name="diary&amp;me@gmail.com's id">1AbC_dEf</string>
            <string name="memo">&lt;꺾쇠&gt; &quot;따옴표&quot;</string>
            <set name="tags">
                <string>하나</string>
                <string>둘</string>
            </set>
        </map>
        """.trimIndent()

    @Test
    fun reads_every_value_type() {
        val values = LegacyPrefs.parse(pref)
        assertEquals(false, values["ask_location"])
        assertEquals(5, values["font_key"])
        assertEquals("1234", values["password"])
        assertEquals(true, values["diary&me@gmail.com"])
        assertEquals("1AbC_dEf", values["diary&me@gmail.com's id"])
        assertEquals("<꺾쇠> \"따옴표\"", values["memo"])
        assertEquals(setOf("하나", "둘"), values["tags"])
    }

    @Test
    fun copy_drops_only_the_password_line() {
        val copy = LegacyPrefs.withoutPassword(pref)
        assertFalse("password" in copy)
        assertFalse("1234" in copy)
        assertEquals(pref.lines().filterNot { "password" in it }, copy.lines())
        assertTrue(LegacyPrefs.parse(copy)["use_pw"] == true)
    }

    @Test
    fun pref2_alarm_values() {
        val pref2 =
            """
            <map>
                <boolean name="is_alarm_key" value="true" />
                <int name="hour_key" value="21" />
                <int name="minute_key" value="5" />
            </map>
            """.trimIndent()
        assertEquals(mapOf("is_alarm_key" to true, "hour_key" to 21, "minute_key" to 5), LegacyPrefs.parse(pref2))
    }
}
