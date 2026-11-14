package org.sjhstudio.diary.core.model

import kotlin.time.Instant

/** 기록 이전 결과(`migration/report.json`). 개수만 담고 일기 내용과 경로는 담지 않음 */
data class MigrationReport(
    /** 레거시 줄 수(읽은 것 + 못 읽은 것 추정) */
    val entriesTotal: Int,
    val entriesMigrated: Int,
    /** 깨져서 못 읽은 줄 수. 1 이상이면 [MigrationOutcome.PARTIAL] */
    val entriesUnreadable: Int,
    val photosFound: Int,
    val photosMissing: Int,
    /** 앞 10글자 날짜와 옛 앱의 느슨한 해석 날짜가 다른 일기 수 */
    val dateDiffers: Int,
    /** 날짜를 읽지 못해 대체 규칙을 쓴 일기 수 */
    val dateUnparsed: Int,
    /** 썸네일을 못 만든 수. 이전 실패는 아님 */
    val thumbnailsFailed: Int,
    val settingsMigrated: List<MigratedSetting>,
    val finishedAt: Instant,
    val outcome: MigrationOutcome
)

enum class MigrationOutcome {
    DONE,
    PARTIAL
}

/** 옮긴 설정 여덟 가지 */
enum class MigratedSetting {
    FONT,
    BODY_TEXT_SIZE,
    THEME_MODE,
    ASK_LOCATION,
    LOCK,
    BIOMETRIC,
    REMINDER,
    REMINDER_TIME
}
