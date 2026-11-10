package org.sjhstudio.diary.core.migration

import kotlinx.datetime.LocalDate
import org.sjhstudio.diary.core.model.MigrationFailure

/**
 * 문의 메일 본문에 넣는 값(contracts/migration-flow.md "문의하기").
 * 단계·종류·시도 횟수와 개수 구간뿐.
 * 일기 내용, 경로, 예외 메시지는 없음(헌법 II).
 */
data class SupportSummary(
    val failure: MigrationFailure?,
    val attempt: Int,
    val entriesBucket: String,
    val photosBucket: String,
    val unreadableBucket: String
)

/**
 * 원본 내보내기 파일 이름에 넣는 날짜(yyyyMMdd).
 * 이름 자체는 화면 언어의 문자열 리소스가 정함.
 */
fun exportDateStamp(date: LocalDate): String =
    "${date.year}${date.month.ordinal.plus(1).toString().padStart(2, '0')}${date.day.toString().padStart(2, '0')}"
