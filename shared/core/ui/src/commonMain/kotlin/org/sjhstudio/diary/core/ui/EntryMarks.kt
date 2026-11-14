package org.sjhstudio.diary.core.ui

import org.sjhstudio.diary.core.model.EntryRow

/** 목록 줄 앞에 달 머리글([newMonth])과 날짜 칸([newDay])을 그릴지 */
data class EntryMarks(val newMonth: Boolean, val newDay: Boolean)

/**
 * 홈과 검색 목록이 함께 쓰는 규칙
 * 앞 줄을 아직 읽지 못했으면 둘 다 그리지 않음. 그렸다가 앞 줄이 오면 지워져 목록이 들썩임
 */
fun entryMarks(index: Int, row: EntryRow, previous: EntryRow?): EntryMarks {
    if (index > 0 && previous == null) return EntryMarks(newMonth = false, newDay = false)
    val newMonth = previous == null || previous.date.year != row.date.year || previous.date.month != row.date.month
    return EntryMarks(newMonth = newMonth, newDay = previous == null || previous.date != row.date)
}
