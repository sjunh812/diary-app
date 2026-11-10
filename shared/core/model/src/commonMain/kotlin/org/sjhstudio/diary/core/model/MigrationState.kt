package org.sjhstudio.diary.core.model

import kotlin.time.Instant

/**
 * 기록 이전이 어디까지 왔는지(`migration/state.json`)
 * 일기 내용, 경로, 예외 메시지는 담지 않음(헌법 II)
 * 단계와 다시 시작 규칙은 contracts/migration-flow.md.
 */
data class MigrationState(
    val version: Int = VERSION,
    val step: MigrationStep = MigrationStep.NOT_STARTED,
    /** 몇 번째 시도인지 */
    val attempt: Int = 0,
    /** 처음 옮겼거나 `Partial`을 마지막으로 다시 읽은 앱 버전. 없으면 0. 같은 버전에서는 다시 읽지 않음 */
    val retriedVersionCode: Int = 0,
    /** 찾을 수 없는 사진을 마지막으로 다시 본 시각 */
    val photoRecheckAt: Instant? = null,
    val startedAt: Instant? = null,
    val updatedAt: Instant? = null,
    val failure: MigrationFailure? = null
) {
    companion object {
        const val VERSION = 1
    }
}

enum class MigrationStep(val id: String) {
    NOT_STARTED("NotStarted"),
    SNAPSHOTTING("Snapshotting"),
    COPYING("Copying"),
    VERIFYING("Verifying"),
    COMMITTING("Committing"),
    DONE("Done"),
    PARTIAL("Partial"),
    FAILED("Failed"),

    /** 실패했지만 사용자가 새 일기부터 쓰기로 함. 앱은 쓸 수 있고 예전 일기는 나중에 다시 옮김 */
    DEFERRED("Deferred")
    ;

    companion object {
        fun fromId(id: String?): MigrationStep? = entries.firstOrNull { it.id == id }
    }
}

data class MigrationFailure(val step: MigrationStep, val kind: MigrationFailureKind)

enum class MigrationFailureKind {
    NO_SPACE,
    SNAPSHOT_IO,
    READ_ERROR,
    WRITE_ERROR,
    MISMATCH,
    UNKNOWN
}
