package org.sjhstudio.diary.core.ui

import androidx.compose.runtime.Composable

@Composable
actual fun ReportDrawnWhen(ready: () -> Boolean) = androidx.activity.compose.ReportDrawnWhen(ready)
