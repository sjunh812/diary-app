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
import org.sjhstudio.diary.core.designsystem.component.LocalTopBarBackHidden
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize

/** 목록 화면 표시 */
val ListPane: Map<String, Any> = mapOf("pane" to "list")

/** 상세 화면 표시. 어느 목록 옆에도 섬 */
val DetailPane: Map<String, Any> = mapOf("pane" to "detail")

/** 설정 목록. 설정 상세는 이 목록 옆에만 섬 */
val SettingsListPane: Map<String, Any> = mapOf("pane" to "list", LIST_ID to SETTINGS_LIST)

/**
 * 설정 상세. 설정 목록 옆에만 섬
 * 홈에서 연 복원 확인 같은 설정 화면이 홈 목록 옆에 붙지 않음
 */
val SettingsDetailPane: Map<String, Any> = mapOf("pane" to "detail", DETAIL_OF to SETTINGS_LIST)

const val SETTINGS_LIST = "settings"
private const val LIST_ID = "listId"
private const val DETAIL_OF = "detailOf"

/** 탭의 맨 위 화면 표시. 앱 틀이 탭 바 자리를 비워 줌 */
const val TAB_ROOT = "tabRoot"
const val FULL_SCREEN = "fullScreen"

/** 위에 얹은 레일의 폭. 쓰기처럼 전체를 쓰는 화면이 아니면 이만큼 비워 둠 */
val LocalRailWidth = compositionLocalOf { Dp.Hairline }

/** 두 칸 장면 안의 화면은 장면이 레일 자리를 비움 */
val LocalInListDetail = compositionLocalOf { false }

/** 쓰기와 사진 크게 보기. 레일과 탭 자리를 비우지 않음 */
val FullScreenEntry: Map<String, Any> = mapOf(FULL_SCREEN to true)

/**
 * 탭 첫 화면. 폰에서는 탭 바와 쓰기 버튼을 함께 그려
 * 상세로 넘어갈 때 탭 바가 이 화면과 같이 움직이고 새 화면이 그 위를 덮음
 */
fun tabRoot(tab: TabScreen): Map<String, Any> = mapOf(TAB_ROOT to true, ROOT_OF to tab) + TabTransitions

const val ROOT_OF = "rootOf"

private val NavEntry<NavKey>.listId get() = metadata[LIST_ID] as? String

/**
 * 두 칸으로 나눌 자리. [list]는 왼쪽 목록, [details]는 그 위에 쌓인 상세(마지막이 오른쪽에 보임)
 * 맨 위 목록 뒤가 모두 그 목록의 상세일 때만 나눔
 */
internal class PaneSplit(val list: Int, val details: Int)

internal fun paneSplit(panes: List<Map<String, Any>>): PaneSplit? {
    val at = panes.indexOfLast { it["pane"] == "list" }
    if (at < 0) return null
    val listId = panes[at][LIST_ID]
    val after = panes.drop(at + 1)
    val ok = after.all { it["pane"] == "detail" && (it[DETAIL_OF] == null || it[DETAIL_OF] == listId) }
    return if (ok) PaneSplit(at, after.size) else null
}

/**
 * 840dp부터 목록과 상세를 나란히 놓음(스펙 001 research R25, contracts/navigation.md)
 * 스택은 폭과 상관없이 [목록, 상세]라 폭이 좁아지면 상세가 한 칸으로 쌓인 상태가 됨(FR-037)
 */
class ListDetailSceneStrategy(private val wide: Boolean, private val placeholder: @Composable (listId: String?) -> Unit) :
    SceneStrategy<NavKey> {
    override fun SceneStrategyScope<NavKey>.calculateScene(entries: List<NavEntry<NavKey>>): Scene<NavKey>? {
        // 상세 안에서 더 들어간 화면(백업 → 복원 고르기)도 오른쪽 칸에 남음
        val split = paneSplit(entries.map { it.metadata })?.takeIf { wide } ?: return null
        val list = entries[split.list]
        val detail = if (split.details > 0) entries.last() else null
        val placeholder: @Composable () -> Unit = { placeholder(list.listId) }
        return ListDetailScene(list, detail, firstDetail = split.details == 1, entries.dropLast(1), placeholder)
    }
}

private class ListDetailScene(
    private val list: NavEntry<NavKey>,
    private val detail: NavEntry<NavKey>?,
    private val firstDetail: Boolean,
    override val previousEntries: List<NavEntry<NavKey>>,
    private val placeholder: @Composable () -> Unit
) : Scene<NavKey> {
    // 상세가 바뀌어도 같은 장면이라 왼쪽 목록이 새로 그려지지 않음
    override val key: Any = list.contentKey
    override val entries: List<NavEntry<NavKey>> = listOfNotNull(list, detail)
    override val content: @Composable () -> Unit = {
        // 장면 전체에 바탕을 깖. 두 칸 사이 틈(24)이 비면 밀려 들어오는 동안 뒤의 어두운 앞 화면이 세로 띠로 비침
        Row(Modifier.fillMaxSize().padding(start = LocalRailWidth.current).sceneBackdrop(DiaryTheme.colors.canvas)) {
            CompositionLocalProvider(LocalInListDetail provides true) {
                Box(Modifier.width(DiaryWriteSize.listPane).fillMaxHeight()) { list.Content() }
            }
            // 목록 칸 바로 끝에 가는 선을 둬 목록과 상세를 나눔
            // 고른 줄의 바탕이 선까지 닿아야 끊겨 보이지 않음. 가운데 두면 틈이 남음
            // 레일 옆 선, 달력 두 칸의 선과 같은 `line` 1
            // 상세 화면이 양옆에 제 여백 24를 두므로 선 뒤에 틈을 더 넣지 않음(넣으면 왼쪽만 48)
            Box(Modifier.width(DiarySpacing.hairline).fillMaxHeight().background(DiaryTheme.colors.line))
            // 오른쪽 첫 화면은 왼쪽 목록이 돌아갈 곳이라 뒤로 단추를 두지 않음
            CompositionLocalProvider(LocalInListDetail provides true, LocalTopBarBackHidden provides firstDetail) {
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
