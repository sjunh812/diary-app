package org.sjhstudio.diary.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.viewmodel.ViewModelStoreProvider
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreProvider
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/** 앱이 닫힐 때 마지막으로 지나는 탭 */
private val START_TAB: TabScreen = HomeKey

/** 하단 탭과 레일의 순서 */
val TABS: List<TabScreen> = listOf(HomeKey, CalendarKey, InsightKey, SettingsKey)

private val BackStackConfiguration =
    SavedStateConfiguration {
        serializersModule = SerializersModule { polymorphic(NavKey::class) { subclassesOfSealed<Screen>() } }
    }

/**
 * 탭마다 화면을 따로 쌓음(공식 nav3-recipes `multiplestacks`).
 * 탭의 화면 키가 쌓임에 남아 있으면 화면 모델과 저장 상태가 살아 있음.
 * 그래서 탭을 옮겼다 와도 스크롤, 고른 날, 기간, 필터가 그대로.
 * 앱은 늘 홈을 지나서 닫힘(`contracts/navigation.md` 뒤로 가기)
 */
class TabNavigation(selected: MutableState<TabScreen>, val stacks: Map<TabScreen, MutableList<NavKey>>) {

    var tab: TabScreen by selected
        private set

    /** 지금 탭의 화면들 */
    val current: MutableList<NavKey> get() = stacks.getValue(tab)

    /** 지금 보이는 화면 */
    val top: NavKey? get() = current.lastOrNull()

    /** 뒤로 가면 지나는 탭. 다른 탭은 쓰지 않아도 상태가 그대로 남음 */
    val visibleTabs: List<TabScreen> get() = if (tab == START_TAB) listOf(START_TAB) else listOf(START_TAB, tab)

    /** 방금 움직임이 탭 사이를 옮긴 것인지. 탭을 옮길 때는 두 화면을 겹치지 않고 바로 바꿈 */
    var movedBetweenTabs: Boolean = false
        private set

    /** 첫 화면에서 지금 탭을 다시 누른 횟수. 화면이 듣고 맨 위로 감 */
    var reselected: Int by mutableIntStateOf(0)
        private set

    /**
     * 탭 누름.
     * 다른 탭이면 그 탭에서 보던 화면으로, 지금 탭이면 첫 화면으로 감.
     * 이미 첫 화면이면 맨 위로 감
     */
    fun selectTab(target: TabScreen) {
        movedBetweenTabs = target != tab
        if (target == tab) {
            if (current.size <= 1) reselected++
            current.keepFirst()
        } else {
            tab = target
        }
    }

    /** 다른 곳에서 탭을 열 때는 그 탭 첫 화면에서 시작 */
    fun openTab(target: TabScreen) {
        movedBetweenTabs = target != tab
        tab = target
        current.keepFirst()
    }

    /**
     * 알림에서 [target] 탭의 [key] 화면으로 바로 감.
     * 다른 탭에 쌓인 화면은 그대로 둬 쓰던 일기도 닫히지 않음
     */
    fun openInTab(target: TabScreen, key: NavKey) {
        openTab(target)
        push(key)
    }

    /**
     * 모든 탭을 첫 화면으로 돌리고 [target]을 엶.
     * 복원이 일기를 바꾼 뒤에는 쌓인 상세가 없는 일기를 볼 수 있음
     */
    fun startOver(target: TabScreen) {
        movedBetweenTabs = target != tab
        tab = target
        stacks.values.forEach { it.keepFirst() }
    }

    /** 화면을 엶. 연타로 같은 화면이 두 번 쌓이지 않게 맨 위와 같으면 그대로 둠 */
    fun push(key: NavKey) {
        movedBetweenTabs = false
        // 쓰기 화면 키는 열 때마다 새 값이라 같은지로는 못 막음. 화면이 바뀌는 동안 두 번 눌러도 하나만 엶
        if (top == key || (key is WriteKey && top is WriteKey)) return
        current.add(key)
    }

    /** 뒤로 가기. 탭 첫 화면이면 홈으로, 홈 첫 화면이면 시스템이 앱을 닫음 */
    fun back() {
        movedBetweenTabs = current.size <= 1 && tab != START_TAB
        when {
            current.size > 1 -> current.removeAt(current.lastIndex)
            tab != START_TAB -> tab = START_TAB
        }
    }

    /** 상세를 엶. 넓은 화면에서 다른 줄을 고르면 상세가 쌓이지 않고 바뀜 */
    fun openEntry(entryId: String) {
        movedBetweenTabs = false
        if (top is EntryDetailKey) current.removeAt(current.lastIndex)
        current.add(EntryDetailKey(entryId))
    }

    /** 휴지통으로 옮긴 일기를 모든 탭에서 닫음 */
    fun closeEntry(entryId: String) {
        movedBetweenTabs = false
        stacks.values.forEach { stack ->
            stack.removeAll { (it is EntryDetailKey && it.entryId == entryId) || (it is PhotoViewerKey && it.entryId == entryId) }
        }
    }

    private fun MutableList<NavKey>.keepFirst() {
        while (size > 1) removeAt(lastIndex)
    }
}

/**
 * 앱이 죽었다 살아나도 탭과 화면 쌓임을 복원.
 * 같은 자리에서 여러 번 부르는 저장 상태라 탭마다 `key`로 자리를 가름.
 * 가르지 않으면 쌓임의 화면 모델과 복원 순서가 서로 섞임
 */
@Composable
fun rememberTabNavigation(startTab: TabScreen): TabNavigation {
    val selected = rememberSaveable(stateSaver = TabSaver) { mutableStateOf(startTab) }
    val stacks = TABS.associateWith { tab -> key(tab.toString()) { rememberNavBackStack(BackStackConfiguration, tab) } }
    return remember { TabNavigation(selected, stacks) }
}

/** 탭 쌓임마다 화면 상태를 묶음. 저장 상태와 화면 모델은 탭마다 따로 */
@Composable
fun TabNavigation.decoratedEntries(
    decorator: NavEntryDecorator<NavKey>,
    entryProvider: (NavKey) -> NavEntry<NavKey>
): List<NavEntry<NavKey>> {
    val entries =
        stacks.mapValues { (tab, stack) ->
            key(tab.toString()) {
                rememberDecoratedNavEntries(
                    backStack = stack,
                    entryDecorators =
                        listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            LocalTabViewModelStores.current?.get(tab)?.let { rememberViewModelStoreNavEntryDecorator(it) }
                                ?: rememberViewModelStoreNavEntryDecorator(),
                            decorator
                        ),
                    entryProvider = entryProvider
                )
            }
        }
    return visibleTabs.flatMap { entries.getValue(it) }
}

/**
 * 탭마다 화면 모델 저장소.
 * 잠기면 잠금 관문이 앱 화면을 치우는데, 저장소가 그 안에 있으면 만들던 백업 파일과 복원 사본까지 함께 지워짐.
 * 그래서 잠금 관문 바깥에서 만들어 넘김
 */
@Suppress("CompositionLocalAllowlist") // 잠금 관문 바깥의 저장소를 탭 쌓임까지 건네는 길
val LocalTabViewModelStores = staticCompositionLocalOf<Map<TabScreen, ViewModelStoreProvider>?> { null }

@Composable
fun ProvideTabViewModelStores(content: @Composable () -> Unit) {
    val stores = TABS.associateWith { tab -> key(tab.toString()) { rememberViewModelStoreProvider() } }
    CompositionLocalProvider(LocalTabViewModelStores provides stores, content = content)
}

private val TabSaver = Saver<TabScreen, Int>(save = { TABS.indexOf(it) }, restore = { TABS[it] })
