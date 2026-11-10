package org.sjhstudio.diary.feature.search.preview

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.tooling.preview.Preview
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.EntryRow
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.SearchFilter
import org.sjhstudio.diary.core.model.Weather
import org.sjhstudio.diary.feature.search.SearchScreen

private val Done = LoadState.NotLoading(endOfPaginationReached = true)
private val Failed = LoadStates(refresh = LoadState.Error(IllegalStateException()), prepend = Done, append = Done)

private val Rows =
    listOf(
        EntryRow(
            "1",
            LocalDate(
                2026,
                10,
                30
            ),
            LocalTime(22, 15), "다음엔 바다 보러 가자고 약속했다.", Mood.LAUGH, Weather.SUNNY, "연남동", false, emptyList()
        ),
        EntryRow(
            "2",
            LocalDate(
                2026,
                9,
                30
            ),
            LocalTime(20, 30), "제주 마지막 날. 애월 바다 앞 카페에 앉아 있었다.", Mood.COOL, Weather.SUNNY, "제주 애월", true, emptyList()
        )
    )

@Composable
private fun Search(dark: Boolean, rows: List<EntryRow>?, query: String, failed: Boolean = false) = DiaryTheme(darkTheme = dark) {
    val data = if (failed) PagingData.empty(Failed) else PagingData.from(rows.orEmpty())
    SearchScreen(
        filter = SearchFilter(query = query),
        period = null,
        results = flowOf(data).collectAsLazyPagingItems(),
        selectedId = null,
        previewStyle = DiaryTheme.typography.body,
        list = rememberLazyListState(),
        focus = remember { FocusRequester() },
        onQuery = {},
        onBack = {},
        onOpen = {},
        onSheet = {},
        onPhoto = {},
        onFavorites = {},
        onClear = {}
    )
}

@Preview(name = "찾기 · 결과 · 라이트", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun ResultsLight() = Search(dark = false, rows = Rows, query = "바다")

@Preview(name = "찾기 · 결과 · 다크", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun ResultsDark() = Search(dark = true, rows = Rows, query = "바다")

@Preview(name = "찾기 · 결과 없음", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun NoResults() = Search(dark = false, rows = emptyList(), query = "바닷가")

@Preview(name = "찾기 · 입력 전", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun Idle() = Search(dark = false, rows = null, query = "")

@Preview(name = "찾기 · 읽기 실패", locale = "ko", widthDp = 390, heightDp = 844)
@Composable
private fun LoadFailed() = Search(dark = true, rows = null, query = "바다", failed = true)
