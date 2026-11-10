package org.sjhstudio.diary.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize

/** 목록 화면 표시. */
val ListPane: Map<String, Any> = mapOf("pane" to "list")

/** 상세 화면 표시. */
val DetailPane: Map<String, Any> = mapOf("pane" to "detail")

/**
 * 탭의 맨 위 화면 표시.
 * 앱 틀이 탭 바 자리를 비워 준다.
 */
const val TAB_ROOT = "tabRoot"
const val FULL_SCREEN = "fullScreen"

/**
 * 위에 얹은 레일의 폭.
 * 쓰기처럼 전체를 쓰는 화면이 아니면 이만큼 비워 둔다.
 */
val LocalRailWidth = compositionLocalOf { Dp.Hairline }

/** 두 칸 장면 안의 화면은 장면이 레일 자리를 비운다. */
val LocalInListDetail = compositionLocalOf { false }

/**
 * 쓰기와 사진 크게 보기.
 * 레일과 탭 자리를 비우지 않는다.
 */
val FullScreenEntry: Map<String, Any> = mapOf(FULL_SCREEN to true)

/**
 * 탭의 맨 위 화면.
 * 폰에서는 이 화면이 탭 바와 쓰기 버튼을 함께 그린다.
 * 상세로 넘어갈 때 탭 바가 따로 사라지지 않고 이 화면과 같이 움직이고, 새 화면이 그 위를 덮는다.
 */
fun tabRoot(tab: TabScreen): Map<String, Any> = mapOf(TAB_ROOT to true, ROOT_OF to tab) + TabTransitions

const val ROOT_OF = "rootOf"

private val NavEntry<NavKey>.isList get() = metadata["pane"] == "list"
private val NavEntry<NavKey>.isDetail get() = metadata["pane"] == "detail"

/**
 * 840dp부터 목록과 상세를 나란히 놓는다(스펙 001 research R25, contracts/navigation.md).
 * 스택은 폭과 상관없이 [목록, 상세]라서 폭이 좁아지면 상세가 한 칸으로 쌓인 상태가 된다(FR-037).
 */
class ListDetailSceneStrategy(private val wide: Boolean, private val placeholder: @Composable () -> Unit) : SceneStrategy<NavKey> {
    override fun SceneStrategyScope<NavKey>.calculateScene(entries: List<NavEntry<NavKey>>): Scene<NavKey>? {
        if (!wide) return null
        val last = entries.last()
        val before = entries.getOrNull(entries.lastIndex - 1)
        return when {
            last.isList -> ListDetailScene(last, null, entries.dropLast(1), placeholder)
            last.isDetail && before?.isList == true -> ListDetailScene(before, last, entries.dropLast(1), placeholder)
            else -> null
        }
    }
}

private class ListDetailScene(
    private val list: NavEntry<NavKey>,
    private val detail: NavEntry<NavKey>?,
    override val previousEntries: List<NavEntry<NavKey>>,
    private val placeholder: @Composable () -> Unit
) : Scene<NavKey> {
    // 상세가 바뀌어도 같은 장면이라 왼쪽 목록이 새로 그려지지 않는다.
    override val key: Any = list.contentKey
    override val entries: List<NavEntry<NavKey>> = listOfNotNull(list, detail)
    override val content: @Composable () -> Unit = {
        // 장면 전체에 바탕을 깐다.
        // 두 칸 사이 틈(24)이 비어 있으면 밀려 들어오는 동안 뒤의 어둡게 깔린 앞 화면이 세로 띠로 비쳤다(2026-11-08 사용자 지적, iPad).
        Row(Modifier.fillMaxSize().padding(start = LocalRailWidth.current).background(DiaryTheme.colors.canvas)) {
            CompositionLocalProvider(LocalInListDetail provides true) {
                Box(Modifier.width(DiaryWriteSize.listPane).fillMaxHeight()) { list.Content() }
            }
            // 목록 칸 바로 끝에 가는 선을 둬 목록과 상세가 나뉜 것이 보이게 한다(2026-11-08 사용자 요청).
            // 고른 줄의 바탕이 선까지 닿아야 끊겨 보이지 않는다(가운데 두었더니 틈이 남았다, 사용자 지적).
            // 레일 옆 선, 달력 두 칸의 선과 같은 `line` 1이다.
            // 상세 화면이 양옆에 제 여백 24를 두므로 선 뒤에 틈을 더 넣지 않는다(넣으면 왼쪽만 48로 넓어진다, 사용자 지적).
            Box(Modifier.width(DiarySpacing.hairline).fillMaxHeight().background(DiaryTheme.colors.line))
            CompositionLocalProvider(LocalInListDetail provides true) {
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    if (detail !=
                        null
                    ) {
                        key(detail.contentKey) { detail.Content() }
                    } else {
                        placeholder()
                    }
                }
            }
        }
    }
}
