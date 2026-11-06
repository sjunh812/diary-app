package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.limitFontScale

/**
 * 하단 탭·레일 한 칸.
 * 고른 칸은 [selectedIcon](굵은 선)을 쓴다.
 */
class DiaryNavItem(val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

/** 이 폭부터 하단 탭 대신 레일을 쓴다(`docs/design.md` 넓은 화면, M3 medium 600dp). */
val DiaryRailMinWidth: Dp = 600.dp

/**
 * 하단 탭(`docs/design.md` 하단 탭, M3 내비 바 64, Figma "설정 (S6)" tab-bar).
 * 64 + 안전 영역 · 칸은 폭을 고르게 나누고 높이 56 · 아이콘 24 · 이름 `micro` · 위 경계선 `line`.
 * 고른 탭은 보라가 아니라 `textPrimary`와 굵은 선, 나머지는 `textTertiary`다.
 */
@Composable
fun DiaryNavigationBar(items: List<DiaryNavItem>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = DiaryTheme.colors
    Column(modifier.fillMaxWidth().background(colors.canvas)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().height(
                64.dp
            ).padding(start = DiarySpacing.x3, end = DiarySpacing.x3, top = 8.dp),
            verticalAlignment = Alignment.Top
        ) {
            items.forEachIndexed { index, item ->
                NavCell(item, index == selected, Modifier.weight(1f).height(56.dp)) { onSelect(index) }
            }
        }
    }
}

/**
 * 레일(600dp 이상, `docs/design.md` 레일, M3 narrow rail 80).
 * 칸 80×64, 오른쪽 경계선 `line`.
 * 맨 위 쓰기 버튼 자리는 스펙 002가 [header]로 채운다.
 * 버튼 아래 여백은 부르는 쪽이 정한다.
 * 낮은 가로 화면에서 칸이 넘치면 스크롤한다.
 */
@Composable
fun DiaryNavigationRail(
    items: List<DiaryNavItem>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit = {}
) {
    val colors = DiaryTheme.colors
    Row(modifier.fillMaxHeight().background(colors.canvas)) {
        Column(
            Modifier.width(80.dp).fillMaxHeight().systemBarsPadding().verticalScroll(rememberScrollState()).padding(top = DiarySpacing.x5),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DiarySpacing.x1)
        ) {
            header()
            items.forEachIndexed { index, item ->
                NavCell(item, index == selected, Modifier.width(80.dp).height(64.dp)) { onSelect(index) }
            }
        }
        Box(Modifier.width(1.dp).fillMaxHeight().background(colors.line))
    }
}

@Composable
private fun NavCell(item: DiaryNavItem, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = DiaryTheme.colors
    val tint = if (selected) colors.textPrimary else colors.textTertiary
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier =
            modifier
                .diaryPress(interaction, colors.textPrimary, DiaryShapes.r16)
                .selectable(selected = selected, role = Role.Tab, interactionSource = interaction, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        DiaryIcon(if (selected) item.selectedIcon else item.icon, contentDescription = null, tint = tint)
        Spacer(Modifier.height(DiarySpacing.x1))
        // 이름은 기기 글자 크기를 1.3배까지만 따른다.
        // 더 키우면 칸 80 안에서 잘린다(DECISIONS 2026-11-03).
        BasicText(item.label, style = DiaryTheme.typography.micro.limitFontScale().copy(color = tint), maxLines = 1)
    }
}
