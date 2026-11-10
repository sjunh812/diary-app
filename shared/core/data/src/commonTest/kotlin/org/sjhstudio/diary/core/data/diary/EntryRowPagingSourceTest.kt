package org.sjhstudio.diary.core.data.diary

import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlin.test.Test
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import org.sjhstudio.diary.core.database.EntryListRow

// DB를 못 열어도 홈 목록이 앱을 끄지 않고 읽기 오류로 끝나야 함
class EntryRowPagingSourceTest {
    private class Broken : PagingSource<Int, EntryListRow>() {
        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, EntryListRow> = error("no such table: draft")

        override fun getRefreshKey(state: PagingState<Int, EntryListRow>): Int? = null
    }

    @Test
    fun a_failing_database_becomes_a_load_error() = runTest {
        val source = EntryRowPagingSource(Broken()) { emptyList() }
        val result = source.load(PagingSource.LoadParams.Refresh(key = null, loadSize = 20, placeholdersEnabled = false))
        assertIs<PagingSource.LoadResult.Error<Int, *>>(result)
    }
}
