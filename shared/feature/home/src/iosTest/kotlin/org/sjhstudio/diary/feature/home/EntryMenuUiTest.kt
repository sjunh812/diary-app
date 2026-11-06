package org.sjhstudio.diary.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.uikit.LocalUIViewController
import androidx.compose.ui.unit.dp
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.getString
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.platform.FontInstallState
import org.sjhstudio.diary.core.platform.FontInstaller
import org.sjhstudio.diary.core.testing.FakeEntryRepository
import org.sjhstudio.diary.core.testing.FakeSettingsRepository
import org.sjhstudio.diary.core.testing.fakeEntryRow
import org.sjhstudio.diary.feature.home.resources.Res
import org.sjhstudio.diary.feature.home.resources.detail_favorite
import org.sjhstudio.diary.feature.home.resources.detail_unfavorite
import platform.UIKit.UIViewController

private object NoFonts : FontInstaller {
    override fun installedPath(id: String): String? = null

    override fun install(id: String) = emptyFlow<FontInstallState>()
}

/** 홈 메뉴의 즐겨찾기는 그 일기의 상태를 따른다. */
@OptIn(ExperimentalTestApi::class)
class EntryMenuUiTest {
    private val add get() = runBlocking { getString(Res.string.detail_favorite) }
    private val remove get() = runBlocking { getString(Res.string.detail_unfavorite) }
    private val row = "메뉴를 열 일기"

    @AfterTest
    fun tearDown() = stopKoin()

    private fun ComposeUiTest.openMenu(favorite: Boolean) {
        val rows = listOf(fakeEntryRow("a", LocalDate(2026, 11, 1), row, favorite))
        startKoin {
            modules(module { viewModel { HomeViewModel(FakeEntryRepository(rows = rows), FakeSettingsRepository(), NoFonts) } })
        }
        setContent {
            CompositionLocalProvider(LocalUIViewController provides UIViewController(null, null)) {
                DiaryTheme(darkTheme = false) {
                    Box(Modifier.size(400.dp, 800.dp)) { HomeRoute({}, {}, {}, {}, {}, selectedId = null) }
                }
            }
        }
        waitForIdle()
        onNode(hasText(row, substring = true)).performTouchInput { longClick() }
        waitForIdle()
    }

    @Test
    fun a_plain_entry_offers_to_add_it() = runComposeUiTest {
        openMenu(favorite = false)

        onAllNodes(hasText(add)).assertCountEquals(1)
        onAllNodes(hasText(remove)).assertCountEquals(0)
    }

    @Test
    fun a_favorite_entry_offers_to_remove_it() = runComposeUiTest {
        openMenu(favorite = true)

        onAllNodes(hasText(remove)).assertCountEquals(1)
        onAllNodes(hasText(add)).assertCountEquals(0)
    }
}
