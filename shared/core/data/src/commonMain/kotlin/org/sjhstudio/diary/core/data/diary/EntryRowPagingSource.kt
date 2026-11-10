package org.sjhstudio.diary.core.data.diary

import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.sjhstudio.diary.core.database.EntryListRow
import org.sjhstudio.diary.core.model.EntryRow

/**
 * 일기 쪽을 화면 줄 쪽으로 바꾸는 페이징 자리
 * `PagingData.map`은 줄마다 불려 묶어 읽을 수 없어 쪽 단위로 바꿈
 */
internal class EntryRowPagingSource(
    private val source: PagingSource<Int, EntryListRow>,
    private val toRows: suspend (List<EntryListRow>) -> List<EntryRow>
) : PagingSource<Int, EntryRow>() {
    init {
        source.registerInvalidatedCallback(::invalidate)
        registerInvalidatedCallback(source::invalidate)
    }

    override val jumpingSupported: Boolean get() = source.jumpingSupported

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, EntryRow> = when (val result = loadSafely(params)) {
        is LoadResult.Page ->
            // 페이징은 화면 스레드에서 돌아 미리보기 만드는 일은 밖으로 뺌
            LoadResult.Page(
                withContext(Dispatchers.Default) { toRows(result.data) },
                result.prevKey,
                result.nextKey,
                result.itemsBefore,
                result.itemsAfter
            )

        is LoadResult.Error -> LoadResult.Error(result.throwable)

        is LoadResult.Invalid -> LoadResult.Invalid()
    }

    // 화면 스레드에서 돌아 DB를 못 열면 앱이 꺼짐. 실패는 읽기 오류로 넘겨 화면이 다시 시도를 보임
    @Suppress("TooGenericExceptionCaught")
    private suspend fun loadSafely(params: LoadParams<Int>): LoadResult<Int, EntryListRow> = try {
        source.load(params)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        LoadResult.Error(e)
    }

    /** 안쪽 자리처럼 보던 곳이 가운데 오는 쪽부터 다시 읽음 */
    override fun getRefreshKey(state: PagingState<Int, EntryRow>): Int? =
        state.anchorPosition?.let { maxOf(0, it - state.config.initialLoadSize / 2) }
}
