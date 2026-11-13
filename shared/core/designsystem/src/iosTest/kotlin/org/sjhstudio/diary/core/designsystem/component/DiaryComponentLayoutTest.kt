package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.LocalScreenGutter

// 공통 부품의 크기와 배치가 `docs/design.md` 규격을 지키는지 확인
@OptIn(ExperimentalTestApi::class)
class DiaryComponentLayoutTest {
    @Test
    fun search_clear_touch_is_48_without_growing_the_field() = runComposeUiTest {
        var text = "일기"
        setContent {
            DiaryTheme(darkTheme = false) {
                Box(Modifier.width(360.dp)) {
                    DiarySearchField(text, { text = it }, "찾기", "지우기", Modifier.fillMaxWidth().testTag("field"))
                }
            }
        }
        onNodeWithTag("field").assertHeightIsEqualTo(44.dp)
        onNodeWithContentDescription("지우기").assertWidthIsEqualTo(48.dp).assertHeightIsEqualTo(48.dp).performClick()
        waitForIdle()
        assertEquals("", text)
    }

    @Test
    fun toast_with_action_stays_48_tall_and_action_touch_is_48() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent {
            DiaryTheme(darkTheme = false) {
                val state = remember { DiaryToastState() }
                LaunchedEffect(Unit) { state.show("휴지통으로 옮겼어요", actionLabel = "되돌리기") }
                Box(Modifier.width(360.dp)) { DiaryToastHost(state, Modifier.testTag("toast")) }
            }
        }
        mainClock.advanceTimeBy(500)
        onNodeWithTag("toast").assertHeightIsEqualTo(48.dp)
        onNodeWithText("되돌리기").assertHeightIsEqualTo(48.dp)
        assertTrue(onNodeWithText("되돌리기").getBoundsInRoot().let { it.right - it.left } >= 48.dp)
    }

    @Test
    fun short_settings_value_stays_at_the_end_of_the_row() = runComposeUiTest {
        setContent {
            DiaryTheme(darkTheme = false) {
                Box(Modifier.width(360.dp)) { DiarySettingsValueRow("언어", "한국어") }
            }
        }
        val value = onNodeWithText("한국어", useUnmergedTree = true).getBoundsInRoot()
        assertEquals(340.dp, value.right)
    }

    @Test
    fun long_settings_value_leaves_room_for_the_name() = runComposeUiTest {
        val long = "아주아주아주아주아주아주아주아주아주아주아주아주아주아주아주아주아주아주아주긴값"
        setContent {
            DiaryTheme(darkTheme = false) {
                Box(Modifier.width(360.dp)) { DiarySettingsValueRow("계정", long) }
            }
        }
        val title = onNodeWithText("계정", useUnmergedTree = true).getBoundsInRoot()
        val value = onNodeWithText(long, useUnmergedTree = true).getBoundsInRoot()
        assertTrue(title.right - title.left > 0.dp)
        assertTrue(value.left >= title.right)
        assertEquals(340.dp, value.right)
    }

    @Test
    fun chip_keeps_its_text_in_a_lazy_row() = runComposeUiTest {
        setContent {
            DiaryTheme(darkTheme = false) {
                LazyRow(Modifier.width(360.dp)) {
                    item { DiaryChip("기분", selected = false, onClick = {}, trailingIcon = DiaryIcons.ChevronDown) }
                }
            }
        }
        val chip = onNodeWithText("기분", useUnmergedTree = true).getBoundsInRoot()
        assertTrue(chip.right - chip.left > 0.dp)
    }

    @Test
    fun button_grows_with_two_lines_instead_of_clipping() = runComposeUiTest {
        val long = "두 줄이 되도록 일부러 아주 길게 적은 버튼 글이에요 정말 길어요"
        setContent {
            DiaryTheme(darkTheme = false) {
                Column(Modifier.width(240.dp)) {
                    DiaryButton("저장", {}, Modifier.fillMaxWidth().testTag("short"), size = DiaryButtonSize.XLarge)
                    DiaryButton(long, {}, Modifier.fillMaxWidth().testTag("long"), size = DiaryButtonSize.Medium)
                }
            }
        }
        onNodeWithTag("short").assertHeightIsEqualTo(56.dp)
        val tall = onNodeWithTag("long").getBoundsInRoot()
        assertTrue(tall.bottom - tall.top > 48.dp)
    }

    @Test
    fun tabs_are_48_tall_and_share_the_width() = runComposeUiTest {
        setContent {
            DiaryTheme(darkTheme = false) {
                Box(Modifier.width(360.dp)) { DiarySegmentedTabs(listOf("주", "월", "년", "전체"), 0, {}, Modifier.testTag("tabs")) }
            }
        }
        onNodeWithTag("tabs").assertHeightIsEqualTo(48.dp)
        onNodeWithText("주").assertHeightIsEqualTo(48.dp).assertWidthIsEqualTo(88.dp)
        onNodeWithText("전체").assertHeightIsEqualTo(48.dp).assertWidthIsEqualTo(88.dp)
    }

    @Test
    fun rows_follow_the_screen_gutter() = runComposeUiTest {
        setContent {
            DiaryTheme(darkTheme = false) {
                CompositionLocalProvider(LocalScreenGutter provides DiarySpacing.x6) {
                    Column(Modifier.width(600.dp)) {
                        DiarySettingsHeader("머리")
                        DiaryValueRow("이름", "값")
                    }
                }
            }
        }
        assertEquals(24.dp, onNodeWithText("머리").getBoundsInRoot().left)
        assertEquals(24.dp, onNodeWithText("이름").getBoundsInRoot().left)
    }

    @Test
    fun sheet_stops_at_90_percent_and_scrolls_to_the_buttons() = runComposeUiTest {
        setContent {
            DiaryTheme(darkTheme = false) {
                Box(Modifier.fillMaxSize().testTag("window")) {
                    DiaryBottomSheet(onDismiss = {}, title = "제목") {
                        BasicText("내용", Modifier.testTag("first"))
                        Box(Modifier.fillMaxWidth().height(3000.dp))
                        DiaryButtonPair("닫기", {}, "완료", {})
                    }
                }
            }
        }
        waitForIdle()
        val windowHeight = onNodeWithTag("window").getBoundsInRoot().let { it.bottom - it.top }
        val first = onNodeWithTag("first").getBoundsInRoot()
        // 시트 맨 위 안쪽(손잡이 위 12 + 손잡이 4 + 아래 20)을 빼면 시트 위 끝
        assertTrue(windowHeight - (first.top - 36.dp) <= windowHeight * 0.9f + 11.dp, "sheet top ${first.top} of $windowHeight")
        onNodeWithText("완료").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun sheet_puts_24_above_the_buttons_and_12_between_contents() = runComposeUiTest {
        setContent {
            DiaryTheme(darkTheme = false) {
                Box(Modifier.fillMaxSize()) {
                    DiaryBottomSheet(onDismiss = {}) {
                        BasicText("하나", Modifier.testTag("one"))
                        BasicText("둘", Modifier.testTag("two"))
                        DiaryButtonPair("닫기", {}, "완료", {})
                    }
                }
            }
        }
        waitForIdle()
        val one = onNodeWithTag("one").getBoundsInRoot()
        val two = onNodeWithTag("two").getBoundsInRoot()
        val button = onNodeWithText("닫기").getBoundsInRoot()
        assertEquals(12.dp, two.top - one.bottom)
        assertEquals(24.dp, button.top - two.bottom)
    }
}
