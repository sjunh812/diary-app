package org.sjhstudio.diary.core.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.sjhstudio.diary.core.model.MigrationStep

class TelemetryTest {
    private class RecordingSink : TelemetrySink {
        val events = mutableListOf<Pair<String, Map<String, String>>>()
        val keys = mutableMapOf<String, String>()

        override fun log(event: String, params: Map<String, String>) {
            events += event to params
        }

        override fun setCustomKey(key: String, value: String) {
            keys[key] = value
        }
    }

    @Test
    fun events_in_the_table_are_sent() {
        val sink = RecordingSink()
        val telemetry = Telemetry(sink, strict = true)

        telemetry.log(AnalyticsEvents.SCREEN_VIEW, mapOf("screen" to "SettingsKey"))
        telemetry.log(AnalyticsEvents.SETTINGS_CHANGED, mapOf("name" to "font", "value" to "kkomi"))
        telemetry.log(AnalyticsEvents.MIGRATION_FINISHED, mapOf("outcome" to "partial", "unreadable_bucket" to "1-9"))

        assertEquals(3, sink.events.size)
    }

    @Test
    fun unknown_event_key_or_value_throws_in_development_builds() {
        val telemetry = Telemetry(RecordingSink(), strict = true)

        assertFailsWith<IllegalArgumentException> { telemetry.log("entry_saved", mapOf("text" to "오늘 한강")) }
        assertFailsWith<IllegalArgumentException> { telemetry.log(AnalyticsEvents.SCREEN_VIEW, mapOf("place" to "서울")) }
        assertFailsWith<IllegalArgumentException> { telemetry.log(AnalyticsEvents.LOCK_RESULT, mapOf("result" to "1234")) }
        // 다른 설정의 값을 섞어 보내도 막는다.
        assertFailsWith<IllegalArgumentException> {
            telemetry.log(AnalyticsEvents.SETTINGS_CHANGED, mapOf("name" to "theme", "value" to "kkomi"))
        }
    }

    @Test
    fun violation_message_never_contains_the_value() {
        val error =
            assertFailsWith<IllegalArgumentException> {
                Telemetry(RecordingSink(), strict = true).log(AnalyticsEvents.SCREEN_VIEW, mapOf("screen" to "오늘 한강에 갔다"))
            }
        assertTrue("오늘 한강" !in error.message.orEmpty())
    }

    @Test
    fun unknown_events_are_dropped_silently_in_release_builds() {
        val sink = RecordingSink()
        val telemetry = Telemetry(sink, strict = false)

        telemetry.log("entry_saved", mapOf("text" to "오늘 한강"))
        telemetry.log(AnalyticsEvents.LOCK_RESULT, mapOf("result" to "ok"))

        assertEquals(listOf(AnalyticsEvents.LOCK_RESULT), sink.events.map { it.first })
    }

    @Test
    fun migration_step_is_the_only_custom_key() {
        val sink = RecordingSink()
        Telemetry(sink, strict = true).setMigrationStep(MigrationStep.COPYING)
        assertEquals(mapOf("migration_step" to "Copying"), sink.keys)
    }

    @Test
    fun buckets_follow_the_contract() {
        assertEquals(
            listOf("0", "1-9", "10-99", "100-999", "1000-9999", "10000+"),
            listOf(0, 9, 10, 999, 1000, 10_000).map(AnalyticsEvents::countBucket)
        )
        assertEquals(
            listOf("<10s", "<30s", "<2m", "<10m", "10m+"),
            listOf(9.seconds, 29.seconds, 1.minutes, 9.minutes, 10.minutes).map(AnalyticsEvents::durationBucket)
        )
    }
}
