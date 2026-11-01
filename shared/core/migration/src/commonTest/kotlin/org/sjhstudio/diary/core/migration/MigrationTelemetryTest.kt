package org.sjhstudio.diary.core.migration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import org.sjhstudio.diary.core.model.MigrationFailureKind
import org.sjhstudio.diary.core.model.MigrationOutcome
import org.sjhstudio.diary.core.model.MigrationReport
import org.sjhstudio.diary.core.model.MigrationStep
import org.sjhstudio.diary.core.platform.Telemetry
import org.sjhstudio.diary.core.platform.TelemetrySink

// 고정 표를 엄격하게 보는 Telemetry로 보내 표 밖 값이 없음을 확인한다(FR-013).
class MigrationTelemetryTest {
    private val events = mutableListOf<Pair<String, Map<String, String>>>()
    private val keys = mutableMapOf<String, String>()
    private val sink =
        object : TelemetrySink {
            override fun log(event: String, params: Map<String, String>) {
                events += event to params
            }

            override fun setCustomKey(key: String, value: String) {
                keys[key] = value
            }
        }
    private val telemetry = MigrationTelemetry(Telemetry(sink, strict = true))

    @Test
    fun sends_only_buckets_and_enums() {
        telemetry.started(entries = 1_284, photos = 0, attempt = 1)
        telemetry.finished(
            MigrationReport(1_284, 1_281, 3, 1_965, 33, 2, 0, 0, emptyList(), Instant.fromEpochMilliseconds(0), MigrationOutcome.PARTIAL),
            durationMillis = 95_000
        )
        telemetry.failed(MigrationStep.COPYING, MigrationFailureKind.WRITE_ERROR, attempt = 2)

        assertEquals(mapOf("entries_bucket" to "1000-9999", "photos_bucket" to "0", "attempt" to "1"), events[0].second)
        assertEquals(
            mapOf("outcome" to "partial", "duration_bucket" to "<2m", "missing_photos_bucket" to "10-99", "unreadable_bucket" to "1-9"),
            events[1].second
        )
        assertEquals(mapOf("step" to "Copying", "kind" to "WRITE_ERROR", "attempt" to "2"), events[2].second)
        assertEquals(mapOf("migration_step" to "Failed"), keys)
    }
}
