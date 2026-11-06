package org.sjhstudio.diary.feature.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.component.DiaryButton
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonKind
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonSize
import org.sjhstudio.diary.core.designsystem.component.DiaryIcon
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryArtSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.designsystem.theme.LocalScreenGutter
import org.sjhstudio.diary.feature.welcome.resources.Res
import org.sjhstudio.diary.feature.welcome.resources.onboarding_backup_action
import org.sjhstudio.diary.feature.welcome.resources.onboarding_backup_body
import org.sjhstudio.diary.feature.welcome.resources.onboarding_backup_title
import org.sjhstudio.diary.feature.welcome.resources.onboarding_fresh_title
import org.sjhstudio.diary.feature.welcome.resources.onboarding_later
import org.sjhstudio.diary.feature.welcome.resources.onboarding_next
import org.sjhstudio.diary.feature.welcome.resources.onboarding_own_body
import org.sjhstudio.diary.feature.welcome.resources.onboarding_own_title
import org.sjhstudio.diary.feature.welcome.resources.onboarding_plus_body
import org.sjhstudio.diary.feature.welcome.resources.onboarding_plus_title
import org.sjhstudio.diary.feature.welcome.resources.onboarding_skip
import org.sjhstudio.diary.feature.welcome.resources.onboarding_start
import org.sjhstudio.diary.feature.welcome.resources.onboarding_style_body
import org.sjhstudio.diary.feature.welcome.resources.onboarding_style_title
import org.sjhstudio.diary.feature.welcome.resources.onboarding_trash_body
import org.sjhstudio.diary.feature.welcome.resources.onboarding_trash_title
import org.sjhstudio.diary.feature.welcome.resources.onboarding_wide_body
import org.sjhstudio.diary.feature.welcome.resources.onboarding_wide_title
import org.sjhstudio.diary.feature.welcome.resources.onboarding_write_body
import org.sjhstudio.diary.feature.welcome.resources.onboarding_write_title

private enum class Page { WRITE, OWN, BACKUP, FRESH }

/** 업데이트 사용자는 편지를 읽고 왔으니 할 일과 달라진 점만 본다. */
private val UpdatedPages = listOf(Page.BACKUP, Page.FRESH)

/**
 * 새 사용자에게는 왜 쓰는지와 기록이 어디에 있는지만 말한다.
 * 나머지는 쓰면서 맥락 팁으로 알려 준다.
 */
private val NewPages = listOf(Page.WRITE, Page.OWN)

/**
 * 온보딩(Figma W2, W3).
 * 두 장이고 언제든 건너뛴다.
 * 권한은 묻지 않는다(FR-007, FR-012).
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun OnboardingScreen(
    updated: Boolean,
    connected: Boolean,
    connecting: Boolean,
    onConnect: (next: () -> Unit) -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pages = if (updated) UpdatedPages else NewPages
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == pages.lastIndex
    val next: () -> Unit = {
        if (last) onFinish() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
    }
    BackHandler(enabled = pager.currentPage > 0) { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }
    val colors = DiaryTheme.colors
    Column(modifier.fillMaxSize().background(colors.canvas).statusBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        val asking = pages[pager.currentPage] == Page.BACKUP && !connected
        // 마지막 장은 아래 버튼이 곧 끝이라 건너뛸 것이 없다.
        // 칸은 남겨 그림이 움직이지 않게 한다.
        // 글자 끝을 상단 바와 같은 화면 여백에 맞춘다.
        // 글자 버튼 안쪽이 16이다.
        Box(
            Modifier.fillMaxWidth().heightIn(min = DiarySpacing.x14).padding(end = LocalScreenGutter.current - DiarySpacing.x4),
            contentAlignment = Alignment.CenterEnd
        ) {
            if (asking || !last) {
                DiaryButton(
                    stringResource(if (asking) Res.string.onboarding_later else Res.string.onboarding_skip),
                    if (asking) next else onFinish,
                    kind = DiaryButtonKind.Text
                )
            }
        }
        HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth()) { index -> PageBody(pages[index], active = index == pager.currentPage) }
        Dots(pages.size, pager.currentPage)
        Spacer(Modifier.height(DiarySpacing.x6))
        val label =
            when {
                asking -> Res.string.onboarding_backup_action
                last -> Res.string.onboarding_start
                else -> Res.string.onboarding_next
            }
        DiaryButton(
            stringResource(label),
            { if (asking) onConnect { next() } else next() },
            Modifier
                .widthIn(max = DiaryWriteSize.bodyMax)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = LocalScreenGutter.current, vertical = DiarySpacing.x3),
            size = DiaryButtonSize.XLarge,
            enabled = !connecting
        )
    }
}

@Composable
private fun PageBody(page: Page, active: Boolean) {
    when (page) {
        Page.WRITE -> ArtPage(Res.string.onboarding_write_title, Res.string.onboarding_write_body) { WriteArt(active) }
        Page.OWN -> ArtPage(Res.string.onboarding_own_title, Res.string.onboarding_own_body) { BackupArt(active) }
        Page.BACKUP -> ArtPage(Res.string.onboarding_backup_title, Res.string.onboarding_backup_body) { BackupArt(active) }
        Page.FRESH -> FreshPage()
    }
}

/**
 * 장 내용을 가운데에 둔다.
 * 낮은 화면이나 큰 글자에서 넘치면 장 안에서 스크롤한다.
 */
