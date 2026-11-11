package org.sjhstudio.diary.feature.backup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.sjhstudio.diary.core.data.RestoreRepository
import org.sjhstudio.diary.core.designsystem.component.DiaryToastState
import org.sjhstudio.diary.core.model.RestoreResult
import org.sjhstudio.diary.core.model.RestoreState
import org.sjhstudio.diary.core.testing.FakeRestoreRepository

@OptIn(ExperimentalTestApi::class)
class RestoreResultNoticeUiTest {
    private val restore = FakeRestoreRepository()

    @BeforeTest
    fun setUp() {
        startKoin {
            modules(
                module {
                    single<RestoreRepository> { restore }
                    single { RestoreResultWatch() }
                }
            )
        }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    /** 화면을 다시 만들어도(백 뒤 다시 열기, 회전) 이미 알린 결과를 또 알리지 않음 */
    @Test
    fun a_result_told_once_is_not_told_again_when_the_frame_is_drawn_again() = runComposeUiTest {
        val toast = DiaryToastState()
        var drawn by mutableStateOf(true)
        setContent { if (drawn) RestoreResultNotice(toast, onResultScreen = false, quiet = false) {} }

        restore.state.value = RestoreState.Done(RestoreResult(1, 0, 0, 0, 0, 0))
        // 문구를 읽는 동안 늦게 뜸
        waitUntil(timeoutMillis = 5_000) { toast.current != null }

        drawn = false
        waitForIdle()
        drawn = true
        waitForIdle()

        val toldAgain = runCatching { waitUntil(timeoutMillis = 2_000) { toast.current != null } }.isSuccess
        assertFalse(toldAgain, "다시 그려도 또 알리지 않음")
    }
}
