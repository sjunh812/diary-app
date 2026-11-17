package org.sjhstudio.diary.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.delay
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import org.sjhstudio.diary.core.model.currentDate

/**
 * 오늘 날짜
 * 앱이 돌아올 때, 켜 둔 채 자정을 넘길 때 다시 읽음
 * `remember`로 잡아 두면 어제가 오늘로 남음
 */
@Composable
fun rememberToday(waitForTomorrow: suspend (LocalDate) -> Unit = ::waitUntilNextDay, read: () -> LocalDate = ::currentDate): LocalDate {
    var today by remember { mutableStateOf(read()) }
    val latestRead by rememberUpdatedState(read)
    val latestWait by rememberUpdatedState(waitForTomorrow)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                val shown = latestRead()
                today = shown
                latestWait(shown)
            }
        }
    }
    return today
}

private suspend fun waitUntilNextDay(shown: LocalDate) {
    delay(untilNextDay(shown, Clock.System.now(), TimeZone.currentSystemDefault()))
}

/**
 * 보인 날짜의 다음 자정까지. 이미 지났으면 0이라 바로 다시 읽음
 * 아직이면 시계가 조금 일러도 어제를 다시 읽지 않게 1초 더
 */
internal fun untilNextDay(shown: LocalDate, now: Instant, zone: TimeZone): Duration {
    val midnight = shown.plus(DatePeriod(days = 1)).atStartOfDayIn(zone)
    return if (now >= midnight) Duration.ZERO else midnight - now + 1.seconds
}
