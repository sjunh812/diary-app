package org.sjhstudio.diary.core.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate

// 켜 둔 채 자정을 넘겨도 앱이 돌아오면 오늘이 바뀐다.
@OptIn(ExperimentalTestApi::class)
class RememberTodayUiTest {
    private class Owner : LifecycleOwner {
        val registry = LifecycleRegistry.createUnsafe(this)
        override val lifecycle: Lifecycle get() = registry
    }

    @Test
    fun coming_back_after_midnight_reads_the_new_day() = runComposeUiTest {
        val owner = Owner()
        owner.registry.currentState = Lifecycle.State.RESUMED
        var clock = LocalDate(2026, 11, 7)
        var shown: LocalDate? = null
        setContent { CompositionLocalProvider(LocalLifecycleOwner provides owner) { shown = rememberToday { clock } } }
        waitForIdle()
        assertEquals(LocalDate(2026, 11, 7), shown)

        owner.registry.currentState = Lifecycle.State.CREATED
        clock = LocalDate(2026, 11, 8)
        owner.registry.currentState = Lifecycle.State.RESUMED
        waitForIdle()

        assertEquals(LocalDate(2026, 11, 8), shown)
    }
}
