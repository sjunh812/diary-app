package org.sjhstudio.diary.feature.plus

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.feature.plus.resources.Res
import org.sjhstudio.diary.feature.plus.resources.plus_checklist_title
import org.sjhstudio.diary.feature.plus.resources.plus_format_title
import org.sjhstudio.diary.feature.plus.resources.plus_see_all

// 결제 화면 혜택 묶음(스펙 008 US6)
@OptIn(ExperimentalTestApi::class)
class PaywallBenefitsUiTest {
    private fun text(res: StringResource) = runBlocking { getString(res) }

    private fun run(from: PlusBenefit?, check: ComposeUiTest.(MutableList<Unit>) -> Unit) = runComposeUiTest {
        val seeAll = mutableListOf<Unit>()
        setContent {
            DiaryTheme(darkTheme = false) {
                PaywallScreen("₩14,900", false, false, false, {}, {}, {}, {}, from = from, onSeeAll = { seeAll += Unit })
            }
        }
        waitForIdle()
        check(seeAll)
    }

    private fun ComposeUiTest.top(res: StringResource) = onNodeWithText(text(res)).getUnclippedBoundsInRoot().top

    @Test
    fun other_benefits_keep_order_without_entry() = run(from = null) {
        assertTrue(top(Res.string.plus_checklist_title) < top(Res.string.plus_format_title))
    }

    @Test
    fun formatting_entry_comes_first() = run(from = PlusBenefit.LIST_FORMATTING) {
        assertTrue(top(Res.string.plus_format_title) < top(Res.string.plus_checklist_title))
    }

    @Test
    fun see_all_opens_the_sheet() = run(from = null) { seeAll ->
        onNodeWithText(text(Res.string.plus_see_all)).performClick()
        waitForIdle()
        assertEquals(1, seeAll.size)
    }
}
