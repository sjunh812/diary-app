package org.sjhstudio.diary.core.migration

import kotlin.time.Duration.Companion.milliseconds
import org.sjhstudio.diary.core.model.MigrationFailureKind
import org.sjhstudio.diary.core.model.MigrationOutcome
import org.sjhstudio.diary.core.model.MigrationReport
import org.sjhstudio.diary.core.model.MigrationStep
import org.sjhstudio.diary.core.platform.AnalyticsEvents
import org.sjhstudio.diary.core.platform.Telemetry

/**
 * 이전 사건을 사용 통계로 보낸다(FR-013, contracts/migration-flow.md "보내도 되는 값").
 * 개수는 구간으로, 실패는 단계와 종류만 보낸다.
 * 예외 메시지, 경로, 글, 기분은 보내지 않는다.
 * 비정상 종료 보고에는 지금 단계(`migration_step`)만 남긴다.
 */
class MigrationTelemetry(private val telemetry: Telemetry) : MigrationEvents {
    override fun started(entries: Int, photos: Int, attempt: Int) {
        telemetry.setMigrationStep(MigrationStep.COPYING)
        telemetry.log(
            AnalyticsEvents.MIGRATION_STARTED,
            mapOf(
                "entries_bucket" to AnalyticsEvents.countBucket(entries),
                "photos_bucket" to AnalyticsEvents.countBucket(photos),
                "attempt" to attempt.coerceIn(1, 99).toString()
            )
        )
    }

    override fun finished(report: MigrationReport, durationMillis: Long) {
        val step = if (report.outcome == MigrationOutcome.PARTIAL) MigrationStep.PARTIAL else MigrationStep.DONE
        telemetry.setMigrationStep(step)
        telemetry.log(
            AnalyticsEvents.MIGRATION_FINISHED,
            mapOf(
                "outcome" to if (report.outcome == MigrationOutcome.PARTIAL) "partial" else "done",
                "duration_bucket" to AnalyticsEvents.durationBucket(durationMillis.milliseconds),
                "missing_photos_bucket" to AnalyticsEvents.countBucket(report.photosMissing),
                "unreadable_bucket" to AnalyticsEvents.countBucket(report.entriesUnreadable)
            )
        )
    }

    override fun failed(step: MigrationStep, kind: MigrationFailureKind, attempt: Int) {
        telemetry.setMigrationStep(MigrationStep.FAILED)
        telemetry.log(
            AnalyticsEvents.MIGRATION_FAILED,
            mapOf("step" to step.id, "kind" to kind.name, "attempt" to attempt.coerceIn(1, 99).toString())
        )
    }
}
