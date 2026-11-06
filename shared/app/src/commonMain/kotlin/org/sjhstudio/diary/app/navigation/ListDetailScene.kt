package org.sjhstudio.diary.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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

val TabRoot: Map<String, Any> = mapOf(TAB_ROOT to true)

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
        Row(Modifier.fillMaxSize().padding(start = LocalRailWidth.current)) {
            CompositionLocalProvider(LocalInListDetail provides true) {
                Box(Modifier.width(DiaryWriteSize.listPane).fillMaxHeight()) { list.Content() }
            }
            Spacer(Modifier.width(DiarySpacing.x6))
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
