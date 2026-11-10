package org.sjhstudio.diary.core.migration

import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import org.sjhstudio.diary.core.model.MigratedSetting
import org.sjhstudio.diary.core.model.MigrationFailure
import org.sjhstudio.diary.core.model.MigrationFailureKind
import org.sjhstudio.diary.core.model.MigrationOutcome
import org.sjhstudio.diary.core.model.MigrationReport
import org.sjhstudio.diary.core.model.MigrationState
import org.sjhstudio.diary.core.model.MigrationStep

/**
 * `migration/state.json`, `report.json`을 임시 파일 + 이름 바꾸기로 씀(research R7).
 * Room 밖 작은 파일이라 DB가 없을 때도 읽힘.
 * 일기 내용, 경로, 예외 메시지는 담지 않음.
 */
class MigrationStateStore(private val fileSystem: FileSystem, private val dir: Path) {
    private val statePath get() = dir / "state.json"
    private val reportPath get() = dir / "report.json"

    fun readState(): MigrationState = read(statePath, StateJson.serializer())?.let { s ->
        MigrationState(
            version = s.version,
            step = MigrationStep.fromId(s.step) ?: MigrationStep.NOT_STARTED,
            attempt = s.attempt,
            retriedVersionCode = s.retriedVersionCode,
            photoRecheckAt = s.photoRecheckAt?.let(Instant::fromEpochMilliseconds),
            startedAt = s.startedAt?.let(Instant::fromEpochMilliseconds),
            updatedAt = s.updatedAt?.let(Instant::fromEpochMilliseconds),
            failure =
                s.failure?.let { f ->
                    MigrationFailure(
                        MigrationStep.fromId(f.step) ?: MigrationStep.NOT_STARTED,
                        runCatching { MigrationFailureKind.valueOf(f.kind) }.getOrDefault(MigrationFailureKind.UNKNOWN)
                    )
                }
        )
    } ?: MigrationState()

    fun writeState(state: MigrationState) = write(
        statePath,
        StateJson.serializer(),
        StateJson(
            version = state.version,
            step = state.step.id,
            attempt = state.attempt,
            retriedVersionCode = state.retriedVersionCode,
            photoRecheckAt = state.photoRecheckAt?.toEpochMilliseconds(),
            startedAt = state.startedAt?.toEpochMilliseconds(),
            updatedAt = state.updatedAt?.toEpochMilliseconds(),
            failure = state.failure?.let { FailureJson(it.step.id, it.kind.name) }
        )
    )

    fun readReport(): MigrationReport? = read(reportPath, ReportJson.serializer())?.let { r ->
        MigrationReport(
            entriesTotal = r.entriesTotal,
            entriesMigrated = r.entriesMigrated,
            entriesUnreadable = r.entriesUnreadable,
            photosFound = r.photosFound,
            photosMissing = r.photosMissing,
            dateDiffers = r.dateDiffers,
            dateUnparsed = r.dateUnparsed,
            thumbnailsFailed = r.thumbnailsFailed,
            settingsMigrated = r.settingsMigrated.mapNotNull { runCatching { MigratedSetting.valueOf(it) }.getOrNull() },
            finishedAt = Instant.fromEpochMilliseconds(r.finishedAt),
            outcome = if (r.outcome == "Partial") MigrationOutcome.PARTIAL else MigrationOutcome.DONE
        )
    }

    fun writeReport(report: MigrationReport) = write(
        reportPath,
        ReportJson.serializer(),
        ReportJson(
            entriesTotal = report.entriesTotal,
            entriesMigrated = report.entriesMigrated,
            entriesUnreadable = report.entriesUnreadable,
            photosFound = report.photosFound,
            photosMissing = report.photosMissing,
            dateDiffers = report.dateDiffers,
            dateUnparsed = report.dateUnparsed,
            thumbnailsFailed = report.thumbnailsFailed,
            settingsMigrated = report.settingsMigrated.map { it.name },
            finishedAt = report.finishedAt.toEpochMilliseconds(),
            outcome = if (report.outcome == MigrationOutcome.PARTIAL) "Partial" else "Done"
        )
    )

    private fun <T> read(path: Path, serializer: kotlinx.serialization.KSerializer<T>): T? = if (!fileSystem.exists(path)) {
        null
    } else {
        runCatching {
            JSON.decodeFromString(serializer, fileSystem.read(path) { readUtf8() })
        }.getOrNull()
    }

    private fun <T> write(path: Path, serializer: kotlinx.serialization.KSerializer<T>, value: T) {
        fileSystem.createDirectories(dir)
        val temp = dir / "${path.name}.tmp"
        fileSystem.write(temp) { writeUtf8(JSON.encodeToString(serializer, value)) }
        fileSystem.atomicMove(temp, path)
    }

    @Serializable
    private data class StateJson(
        val version: Int,
        val step: String,
        val attempt: Int,
        val retriedVersionCode: Int = 0,
        val photoRecheckAt: Long? = null,
        val startedAt: Long? = null,
        val updatedAt: Long? = null,
        val failure: FailureJson? = null
    )

    @Serializable
    private data class FailureJson(val step: String, val kind: String)

    @Serializable
    private data class ReportJson(
        val entriesTotal: Int,
        val entriesMigrated: Int,
        val entriesUnreadable: Int,
        val photosFound: Int,
        val photosMissing: Int,
        val dateDiffers: Int,
        val dateUnparsed: Int,
        val thumbnailsFailed: Int,
        val settingsMigrated: List<String>,
        val finishedAt: Long,
        val outcome: String
    )

    private companion object {
        val JSON = Json { ignoreUnknownKeys = true }
    }
}
