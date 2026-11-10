package org.sjhstudio.diary.app.navigation

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import kotlin.test.Test
import kotlin.test.assertEquals

// 쓰기 화면 둘이 화면 모델을 나눠 쓰면 다른 일기가 열림
// 키의 toString()에 일기 ID를 담지 않아 기본 내용 키는 모든 쓰기 화면이 같음
private class TargetHolder(val key: WriteKey) : ViewModel()

@OptIn(ExperimentalTestApi::class)
class WriteKeyNavUiTest {
    private val seen = mutableListOf<String?>()

    private fun write(backStack: MutableList<NavKey>, block: suspend () -> Unit) = runComposeUiTest {
        val owner =
            object : ViewModelStoreOwner {
                override val viewModelStore = ViewModelStore()
            }
        setContent {
            CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                    entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator(), rememberViewModelStoreNavEntryDecorator()),
                    entryProvider =
                        entryProvider {
                            entry<HomeKey> { BasicText("home") }
                            entry<PlusKey> { BasicText("plus") }
                            entry<WriteKey>(clazzContentKey = { it.contentKey }) { key ->
                                val holder = viewModel { TargetHolder(key) }
                                seen += holder.key.entryId
                                BasicText("write")
                            }
                        }
                )
            }
        }
        waitForIdle()
        block()
        waitForIdle()
    }

    /** 쓰기 → 결제 위에 다시 쓰기가 열려도 각자의 화면 모델을 받음 */
    @Test
    fun another_write_on_the_stack_gets_its_own_screen_model() {
        val backStack = mutableStateListOf<NavKey>(HomeKey, WriteKey(entryId = "a"))
        write(backStack) {
            backStack.add(PlusKey)
            backStack.add(WriteKey(entryId = "b"))
        }

        assertEquals("b", seen.last())
    }
}
