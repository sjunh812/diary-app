package org.sjhstudio.diary.feature.plus

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.feature.plus.resources.Res
import org.sjhstudio.diary.feature.plus.resources.plus_checklist_title
import org.sjhstudio.diary.feature.plus.resources.plus_coming
import org.sjhstudio.diary.feature.plus.resources.plus_format_title
import org.sjhstudio.diary.feature.plus.resources.plus_others_in_all
import org.sjhstudio.diary.feature.plus.resources.plus_see_all

// 결제 화면 혜택 묶음(스펙 008 US6). 다른 혜택은 목록 없이 [모두 보기]로 이끌고, 누르고 들어온 혜택만 한 줄
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

    @Test
    fun other_benefits_point_to_see_all_without_list() = run(from = null) {
        onNodeWithText(text(Res.string.plus_others_in_all)).assertIsDisplayed()
        // 업데이트 안내는 결제 화면에 두지 않음(관리 화면에만)
        onNodeWithText(text(Res.string.plus_coming)).assertDoesNotExist()
        onNodeWithText(text(Res.string.plus_checklist_title)).assertDoesNotExist()
        onNodeWithText(text(Res.string.plus_format_title)).assertDoesNotExist()
    }

    @Test
    fun formatting_entry_shows_only_that_benefit() = run(from = PlusBenefit.LIST_FORMATTING) {
        onNodeWithText(text(Res.string.plus_format_title)).assertIsDisplayed()
        onNodeWithText(text(Res.string.plus_checklist_title)).assertDoesNotExist()
    }

    @Test
    fun see_all_opens_the_sheet() = run(from = null) { seeAll ->
        onNodeWithText(text(Res.string.plus_see_all)).performClick()
        waitForIdle()
        assertEquals(1, seeAll.size)
    }
}
