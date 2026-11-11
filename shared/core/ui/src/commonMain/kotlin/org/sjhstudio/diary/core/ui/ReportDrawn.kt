package org.sjhstudio.diary.core.ui

import androidx.compose.runtime.Composable

/**
 * 첫 화면이 사용자에게 쓸 만하게 그려졌다고 운영체제에 알림(Android `reportFullyDrawn`)
 * 시작 시간 측정(Macrobenchmark TTFD)과 Play 시작 지표가 이 시점을 씀. iOS에는 해당하는 것이 없음
 */
@Composable
expect fun ReportDrawnWhen(ready: () -> Boolean)
