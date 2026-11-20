package org.sjhstudio.diary.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDate
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.EntryRow
import org.sjhstudio.diary.core.model.ListStyle

// 목록 위 카드·팁이 있으면 뼈대와 빈 안내는 그 아래부터, 없으면 목록 자리 맨 위부터
@OptIn(ExperimentalTestApi::class)
class HomeListTopUiTest {
    private val reading: Flow<PagingData<EntryRow>> = emptyFlow()
    private val done = LoadState.NotLoading(endOfPaginationReached = true)
    private val empty: Flow<PagingData<EntryRow>> = flowOf(PagingData.empty(LoadStates(refresh = done, prepend = done, append = done)))

    private fun ComposeUiTest.home(rows: Flow<PagingData<EntryRow>>, style: ListStyle, top: () -> Dp, favoritesOnly: Boolean) {
        setContent {
            DiaryTheme(darkTheme = false) {
                Box(Modifier.size(400.dp, 800.dp)) {
                    HomeScreen(
                        title = "2026년 11월",
                        rows = rows.collectAsLazyPagingItems(),
                        list = rememberLazyListState(),
                        grid = rememberLazyGridState(),
                        style = style,
                        favoritesOnly = favoritesOnly,
                        selectedId = null,
                        today = LocalDate(2026, 11, 20),
                        previewStyle = DiaryTheme.typography.body,
                        onToggleFavorites = {},
                        onPickMonth = {},
                        onOpen = {},
                        onMenu = {},
                        onWrite = {},
                        onSearch = {},
                        listTop = {
                            // 카드와 팁을 함께 띄운 높이를 흉내 냄. 0이면 카드 없음
                            Column(Modifier.testTag(TOP)) { if (top() > 0.dp) Box(Modifier.fillMaxWidth().height(top())) }
                        }
                    )
                }
            }
        }
        waitForIdle()
    }

    private fun ComposeUiTest.bounds(tag: String): DpRect = try {
        onNodeWithTag(tag).getBoundsInRoot()
    } catch (e: AssertionError) {
        throw AssertionError("$tag 없음\n" + onRoot().printToString(), e)
    }

    private fun ComposeUiTest.check(rows: Flow<PagingData<EntryRow>>, tag: String, style: ListStyle, favoritesOnly: Boolean = false) {
        var height by mutableStateOf(0.dp)
        home(rows, style, { height }, favoritesOnly)
        val tops = listOf(0.dp, 140.dp, 260.dp).map { top ->
            height = top
            waitForIdle()
            val below = bounds(tag)
            // 카드(와 팁) 자리 아래에서 시작하고 겹치지 않음. 높이 0인 자리는 위치가 잡히지 않아 카드가 있을 때만 견줌
            if (top > 0.dp) {
                val card = bounds(TOP)
                assertTrue(below.top >= card.bottom, "$style 카드 $top: ${below.top} < ${card.bottom}")
            }
            below.top
        }
        // 카드가 없으면 목록 자리 맨 위부터, 있으면 그 높이만큼 내려감
        assertEquals(tops[0] + 140.dp, tops[1], "$style 카드 140")
        assertEquals(tops[0] + 260.dp, tops[2], "$style 카드 260")
    }

    @Test
    fun feed_skeleton_sits_below_the_top() = runComposeUiTest { check(reading, HOME_SKELETON_TAG, ListStyle.FEED) }

    @Test
    fun card_skeleton_sits_below_the_top() = runComposeUiTest { check(reading, HOME_SKELETON_TAG, ListStyle.CARD) }

    @Test
    fun grid_skeleton_sits_below_the_top() = runComposeUiTest { check(reading, HOME_SKELETON_TAG, ListStyle.PHOTO_GRID) }

    @Test
    fun empty_home_sits_below_the_top() = runComposeUiTest { check(empty, HOME_EMPTY_TAG, ListStyle.FEED) }

    @Test
    fun empty_favorites_sit_below_the_top() = runComposeUiTest { check(empty, HOME_EMPTY_TAG, ListStyle.FEED, favoritesOnly = true) }

    /** 빈 홈에서 첫 일기를 쓰고 돌아와 다시 읽는 동안 빈 안내가 남고 뼈대가 스치지 않음 */
    @Test
    fun rereading_an_empty_home_keeps_the_empty_notice() = runComposeUiTest {
        val data = MutableStateFlow(PagingData.empty<EntryRow>(LoadStates(refresh = done, prepend = done, append = done)))
        home(data, ListStyle.FEED, { 0.dp }, favoritesOnly = false)
        onNodeWithTag(HOME_EMPTY_TAG).assertExists()

        data.value = PagingData.empty(LoadStates(refresh = LoadState.Loading, prepend = done, append = done))
        waitForIdle()

        onNodeWithTag(HOME_EMPTY_TAG).assertExists()
        onNodeWithTag(HOME_SKELETON_TAG).assertDoesNotExist()
    }

    private companion object {
        const val TOP = "list-top-slot"
    }
}