@Composable
private fun PageColumn(content: @Composable ColumnScope.() -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(horizontal = DiarySpacing.x6),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            content = content
        )
    }
}

/**
 * 그림 하나, 제목 한 줄, 설명.
 * 그림 칸 높이가 같아서 장을 넘겨도 제목이 움직이지 않는다.
 */
@Composable
private fun ArtPage(title: StringResource, body: StringResource, art: @Composable () -> Unit) {
    val colors = DiaryTheme.colors
    PageColumn {
        Box(Modifier.height(DiaryArtSize.height), contentAlignment = Alignment.Center) { art() }
        Spacer(Modifier.height(DiarySpacing.x10))
        BasicText(
            stringResource(title),
            style = DiaryTheme.typography.title1.copy(color = colors.textPrimary, textAlign = TextAlign.Center)
        )
        Spacer(Modifier.height(DiarySpacing.x2))
        BasicText(
            stringResource(body),
            style = DiaryTheme.typography.body.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
            modifier = Modifier.widthIn(max = DiaryWriteSize.bodyMax),
            minLines = 2
        )
    }
}

/**
 * 달라진 점은 한 장에 모아 훑어보게 한다.
 * 넘기는 장을 늘리지 않는다.
 */
@Composable
private fun FreshPage() {
    val colors = DiaryTheme.colors
    PageColumn {
        BasicText(
            stringResource(Res.string.onboarding_fresh_title),
            style = DiaryTheme.typography.title1.copy(color = colors.textPrimary, textAlign = TextAlign.Center)
        )
        Spacer(Modifier.height(DiarySpacing.x8))
        Column(Modifier.widthIn(max = DiaryWriteSize.bodyMax), verticalArrangement = Arrangement.spacedBy(DiarySpacing.x6)) {
            FreshRow(DiaryIcons.TextColor, Res.string.onboarding_style_title, Res.string.onboarding_style_body)
            FreshRow(DiaryIcons.Trash, Res.string.onboarding_trash_title, Res.string.onboarding_trash_body)
            FreshRow(DiaryIcons.Tablet, Res.string.onboarding_wide_title, Res.string.onboarding_wide_body)
            FreshRow(DiaryIcons.Sparkles, Res.string.onboarding_plus_title, Res.string.onboarding_plus_body)
        }
    }
}

@Composable
private fun FreshRow(icon: ImageVector, title: StringResource, body: StringResource) {
    val colors = DiaryTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x4), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(DiaryArtSize.rowBadge).clip(DiaryShapes.full).background(colors.selected),
            contentAlignment = Alignment.Center
        ) {
            DiaryIcon(icon, contentDescription = null, tint = colors.accentText)
        }
        Column(verticalArrangement = Arrangement.spacedBy(DiarySpacing.x0_5)) {
            BasicText(stringResource(title), style = DiaryTheme.typography.bodyStrong.copy(color = colors.textPrimary))
            BasicText(stringResource(body), style = DiaryTheme.typography.caption.copy(color = colors.textSecondary))
        }
    }
}

@Composable
private fun Dots(count: Int, current: Int) {
    val colors = DiaryTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2)) {
        repeat(count) { index ->
            Box(
                Modifier
                    .size(DiarySpacing.x2)
                    .clip(DiaryShapes.full)
                    .background(if (index == current) colors.accent else colors.lineStrong)
            )
        }
    }
}
