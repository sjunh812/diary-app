package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme

// 하단 탭·레일 이름이 기기 글자 크기 200%에서도 칸 80 안에서 잘리지 않음(docs/design.md 하단 탭, DECISIONS 2026-11-03)
// 세 언어 이름을 직접 넣어 시뮬레이터 언어와 상관없이 봄
@OptIn(ExperimentalTestApi::class)
class DiaryNavigationTest {
    private val labels =
        mapOf(
            "ko" to listOf("홈", "달력", "돌아보기", "설정"),
            "en" to listOf("Home", "Calendar", "Insights", "Settings"),
            "ja" to listOf("ホーム", "カレンダー", "ふりかえり", "設定")
        )

    @Test
    fun bar_labels_are_not_clipped_at_large_text() {
        for ((language, names) in labels) {
            for (fontScale in listOf(1f, 1.3f, 2f)) {
                val clipped = clippedLabels(fontScale) { DiaryNavigationBar(items(names), selected = 0, onSelect = {}) }
                assertEquals(emptyList(), clipped, "$language $fontScale")
            }
        }
    }

    @Test
    fun rail_labels_are_not_clipped_at_large_text() {
        for ((language, names) in labels) {
            for (fontScale in listOf(1f, 2f)) {
                val clipped = clippedLabels(fontScale) { DiaryNavigationRail(items(names), selected = 0, onSelect = {}) }
                assertEquals(emptyList(), clipped, "$language $fontScale")
            }
        }
    }

    private fun items(names: List<String>) = names.map { DiaryNavItem(it, DiaryIcons.TabHome, DiaryIcons.TabHomeSelected) }

    private fun clippedLabels(fontScale: Float, content: @Composable () -> Unit): List<String> {
        var clipped = emptyList<String>()
        runComposeUiTest {
            setContent {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                    // 가장 좁은 폰(360)과 레일이 서는 높이
                    DiaryTheme(darkTheme = false) { Box(Modifier.width(360.dp).height(640.dp)) { content() } }
                }
            }
            clipped = layouts().filter { it.isClipped() }.map { "${it.layoutInput.text.text} ${it.layoutInput.style.fontSize}" }
        }
        return clipped
    }

    private fun ComposeUiTest.layouts(): List<TextLayoutResult> =
        onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text), useUnmergedTree = true).fetchSemanticsNodes().map { node ->
            val results = mutableListOf<TextLayoutResult>()
            node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
            results.single()
        }

    // `hasVisualOverflow`는 쓰지 않음
    // 가운데 맞춘 한 줄 글자는 칸 폭으로 배치돼 넘치지 않아도 폭이 넘친다고 나옴
    // 그래서 글자 높이가 칸보다 큰지, 글자가 제 폭보다 좁게 보이는지로 잘림을 봄
    private fun TextLayoutResult.isClipped() = didOverflowHeight || multiParagraph.maxIntrinsicWidth > size.width + 0.5f
}
