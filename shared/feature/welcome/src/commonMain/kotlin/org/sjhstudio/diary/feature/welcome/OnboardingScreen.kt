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
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.component.DiaryButton
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonKind
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryArtSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.designsystem.theme.LocalScreenGutter
import org.sjhstudio.diary.feature.welcome.resources.Res
import org.sjhstudio.diary.feature.welcome.resources.onboarding_backup_body
import org.sjhstudio.diary.feature.welcome.resources.onboarding_backup_title
import org.sjhstudio.diary.feature.welcome.resources.onboarding_find_body
import org.sjhstudio.diary.feature.welcome.resources.onboarding_find_title
import org.sjhstudio.diary.feature.welcome.resources.onboarding_look_back_body
import org.sjhstudio.diary.feature.welcome.resources.onboarding_look_back_title
import org.sjhstudio.diary.feature.welcome.resources.onboarding_next
import org.sjhstudio.diary.feature.welcome.resources.onboarding_own_body
import org.sjhstudio.diary.feature.welcome.resources.onboarding_own_title
import org.sjhstudio.diary.feature.welcome.resources.onboarding_skip
import org.sjhstudio.diary.feature.welcome.resources.onboarding_start
import org.sjhstudio.diary.feature.welcome.resources.onboarding_style_body
import org.sjhstudio.diary.feature.welcome.resources.onboarding_style_title
import org.sjhstudio.diary.feature.welcome.resources.onboarding_tablet_body
import org.sjhstudio.diary.feature.welcome.resources.onboarding_tablet_title
import org.sjhstudio.diary.feature.welcome.resources.onboarding_trash_body
import org.sjhstudio.diary.feature.welcome.resources.onboarding_trash_title
import org.sjhstudio.diary.feature.welcome.resources.onboarding_write_body
import org.sjhstudio.diary.feature.welcome.resources.onboarding_write_title

/** 한 장에 그림 하나, 제목 한 줄, 설명 두 줄. 여러 기능을 목록으로 몰지 않음 */
private enum class Page(val title: StringResource, val body: StringResource) {
    BACKUP(Res.string.onboarding_backup_title, Res.string.onboarding_backup_body),
    STYLE(Res.string.onboarding_style_title, Res.string.onboarding_style_body),
    FIND(Res.string.onboarding_find_title, Res.string.onboarding_find_body),
    TRASH(Res.string.onboarding_trash_title, Res.string.onboarding_trash_body),
    TABLET(Res.string.onboarding_tablet_title, Res.string.onboarding_tablet_body),
    WRITE(Res.string.onboarding_write_title, Res.string.onboarding_write_body),
    LOOK_BACK(Res.string.onboarding_look_back_title, Res.string.onboarding_look_back_body),
    OWN(Res.string.onboarding_own_title, Res.string.onboarding_own_body)
}

/**
 * 업데이트 사용자는 쓰는 법 대신 달라진 점만.
 * 백업은 홈 백업 줄에서 켜므로 버튼 없이 좋아진 점만. 태블릿은 폰에서도 보여 있는 줄 알게 함
 */
private val UpdatedPages = listOf(Page.BACKUP, Page.STYLE, Page.FIND, Page.TRASH, Page.TABLET)

/** 새 사용자는 무엇을 하는 앱인지, 무엇을 얻는지, 기록이 어디 있는지 */
private val NewPages = listOf(Page.WRITE, Page.LOOK_BACK, Page.OWN, Page.TABLET)

/** 온보딩(Figma W2, W3). 언제든 건너뜀. 권한과 연결은 묻지 않음(FR-007, FR-012) */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun OnboardingScreen(updated: Boolean, onFinish: () -> Unit, modifier: Modifier = Modifier) {
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
        // 마지막 장은 아래 버튼이 곧 끝이라 건너뛸 것이 없음
        // 칸은 남겨 그림이 움직이지 않게 함
        // 글자 끝을 상단 바와 같은 화면 여백에 맞춤
        // 글자 버튼 안쪽이 16
        Box(
            Modifier.fillMaxWidth().heightIn(min = DiarySpacing.x14).padding(end = LocalScreenGutter.current - DiarySpacing.x4),
            contentAlignment = Alignment.CenterEnd
        ) {
            if (!last) DiaryButton(stringResource(Res.string.onboarding_skip), onFinish, kind = DiaryButtonKind.Text)
        }
        HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth()) { index -> PageBody(pages[index], active = index == pager.currentPage) }
        Dots(pages.size, pager.currentPage)
        Spacer(Modifier.height(DiarySpacing.x6))
        DiaryButton(
            stringResource(if (last) Res.string.onboarding_start else Res.string.onboarding_next),
            next,
            Modifier
                .widthIn(max = DiaryWriteSize.bodyMax)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = LocalScreenGutter.current, vertical = DiarySpacing.x3),
            size = DiaryButtonSize.XLarge
        )
    }
}

@Composable
private fun PageBody(page: Page, active: Boolean) {
    ArtPage(page.title, page.body) {
        when (page) {
            Page.BACKUP, Page.OWN -> BackupArt(active)
            Page.WRITE -> WriteArt(active)
            Page.STYLE -> StyleArt(active)
            Page.FIND, Page.LOOK_BACK -> FindArt(active)
            Page.TRASH -> TrashArt(active)
            Page.TABLET -> TabletArt(active)
        }
    }
}

/**
 * 장 내용을 가운데에 둠.
 * 낮은 화면이나 큰 글자에서 넘치면 장 안에서 스크롤.
 */
@Composable
private fun PageColumn(content: @Composable ColumnScope.() -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(horizontal = LocalScreenGutter.current),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            content = content
        )
    }
}

/**
 * 그림 하나, 제목 한 줄, 설명.
 * 그림 칸 높이가 같아 장을 넘겨도 제목이 움직이지 않음.
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

@Composable
private fun Dots(count: Int, current: Int) {
    val colors = DiaryTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2)) {
        repeat(count) { index ->
            Box(
                Modifier
                    .size(DiarySpacing.x2)
                    .clip(DiaryShapes.full)
                    .background(if (index == current) colors.textPrimary else colors.textTertiary)
            )
        }
    }
}
