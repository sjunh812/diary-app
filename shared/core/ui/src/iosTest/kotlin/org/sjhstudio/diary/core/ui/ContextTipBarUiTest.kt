package org.sjhstudio.diary.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.sjhstudio.diary.core.data.ContextTips
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.ContextTip
import org.sjhstudio.diary.core.ui.resources.Res
import org.sjhstudio.diary.core.ui.resources.tip_close

// 탭을 오가 다시 그려도 팁 자리가 첫 프레임부터 같음
// 밖에서 끝내도 떠날 때까지 남음
@OptIn(ExperimentalTestApi::class)
class ContextTipBarUiTest {
    private class FakeTips(var open: Set<ContextTip>?) : ContextTips {
        /** 읽기가 끝나지 않으면 화면이 기다리는지 봄 */
        val loaded = CompletableDeferred<Unit>()
        val done = mutableListOf<ContextTip>()

        override fun isOpen(tip: ContextTip): Boolean? = open?.contains(tip)

        override suspend fun load() {
            loaded.await()
            if (open == null) open = ContextTip.entries.toSet()
        }

        override fun done(tip: ContextTip) {
            done += tip
            open = open?.minus(tip)
        }
    }

    private var tips = FakeTips(ContextTip.entries.toSet())
    private val text = "기분을 누르면 그 기분의 일기를 모아볼 수 있어요."

    @BeforeTest
    fun setUp() {
        startKoin { modules(module { single<ContextTips> { tips } }) }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun an_open_tip_shows_without_waiting_for_a_read() = runComposeUiTest {
        // 읽기를 끝내지 않아도 이미 쥔 값으로 바로 그림
        setContent { DiaryTheme(darkTheme = false) { ContextTipBar(ContextTip.INSIGHT_OPEN, text) } }
        onNodeWithText(text).assertExists()
    }

    @Test
    fun the_content_below_a_tip_never_moves_when_drawn_again() = runComposeUiTest {
        // 팁 아래 내용의 위치를 그릴 때마다 모음
        // 첫 프레임에 팁이 없으면 다른 값이 섞임
        val ys = mutableListOf<Float>()
        var drawn by mutableStateOf(true)
        setContent {
            DiaryTheme(darkTheme = false) {
                if (drawn) {
                    Column {
                        ContextTipBar(ContextTip.INSIGHT_OPEN, text)
                        Box(Modifier.size(1.dp).onGloballyPositioned { ys += it.positionInRoot().y })
                    }
                }
            }
        }
        waitForIdle()
        // 탭을 옮겼다 돌아옴
        drawn = false
        waitForIdle()
        drawn = true
        waitForIdle()
        assertEquals(1, ys.distinct().size, "위치: $ys")
        assertTrue(ys.first() > 0f, "팁 높이만큼 내려가 있음")
    }

    @Test
    fun a_tip_not_read_yet_shows_after_the_read() = runComposeUiTest {
        tips.open = null
        setContent { DiaryTheme(darkTheme = false) { ContextTipBar(ContextTip.INSIGHT_OPEN, text) } }
        onNodeWithText(text).assertDoesNotExist()
        tips.loaded.complete(Unit)
        waitUntil(timeoutMillis = 5_000) { onAllNodesText().isNotEmpty() }
    }

    @Test
    fun closing_hides_it_at_once_and_records_once() = runComposeUiTest {
        setContent { DiaryTheme(darkTheme = false) { ContextTipBar(ContextTip.INSIGHT_OPEN, text) } }
        val close = runBlocking { getString(Res.string.tip_close) }
        onNodeWithContentDescription(close).performClick()
        onNodeWithText(text).assertDoesNotExist()
        assertEquals(listOf(ContextTip.INSIGHT_OPEN), tips.done)
    }

    @Test
    fun finished_elsewhere_stays_until_the_screen_leaves() = runComposeUiTest {
        var drawn by mutableStateOf(true)
        setContent { DiaryTheme(darkTheme = false) { if (drawn) ContextTipBar(ContextTip.INSIGHT_OPEN, text) } }
        // 돌아보기에서 칸을 눌러 끝냄
        // 넘어가는 동안은 그대로
        tips.done(ContextTip.INSIGHT_OPEN)
        waitForIdle()
        onNodeWithText(text).assertExists()

        drawn = false
        waitForIdle()
        drawn = true
        waitForIdle()
        onNodeWithText(text).assertDoesNotExist()
    }

    private fun ComposeUiTest.onAllNodesText() = onAllNodes(hasText(text)).fetchSemanticsNodes()
}
