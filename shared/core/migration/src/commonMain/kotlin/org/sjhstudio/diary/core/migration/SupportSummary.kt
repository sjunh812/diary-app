package org.sjhstudio.diary.core.migration

import kotlinx.datetime.LocalDate
import org.sjhstudio.diary.core.model.MigrationFailure

/**
 * 문의 메일 본문에 넣는 값(contracts/migration-flow.md "문의하기"). 단계·종류·시도 횟수와 개수 구간뿐이다.
 * 일기 내용, 경로, 예외 메시지는 없다(헌법 II).
 */
data class SupportSummary(
    val failure: MigrationFailure?,
    val attempt: Int,
    val entriesBucket: String,
    val photosBucket: String,
    val unreadableBucket: String
)

/** `1일1일기-원본-yyyyMMdd.zip` */
fun originalExportFileName(date: LocalDate): String =
    "1일1일기-원본-${date.year}${date.month.ordinal.plus(1).toString().padStart(2, '0')}${date.day.toString().padStart(2, '0')}.zip"
