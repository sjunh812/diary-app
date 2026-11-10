package org.sjhstudio.diary.feature.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagingData
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDate
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.sjhstudio.diary.core.data.diary.EntryRepository
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.EntryRow
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.SearchFilter
import org.sjhstudio.diary.core.platform.FontInstallState
import org.sjhstudio.diary.core.platform.FontInstaller
import org.sjhstudio.diary.core.testing.FakeEntryRepository
import org.sjhstudio.diary.core.testing.FakeSettingsRepository
import org.sjhstudio.diary.core.testing.fakeEntryRow

private object NoFonts : FontInstaller {
    override fun installedPath(id: String): String? = null

    override fun install(id: String) = emptyFlow<FontInstallState>()
}

/** 조건이 바뀌면 맨 위로, 상세에 다녀왔을 때는 보던 자리로(FR-027) */
@OptIn(ExperimentalTestApi::class)
class SearchScrollUiTest {
    private val filter = SearchFilter(moods = setOf(Mood.GOOD))
    private lateinit var model: SearchViewModel

    /** 사진 조건을 켜면 앞 줄이 빠지고 끄면 돌아옴. 맨 위 줄이 달라짐 */
    private val entries =
        object : EntryRepository by FakeEntryRepository() {
            override fun search(filter: SearchFilter): Flow<PagingData<EntryRow>> {
                val range = if (filter.hasPhoto) FIRST_WITH_PHOTO until ROWS else 0 until ROWS
                return flowOf(PagingData.from(range.map { fakeEntryRow("e$it", DAY, label(it)) }))
            }
        }

    private fun label(index: Int) = "줄-$index-끝"

    @AfterTest
    fun tearDown() = stopKoin()

    private fun ComposeUiTest.search(shown: () -> Boolean = { true }) {
        startKoin {
            modules(
                module {
                    viewModel { (initial: SearchFilter) ->
                        SearchViewModel(entries, initial, FakeSettingsRepository(), NoFonts, saved = SavedStateHandle()).also { model = it }
                    }
                }
            )
        }
        setContent {
            val holder = rememberSaveableStateHolder()
            DiaryTheme(darkTheme = false) {
                Box(Modifier.size(400.dp, 800.dp)) {
                    if (shown()) holder.SaveableStateProvider("search") { SearchRoute(filter, null, {}, {}) }
                }
            }
        }
        waitShown(0)
    }

    private fun ComposeUiTest.waitShown(index: Int) = waitUntil(timeoutMillis = TIMEOUT) {
        onAllNodes(hasText(label(index), substring = true)).fetchSemanticsNodes().isNotEmpty()
    }

    private fun ComposeUiTest.scrollFar(lastIndex: Int = ROWS) {
        // 조건 칩 줄도 스크롤돼 세로 목록만 고름
        onNode(
            hasScrollToIndexAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)
        ).performScrollToIndex(lastIndex)
        waitShown(ROWS - 1)
    }

    @Test
    fun a_narrower_filter_goes_to_the_top() = runComposeUiTest {
        search()
        scrollFar()

        model.togglePhoto()

        waitShown(FIRST_WITH_PHOTO)
    }

    /** 첫 줄이 뒤로 밀려도 그 줄을 따라가지 않고 맨 위로 */
    @Test
    fun a_wider_filter_goes_to_the_top() = runComposeUiTest {
        search()
        model.togglePhoto()
        waitShown(FIRST_WITH_PHOTO)
        scrollFar(ROWS - FIRST_WITH_PHOTO)

        model.togglePhoto()

        waitShown(0)
    }

    @Test
    fun coming_back_from_a_detail_stays_where_you_were() = runComposeUiTest {
        var visible by mutableStateOf(true)
        search { visible }
        scrollFar()

        visible = false
        waitForIdle()
        visible = true
        waitForIdle()

        waitShown(ROWS - 1)
        assertEquals(0, onAllNodes(hasText(label(0), substring = true)).fetchSemanticsNodes().size)
    }

    private companion object {
        const val ROWS = 40
        const val FIRST_WITH_PHOTO = 10
        const val TIMEOUT = 5_000L
        val DAY = LocalDate(2026, 11, 1)
    }
}
