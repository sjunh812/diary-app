package org.sjhstudio.diary.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.datetime.LocalDate
import org.sjhstudio.diary.core.model.currentDate

/**
 * 오늘 날짜.
 * 앱이 돌아올 때마다 다시 읽는다.
 * `remember`로 잡아 두면 켜 둔 채 자정을 넘긴 뒤에도 어제가 오늘로 남는다.
 */
@Composable
fun rememberToday(read: () -> LocalDate = ::currentDate): LocalDate {
    var today by remember { mutableStateOf(read()) }
    val latestRead by rememberUpdatedState(read)
    LifecycleResumeEffect(Unit) {
        today = latestRead()
        onPauseOrDispose {}
    }
    return today
}
