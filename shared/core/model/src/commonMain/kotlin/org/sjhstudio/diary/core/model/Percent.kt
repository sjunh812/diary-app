package org.sjhstudio.diary.core.model

/**
 * 화면에 보이는 백분율(반올림).
 * 돌아보기와 고급 통계가 같은 셈을 써야 같은 기록이 다른 숫자로 보이지 않음.
 */
fun percentOf(count: Int, total: Int): Int = if (total <= 0) 0 else (count * PERCENT + total / 2) / total

private const val PERCENT = 100
