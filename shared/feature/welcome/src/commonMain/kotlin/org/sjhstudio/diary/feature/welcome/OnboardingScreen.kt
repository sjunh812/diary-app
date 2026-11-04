package org.sjhstudio.diary.feature.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.BasicText
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
import org.sjhstudio.diary.core.designsystem.component.DiaryBadgeTone
import org.sjhstudio.diary.core.designsystem.component.DiaryButton
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonKind
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonSize
import org.sjhstudio.diary.core.designsystem.component.DiaryHeroBadge
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.feature.welcome.resources.Res
import org.sjhstudio.diary.feature.welcome.resources.onboarding_backup_action
import org.sjhstudio.diary.feature.welcome.resources.onboarding_backup_body
import org.sjhstudio.diary.feature.welcome.resources.onboarding_backup_title
import org.sjhstudio.diary.feature.welcome.resources.onboarding_devices_body
import org.sjhstudio.diary.feature.welcome.resources.onboarding_devices_title
import org.sjhstudio.diary.feature.welcome.resources.onboarding_fresh_body
import org.sjhstudio.diary.feature.welcome.resources.onboarding_fresh_title
import org.sjhstudio.diary.feature.welcome.resources.onboarding_later
import org.sjhstudio.diary.feature.welcome.resources.onboarding_next
import org.sjhstudio.diary.feature.welcome.resources.onboarding_own_body
import org.sjhstudio.diary.feature.welcome.resources.onboarding_own_title
import org.sjhstudio.diary.feature.welcome.resources.onboarding_skip
import org.sjhstudio.diary.feature.welcome.resources.onboarding_start
import org.sjhstudio.diary.feature.welcome.resources.onboarding_write_body
import org.sjhstudio.diary.feature.welcome.resources.onboarding_write_title

private class Page(val icon: ImageVector, val title: StringResource, val body: StringResource, val backup: Boolean = false)

private val UpdatedPages =
    listOf(
        Page(DiaryIcons.ShieldCheck, Res.string.onboarding_backup_title, Res.string.onboarding_backup_body, backup = true),
        Page(DiaryIcons.Pen, Res.string.onboarding_fresh_title, Res.string.onboarding_fresh_body)
    )

private val NewPages =
    listOf(
        Page(DiaryIcons.Pen, Res.string.onboarding_write_title, Res.string.onboarding_write_body),
        Page(DiaryIcons.ShieldCheck, Res.string.onboarding_own_title, Res.string.onboarding_own_body),
        Page(DiaryIcons.BookOpen, Res.string.onboarding_devices_title, Res.string.onboarding_devices_body)
    )

/** 온보딩(Figma W2, W3). 많아야 세 장이고 언제든 건너뛴다. 권한은 묻지 않는다(FR-007, FR-012). */
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
    val page = pages[pager.currentPage]
    val last = pager.currentPage == pages.lastIndex
    val next: () -> Unit = {
        if (last) onFinish() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
    }
    BackHandler(enabled = pager.currentPage > 0) { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }
    val colors = DiaryTheme.colors
    Column(modifier.fillMaxSize().background(colors.canvas).statusBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        val asking = page.backup && !connected
        Box(Modifier.fillMaxWidth().padding(horizontal = DiarySpacing.x2), contentAlignment = Alignment.CenterEnd) {
            DiaryButton(
                stringResource(if (asking) Res.string.onboarding_later else Res.string.onboarding_skip),
                if (asking) next else onFinish,
                kind = DiaryButtonKind.Text
            )
        }
        HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth()) { index -> PageBody(pages[index]) }
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
                .padding(horizontal = DiarySpacing.screenCompact, vertical = DiarySpacing.x3),
            size = DiaryButtonSize.XLarge,
            enabled = !connecting
        )
    }
}

@Composable
private fun PageBody(page: Page) {
    val colors = DiaryTheme.colors
    Column(
        Modifier.fillMaxSize().padding(horizontal = DiarySpacing.x6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x2, Alignment.CenterVertically)
    ) {
        DiaryHeroBadge(page.icon, tone = DiaryBadgeTone.Accent)
        Spacer(Modifier.height(DiarySpacing.x1))
        BasicText(
            stringResource(page.title),
            style = DiaryTheme.typography.title2.copy(color = colors.textPrimary, textAlign = TextAlign.Center)
        )
        BasicText(
            stringResource(page.body),
            style = DiaryTheme.typography.body.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
            modifier = Modifier.widthIn(max = DiaryWriteSize.bodyMax)
        )
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
                    .background(if (index == current) colors.accent else colors.line)
            )
        }
    }
}
