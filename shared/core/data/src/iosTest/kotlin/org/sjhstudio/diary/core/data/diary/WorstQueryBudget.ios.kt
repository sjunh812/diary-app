package org.sjhstudio.diary.core.data.diary

// iOS 시험은 최적화하지 않은 빌드라 JVM보다 스무 배쯤 느리고, CI 가상 머신에서는 몇 배 더 느리다.
// 여기서는 끝나는지만 보고, 빠르기 기준은 Android 시험과 실제 기기에서 본다.
// 1초로 두면 코어 세 개짜리 CI 러너에서 코드와 상관없이 가끔 실패했다(2026-11-04, 11-05).
internal actual val QUERY_BUDGET_MILLIS: Long = 10_000L

internal actual val WORST_QUERY_BUDGET_MILLIS: Long = 30_000L
