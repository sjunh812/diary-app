package org.sjhstudio.diary.app.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// 알림을 눌러 들어온 길(스펙 001 FR-022).
// 앱 틀은 잠금과 이전 관문을 지난 뒤에만 그려져서, 요청은 그때까지 기다린다.
@OptIn(ExperimentalTestApi::class)
class AppLaunchUiTest {
    private fun navigation(vararg keys: NavKey) = TabNavigation(
        selected = mutableStateOf(HomeKey),
        stacks = TABS.associateWith { tab -> if (tab == HomeKey) mutableListOf(HomeKey, *keys) else mutableListOf<NavKey>(tab) }
    )

    @Test
    fun a_request_made_before_the_app_is_drawn_opens_write_once_it_is() = runComposeUiTest {
        val launch = AppLaunch()
        val nav = navigation()
        launch.requestWrite()

        setContent { OpenRequestedScreens(launch, nav) }
        waitForIdle()

        assertTrue((nav.top as? WriteKey)?.sameTarget(WriteKey()) == true)
        assertFalse(launch.writeRequested.value)
    }

    @Test
    fun a_request_made_while_the_app_is_open_opens_write() = runComposeUiTest {
        val launch = AppLaunch()
        val nav = navigation()
        setContent { OpenRequestedScreens(launch, nav) }
        waitForIdle()

        launch.requestWrite()
        waitForIdle()

        assertTrue((nav.top as? WriteKey)?.sameTarget(WriteKey()) == true)
    }

    /** 잠금을 풀고 앱 틀이 다시 그려져도 쓰기가 또 열리지 않는다. */
    @Test
    fun one_request_opens_write_only_once() = runComposeUiTest {
        val launch = AppLaunch()
        val nav = navigation()
        launch.requestWrite()
        setContent { OpenRequestedScreens(launch, nav) }
        waitForIdle()
        nav.back()

        waitForIdle()

        assertEquals(HomeKey, nav.top)
    }

    /** 고치던 글 위에 새 글이 열리면 어느 글을 쓰는지 헷갈린다. */
    @Test
    fun writing_stays_as_it_is() = runComposeUiTest {
        val launch = AppLaunch()
        val nav = navigation(WriteKey(entryId = "a"))
        launch.requestWrite()

        setContent { OpenRequestedScreens(launch, nav) }
        waitForIdle()

        assertTrue((nav.top as? WriteKey)?.sameTarget(WriteKey(entryId = "a")) == true)
        assertFalse(launch.writeRequested.value)
    }
}
