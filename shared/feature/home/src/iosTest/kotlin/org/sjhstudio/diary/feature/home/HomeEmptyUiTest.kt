package org.sjhstudio.diary.feature.home

import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.EntryRow
import org.sjhstudio.diary.core.model.ListStyle
import org.sjhstudio.diary.feature.home.resources.Res
import org.sjhstudio.diary.feature.home.resources.home_empty_action
import org.sjhstudio.diary.feature.home.resources.home_empty_title
import org.sjhstudio.diary.feature.home.resources.home_error_retry
import org.sjhstudio.diary.feature.home.resources.home_favorites_empty

// 첫 실행과 즐겨찾기 없음, 읽기 실패에서 사용자가 무엇을 하면 되는지 보이는지 본다.
@OptIn(ExperimentalTestApi::class)
class HomeEmptyUiTest {
    private val done = LoadState.NotLoading(endOfPaginationReached = true)
    private val loaded = LoadStates(refresh = done, prepend = done, append = done)
    private val failed = LoadStates(refresh = LoadState.Error(IllegalStateException()), prepend = done, append = done)

    private class Labels {
        var title = ""
        var action = ""
        var favorites = ""
        var retry = ""
    }

    private fun ComposeUiTest.home(data: PagingData<EntryRow>, favoritesOnly: Boolean = false, onWrite: () -> Unit = {}): Labels {
        val labels = Labels()
        setContent {
            labels.title = stringResource(Res.string.home_empty_title)
            labels.action = stringResource(Res.string.home_empty_action)
            labels.favorites = stringResource(Res.string.home_favorites_empty)
            labels.retry = stringResource(Res.string.home_error_retry)
            DiaryTheme(darkTheme = false) {
                HomeScreen(
                    title = "2026년 10월",
                    rows = flowOf(data).collectAsLazyPagingItems(),
                    list = rememberLazyListState(),
                    grid = rememberLazyGridState(),
                    style = ListStyle.FEED,
                    favoritesOnly = favoritesOnly,
                    selectedId = null,
                    today = LocalDate(2026, 10, 31),
                    previewStyle = DiaryTheme.typography.body,
                    onToggleFavorites = {},
                    onPickMonth = {},
                    onOpen = {},
                    onMenu = {},
                    onWrite = onWrite,
                    onSearch = {}
                )
            }
        }
        waitForIdle()
        return labels
    }

    @Test
    fun first_run_shows_a_way_to_write() = runComposeUiTest {
        var writes = 0
        val labels = home(PagingData.empty(loaded), onWrite = { writes++ })

        onNode(hasText(labels.title)).assertExists()
        onNode(hasText(labels.action)).performClick()
        waitForIdle()

        assertEquals(1, writes)
    }

    @Test
    fun no_favorites_says_so_without_a_write_button() = runComposeUiTest {
        val labels = home(PagingData.empty(loaded), favoritesOnly = true)

        onNode(hasText(labels.favorites)).assertExists()
        onNode(hasText(labels.action)).assertDoesNotExist()
    }

    @Test
    fun a_failed_read_offers_a_retry_instead_of_saying_there_is_nothing() = runComposeUiTest {
        val labels = home(PagingData.empty(failed))

        onNode(hasText(labels.retry)).assertExists()
        onNode(hasText(labels.title)).assertDoesNotExist()
    }
}
