package org.sjhstudio.diary.feature.write

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.uikit.LocalUIViewController
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.Draft
import org.sjhstudio.diary.core.model.DraftKey
import org.sjhstudio.diary.core.testing.FakeDraftRepository
import platform.UIKit.UIViewController

// 화면을 돌리거나 다른 화면에 가렸다 돌아와도 편집기가 옛 글로 돌아가지 않는지(헌법 I)
// 컴포지션을 없앴다가 저장된 상태로 다시 만들어 돌리기를 흉내 냄
// 화면 모델은 돌리기에서 살아남으니 밖에 둠
@OptIn(ExperimentalTestApi::class)
class WriteRouteUiTest {
    private fun draft(key: DraftKey, body: String) =
        Draft(key, LocalDate(2026, 11, 1), LocalTime(9, 0), body, null, null, null, false, false, Clock.System.now())

    private fun ComposeUiTest.editorText(): String {
        val node = onAllNodes(hasSetTextAction())[0].fetchSemanticsNode()
        return node.config[SemanticsProperties.EditableText].text
    }

    /** 화면을 돌리는 흉내. 컴포지션을 없앴다가 저장해 둔 상태로 다시 만듦 */
    private fun ComposeUiTest.showWrite(viewModel: WriteViewModel, dark: () -> Boolean = { false }, shown: () -> Boolean) {
        setContent {
            CompositionLocalProvider(LocalUIViewController provides UIViewController()) {
                DiaryTheme(darkTheme = dark()) {
                    val holder = rememberSaveableStateHolder()
                    if (shown()) {
                        holder.SaveableStateProvider("write") {
                            WriteRoute(target = WriteTarget.Edit("e1"), onSaved = {}, onClosed = {}, injected = viewModel)
                        }
                    }
                }
            }
        }
        waitForIdle()
    }

    @Test
    fun coming_back_keeps_what_was_typed() = runComposeUiTest {
        val key = DraftKey.Edit("e1")
        val repository = FakeDraftRepository(editable = mapOf("e1" to draft(key, "<p>옛 글</p>")))
        val viewModel = testWriteViewModel(WriteTarget.Edit("e1"), repository, timeZone = TimeZone.UTC)
        var shown by mutableStateOf(true)
        showWrite(viewModel) { shown }
        assertEquals("옛 글", editorText())

        onAllNodes(hasSetTextAction())[0].performTextInput("새 글 ")
        waitForIdle()
        val typed = editorText()
        assertTrue("새 글" in typed, typed)

        shown = false
        waitForIdle()
        shown = true
        waitForIdle()

        assertEquals(typed, editorText())
        viewModel.onStop()
        waitForIdle()
        val stored = runBlocking { repository.draft(key).first() }?.bodyHtml.orEmpty()
        assertTrue("새 글" in stored, stored)
    }

    /**
     * 라이트로 바뀌어 돌아와도 친 글과 글자색이 남음
     * 편집기 안의 다크 색은 라이트에서 저장 값으로 되돌릴 수 없음
     */
    @Test
    fun a_new_theme_keeps_the_text_and_its_colors() = runComposeUiTest {
        val key = DraftKey.Edit("e1")
        val body = """<p><span style="color:#b91c1c">빨강</span></p>"""
        val repository = FakeDraftRepository(editable = mapOf("e1" to draft(key, body)))
        val viewModel = testWriteViewModel(WriteTarget.Edit("e1"), repository, timeZone = TimeZone.UTC)
        var shown by mutableStateOf(true)
        var dark by mutableStateOf(true)
        showWrite(viewModel, { dark }) { shown }
        onAllNodes(hasSetTextAction())[0].performTextInput("새 글 ")
        waitForIdle()
        val typed = editorText()

        shown = false
        waitForIdle()
        dark = false
        shown = true
        waitForIdle()

        assertEquals(typed, editorText())
        onAllNodes(hasSetTextAction())[0].performTextInput("더 ")
        waitForIdle()
        viewModel.onStop()
        waitForIdle()
        val stored = runBlocking { repository.draft(key).first() }?.bodyHtml.orEmpty()
        assertTrue("#b91c1c" in stored, stored)
        assertTrue("새 글" in stored && "더" in stored, stored)
    }

    /** 앱 틀이 화면 모델 저장소를 나눠 주지 못해도 다른 일기의 화면 모델을 받지 않음 */
    @Test
    fun another_entry_gets_its_own_screen_model() = runComposeUiTest {
        val a = DraftKey.Edit("a")
        val b = DraftKey.Edit("b")
        val repository = FakeDraftRepository(editable = mapOf("a" to draft(a, "<p>일기 가</p>"), "b" to draft(b, "<p>일기 나</p>")))
        startKoin { modules(module { viewModel { (target: WriteTarget) -> testWriteViewModel(target, repository) } }) }
        val owner =
            object : ViewModelStoreOwner {
                override val viewModelStore = ViewModelStore()
            }
        var target by mutableStateOf("a")
        setContent {
            CompositionLocalProvider(LocalUIViewController provides UIViewController(), LocalViewModelStoreOwner provides owner) {
                DiaryTheme(darkTheme = false) {
                    key(target) {
                        WriteRoute(target = WriteTarget.Edit(target), onSaved = {}, onClosed = {})
                    }
                }
            }
        }
        waitForIdle()
        assertEquals("일기 가", editorText())

        target = "b"
        waitForIdle()

        assertEquals("일기 나", editorText())
    }

    @AfterTest
    fun tearDown() = stopKoin()
}
