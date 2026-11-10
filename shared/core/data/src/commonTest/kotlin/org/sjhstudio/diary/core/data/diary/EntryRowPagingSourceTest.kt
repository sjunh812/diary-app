package org.sjhstudio.diary.core.data.diary

import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import org.sjhstudio.diary.core.database.EntryListRow

// DB를 못 열어도 홈 목록이 앱을 끄거나 뼈대에 멈추지 않고 읽기 오류로 끝나야 함
class EntryRowPagingSourceTest {
    private class Broken : PagingSource<Int, EntryListRow>() {
        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, EntryListRow> = error("no such table: draft")

        override fun getRefreshKey(state: PagingState<Int, EntryListRow>): Int? = null
    }

    // DB 열기가 실패한 뒤 Room이 연결을 끝없이 기다리는 꼴
    private class Stuck : PagingSource<Int, EntryListRow>() {
        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, EntryListRow> = awaitCancellation()

        override fun getRefreshKey(state: PagingState<Int, EntryListRow>): Int? = null
    }

    private val first = PagingSource.LoadParams.Refresh<Int>(key = null, loadSize = 20, placeholdersEnabled = false)

    @Test
    fun a_failing_database_becomes_a_load_error() = runTest {
        assertIs<PagingSource.LoadResult.Error<Int, *>>(EntryRowPagingSource(Broken()) { emptyList() }.load(first))
    }

    @Test
    fun a_read_that_never_ends_becomes_a_load_error() = runTest {
        assertIs<PagingSource.LoadResult.Error<Int, *>>(EntryRowPagingSource(Stuck(), loadTimeout = 100.milliseconds) { emptyList() }.load(first))
    }
}
