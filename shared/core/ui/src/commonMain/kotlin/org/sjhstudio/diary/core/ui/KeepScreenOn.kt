package org.sjhstudio.diary.core.ui

import androidx.compose.runtime.Composable

/** 이 화면이 보이는 동안 화면이 꺼지지 않게 함(기록 이전 진행, research R7) */
@Composable
expect fun KeepScreenOn()
