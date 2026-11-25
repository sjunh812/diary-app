package org.sjhstudio.diary.feature.write

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.retain.LocalRetainedValuesStoreProvider
import androidx.compose.runtime.retain.ManagedRetainedValuesStore
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.uikit.LocalUIViewController
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
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
    private class Owner : LifecycleOwner {
        val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle get() = registry
    }

    private fun draft(key: DraftKey, body: String) =
        Draft(key, LocalDate(2026, 11, 1), LocalTime(9, 0), body, null, null, null, false, false, Clock.System.now())

    private fun ComposeUiTest.editorText(): String {
        val node = onAllNodes(hasSetTextAction())[0].fetchSemanticsNode()
        return node.config[SemanticsProperties.EditableText].text
    }

    /** 화면을 다시 그리는 흉내(회전·잠금). 컴포지션을 없앴다가 저장해 둔 상태로 다시 만듦 */
    private fun ComposeUiTest.showWrite(
        viewModel: WriteViewModel,
        target: WriteTarget = WriteTarget.Edit("e1"),
        dark: () -> Boolean = { false },
        width: () -> Dp = { 390.dp },
        lifecycle: () -> Owner = { Owner() },
        retained: ManagedRetainedValuesStore = forgetful(),
        shown: () -> Boolean
    ) {
        setContent {
            CompositionLocalProvider(LocalUIViewController provides UIViewController(), LocalLifecycleOwner provides lifecycle()) {
                DiaryTheme(darkTheme = dark()) {
                    val holder = rememberSaveableStateHolder()
                    // 시험 저장소는 자기가 빠질 때만 값을 남겨 화면과 함께 뺌
                    if (shown()) {
                        holder.SaveableStateProvider("write") {
                            LocalRetainedValuesStoreProvider(retained) {
                                Box(Modifier.width(width()).height(800.dp)) {
                                    WriteRoute(target = target, onSaved = {}, onClosed = {}, injected = viewModel)
                                }
                            }
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
        showWrite(viewModel, dark = { dark }) { shown }
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

    /** 열지 못한 일기는 빈 일기처럼 보이지 않게 편집기 없이 실패 안내만 그림 */
    @Test
    fun an_entry_that_cannot_open_has_no_editor() = runComposeUiTest {
        val viewModel = testWriteViewModel(WriteTarget.Edit("e1"), FakeDraftRepository(), timeZone = TimeZone.UTC)
        showWrite(viewModel) { true }

        assertTrue(onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isEmpty())
    }

    /** 새 글은 편집기가 그려지자마자 바로 쓸 수 있게 커서를 받음 */
    @Test
    fun a_new_entry_gets_the_cursor() = runComposeUiTest {
        val viewModel = testWriteViewModel(WriteTarget.New(LocalDate(2026, 11, 1)), FakeDraftRepository(), timeZone = TimeZone.UTC)
        showWrite(viewModel, target = WriteTarget.New(LocalDate(2026, 11, 1))) { true }

        onAllNodes(hasSetTextAction())[0].assertIsFocused()
    }

    /** 쓰던 일기를 열면 커서를 주지 않음. 읽다가 고칠 자리를 직접 누름 */
    @Test
    fun an_entry_being_edited_opens_without_the_cursor() = runComposeUiTest {
        val key = DraftKey.Edit("e1")
        val repository = FakeDraftRepository(editable = mapOf("e1" to draft(key, "<p>옛 글</p>")))
        val viewModel = testWriteViewModel(WriteTarget.Edit("e1"), repository, timeZone = TimeZone.UTC)
        showWrite(viewModel) { true }

        onAllNodes(hasSetTextAction())[0].assertIsNotFocused()
    }

    private fun ComposeUiTest.typeInto(
        viewModel: WriteViewModel,
        target: WriteTarget = WriteTarget.Edit("e1"),
        width: () -> Dp = { 390.dp },
        owner: () -> Owner = { Owner() },
        retained: ManagedRetainedValuesStore = forgetful(),
        shown: () -> Boolean
    ) {
        showWrite(viewModel, target = target, width = width, lifecycle = owner, retained = retained, shown = shown)
        onAllNodes(hasSetTextAction())[0].performTextInput("새 ")
        waitForIdle()
        onAllNodes(hasSetTextAction())[0].assertIsFocused()
    }

    private fun editViewModel(): WriteViewModel {
        val key = DraftKey.Edit("e1")
        val repository = FakeDraftRepository(editable = mapOf("e1" to draft(key, "<p>옛 글</p>")))
        return testWriteViewModel(WriteTarget.Edit("e1"), repository, timeZone = TimeZone.UTC)
    }

    private val newTarget = WriteTarget.New(LocalDate(2026, 11, 1))

    private fun newViewModel() = testWriteViewModel(newTarget, FakeDraftRepository(), timeZone = TimeZone.UTC)

    /** 잠금처럼 화면이 살아 있는 채 치울 때는 retain 값을 남기지 않음 */
    private fun forgetful() = ManagedRetainedValuesStore().apply { disableRetainingExitedValues() }

    /**
     * 재생성 흉내
     * 없어지는 동안만 retain 값을 남기고 다시 그린 첫 화면 뒤 버림
     */
    private fun ComposeUiTest.recreate(
        owner: Owner,
        retained: ManagedRetainedValuesStore,
        setShown: (Boolean) -> Unit,
        newOwner: () -> Unit,
        firstFrame: () -> Unit = { setShown(true) }
    ) {
        owner.registry.currentState = Lifecycle.State.DESTROYED
        retained.enableRetainingExitedValues()
        setShown(false)
        waitForIdle()
        newOwner()
        firstFrame()
        waitForIdle()
        retained.disableRetainingExitedValues()
        waitForIdle()
    }

    /**
     * 쓰던 중 회전으로 화면을 다시 만들면 커서를 돌려줌
     * 시험 환경은 화면 키보드가 없어 하드웨어 키보드로 쓰는 것과 같음
     */
    @Test
    fun rotating_while_writing_keeps_the_cursor() = runComposeUiTest {
        val viewModel = editViewModel()
        val retained = forgetful()
        var owner by mutableStateOf(Owner())
        var shown by mutableStateOf(true)
        typeInto(viewModel, owner = { owner }, retained = retained) { shown }

        recreate(owner, retained, { shown = it }, { owner = Owner() })

        onAllNodes(hasSetTextAction())[0].assertIsFocused()
    }

    /** 다시 만든 첫 화면이 잠금이면 쓰던 커서를 버림. 열어도 키보드가 올라오지 않음 */
    @Test
    fun the_lock_screen_after_rotating_drops_the_cursor() = runComposeUiTest {
        val viewModel = editViewModel()
        val retained = forgetful()
        var owner by mutableStateOf(Owner())
        var shown by mutableStateOf(true)
        typeInto(viewModel, owner = { owner }, retained = retained) { shown }

        recreate(owner, retained, { shown = it }, { owner = Owner() }, firstFrame = {})
        shown = true
        waitForIdle()

        onAllNodes(hasSetTextAction())[0].assertIsNotFocused()
    }

    /** 다시 만든 화면을 권유가 덮고 있으면 돌려줄 커서를 버림. 닫거나 다시 돌려도 키보드가 올라오지 않음 */
    @Test
    fun a_prompt_over_the_recreated_screen_drops_the_cursor() = runComposeUiTest {
        val viewModel = editViewModel()
        val retained = forgetful()
        var owner by mutableStateOf(Owner())
        var shown by mutableStateOf(true)
        typeInto(viewModel, owner = { owner }, retained = retained) { shown }

        recreate(owner, retained, { shown = it }, { owner = Owner() }) {
            viewModel.requestChecklist(ChecklistTarget("", 0, 0))
            waitUntil { viewModel.state.value.checklistPick is ChecklistPick.Locked }
            shown = true
        }
        viewModel.checklistPickHandled()
        waitForIdle()

        onAllNodes(hasSetTextAction())[0].assertIsNotFocused()

        // 본문을 건드리지 않고 한 번 더 돌려도 안 돌려줌
        recreate(owner, retained, { shown = it }, { owner = Owner() })
        onAllNodes(hasSetTextAction())[0].assertIsNotFocused()
    }

    /** 새 일기도 쓰던 중 회전하면 커서를 돌려줌 */
    @Test
    fun rotating_a_new_entry_keeps_the_cursor() = runComposeUiTest {
        val viewModel = newViewModel()
        val retained = forgetful()
        var owner by mutableStateOf(Owner())
        var shown by mutableStateOf(true)
        typeInto(viewModel, target = newTarget, owner = { owner }, retained = retained) { shown }

        recreate(owner, retained, { shown = it }, { owner = Owner() })

        onAllNodes(hasSetTextAction())[0].assertIsFocused()
    }

    /** 쓰던 중 권유를 열고 닫은 뒤 회전해도 키보드를 올리지 않음 */
    @Test
    fun rotating_after_closing_a_prompt_does_not_raise_the_keyboard() = runComposeUiTest {
        val viewModel = editViewModel()
        val retained = forgetful()
        var owner by mutableStateOf(Owner())
        var shown by mutableStateOf(true)
        typeInto(viewModel, owner = { owner }, retained = retained) { shown }
        viewModel.requestChecklist(ChecklistTarget(editorText(), 0, 0))
        waitForIdle()
        viewModel.checklistPickHandled()
        waitForIdle()

        recreate(owner, retained, { shown = it }, { owner = Owner() })

        onAllNodes(hasSetTextAction())[0].assertIsNotFocused()
    }

    /** 쓰다가 아이패드를 돌리거나 폴더블을 펴 두 칸으로 바뀌어도 커서가 남음 */
    @Test
    fun switching_to_two_panes_keeps_the_cursor() = runComposeUiTest {
        val viewModel = editViewModel()
        var width by mutableStateOf(390.dp)
        typeInto(viewModel, width = { width }) { true }

        width = 1024.dp
        waitForIdle()
        onAllNodes(hasSetTextAction())[0].assertIsFocused()

        width = 390.dp
        waitForIdle()
        onAllNodes(hasSetTextAction())[0].assertIsFocused()
    }

    /** 잠금처럼 쓰기 화면을 치웠다 다시 그리면 커서를 주지 않음 */
    @Test
    fun coming_back_from_the_lock_screen_does_not_raise_the_keyboard() = runComposeUiTest {
        val viewModel = editViewModel()
        var shown by mutableStateOf(true)
        typeInto(viewModel) { shown }

        shown = false
        waitForIdle()
        shown = true
        waitForIdle()

        onAllNodes(hasSetTextAction())[0].assertIsNotFocused()
    }

    /** 새 일기도 잠금처럼 치웠다 다시 그리면 커서를 다시 주지 않음 */
    @Test
    fun a_new_entry_coming_back_from_the_lock_screen_does_not_raise_the_keyboard() = runComposeUiTest {
        val viewModel = newViewModel()
        var shown by mutableStateOf(true)
        typeInto(viewModel, target = newTarget) { shown }

        shown = false
        waitForIdle()
        shown = true
        waitForIdle()

        onAllNodes(hasSetTextAction())[0].assertIsNotFocused()
    }

    /** 쓰던 중 Plus 권유가 뜨면 커서를 거두고, 닫아도 돌려주지 않음 */
    @Test
    fun closing_a_plus_prompt_does_not_raise_the_keyboard() = runComposeUiTest {
        val viewModel = editViewModel()
        typeInto(viewModel) { true }

        viewModel.requestChecklist(ChecklistTarget(editorText(), 0, 0))
        waitForIdle()
        onAllNodes(hasSetTextAction())[0].assertIsNotFocused()

        viewModel.checklistPickHandled()
        waitForIdle()
        onAllNodes(hasSetTextAction())[0].assertIsNotFocused()
    }

    /** 쓰는 중 테마가 바뀌어도 고르던 범위가 그대로 남음 */
    @Test
    fun a_theme_change_while_writing_keeps_the_selection() = runComposeUiTest {
        val key = DraftKey.Edit("e1")
        val repository = FakeDraftRepository(editable = mapOf("e1" to draft(key, "<p>고르던 글</p>")))
        val viewModel = testWriteViewModel(WriteTarget.Edit("e1"), repository, timeZone = TimeZone.UTC)
        var dark by mutableStateOf(false)
        showWrite(viewModel, dark = { dark }) { true }
        val editor = onAllNodes(hasSetTextAction())[0]
        editor.performTextInputSelection(TextRange(0, 3))
        waitForIdle()

        dark = true
        waitForIdle()

        val selection = editor.fetchSemanticsNode().config[SemanticsProperties.TextSelectionRange]
        assertEquals(TextRange(0, 3), selection)
        assertEquals("고르던 글", editorText())
    }

    @AfterTest
    fun tearDown() = stopKoin()
}
