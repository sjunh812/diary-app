package org.sjhstudio.diary.feature.plus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.sjhstudio.diary.core.designsystem.component.DiaryBottomSheet
import org.sjhstudio.diary.core.designsystem.component.DiaryButton
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonKind
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonSize
import org.sjhstudio.diary.core.designsystem.component.DiaryIcon
import org.sjhstudio.diary.core.designsystem.component.DiaryIconButton
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsRow
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsValueRow
import org.sjhstudio.diary.core.designsystem.component.DiaryTag
import org.sjhstudio.diary.core.designsystem.component.DiaryToastHost
import org.sjhstudio.diary.core.designsystem.component.DiaryToastState
import org.sjhstudio.diary.core.designsystem.component.DiaryTopBar
import org.sjhstudio.diary.core.designsystem.component.PhraseText
import org.sjhstudio.diary.core.designsystem.component.diaryScrollFade
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.CenteredLineBreak
import org.sjhstudio.diary.core.designsystem.theme.DiaryArtSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryIconSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.designsystem.theme.LocalScreenGutter
import org.sjhstudio.diary.core.designsystem.theme.diaryLanguage
import org.sjhstudio.diary.core.model.Plan
import org.sjhstudio.diary.core.model.ProductId
import org.sjhstudio.diary.core.ui.DiaryLottie
import org.sjhstudio.diary.core.ui.LegalLinks
import org.sjhstudio.diary.core.ui.rememberOpenWebLink
import org.sjhstudio.diary.feature.plus.resources.Res
import org.sjhstudio.diary.feature.plus.resources.plus_ad_free_owned
import org.sjhstudio.diary.feature.plus.resources.plus_ads_body
import org.sjhstudio.diary.feature.plus.resources.plus_ads_only
import org.sjhstudio.diary.feature.plus.resources.plus_ads_title
import org.sjhstudio.diary.feature.plus.resources.plus_all_title
import org.sjhstudio.diary.feature.plus.resources.plus_checklist_body
import org.sjhstudio.diary.feature.plus.resources.plus_checklist_title
import org.sjhstudio.diary.feature.plus.resources.plus_close
import org.sjhstudio.diary.feature.plus.resources.plus_coming
import org.sjhstudio.diary.feature.plus.resources.plus_failed
import org.sjhstudio.diary.feature.plus.resources.plus_format_body
import org.sjhstudio.diary.feature.plus.resources.plus_format_title
import org.sjhstudio.diary.feature.plus.resources.plus_manage_benefits
import org.sjhstudio.diary.feature.plus.resources.plus_manage_note
import org.sjhstudio.diary.feature.plus.resources.plus_manage_price
import org.sjhstudio.diary.feature.plus.resources.plus_manage_store
import org.sjhstudio.diary.feature.plus.resources.plus_manage_store_body
import org.sjhstudio.diary.feature.plus.resources.plus_manage_thanks
import org.sjhstudio.diary.feature.plus.resources.plus_manage_title
import org.sjhstudio.diary.feature.plus.resources.plus_manage_top
import org.sjhstudio.diary.feature.plus.resources.plus_mood_theme_body
import org.sjhstudio.diary.feature.plus.resources.plus_mood_theme_title
import org.sjhstudio.diary.feature.plus.resources.plus_new_tag
import org.sjhstudio.diary.feature.plus.resources.plus_no_store
import org.sjhstudio.diary.feature.plus.resources.plus_notice
import org.sjhstudio.diary.feature.plus.resources.plus_others_in_all
import org.sjhstudio.diary.feature.plus.resources.plus_pending
import org.sjhstudio.diary.feature.plus.resources.plus_photos_body
import org.sjhstudio.diary.feature.plus.resources.plus_photos_title
import org.sjhstudio.diary.feature.plus.resources.plus_price
import org.sjhstudio.diary.feature.plus.resources.plus_privacy
import org.sjhstudio.diary.feature.plus.resources.plus_restore
import org.sjhstudio.diary.feature.plus.resources.plus_restore_failed
import org.sjhstudio.diary.feature.plus.resources.plus_restore_none
import org.sjhstudio.diary.feature.plus.resources.plus_restored
import org.sjhstudio.diary.feature.plus.resources.plus_retry
import org.sjhstudio.diary.feature.plus.resources.plus_see_all
import org.sjhstudio.diary.feature.plus.resources.plus_start
import org.sjhstudio.diary.feature.plus.resources.plus_start_price
import org.sjhstudio.diary.feature.plus.resources.plus_stats_body
import org.sjhstudio.diary.feature.plus.resources.plus_stats_title
import org.sjhstudio.diary.feature.plus.resources.plus_terms
import org.sjhstudio.diary.feature.plus.resources.plus_theme_body
import org.sjhstudio.diary.feature.plus.resources.plus_theme_title
import org.sjhstudio.diary.feature.plus.resources.plus_title

// 혜택 하나에 1.5초씩 넷과 끝 배지(210프레임)
private const val ART_MILLIS = 7_000
private const val ART_RATIO = 350f / 220f
private const val ART_TINT = 0.4f

/**
 * 설정 초대 카드와 뱃지, 사진 한도, 고급 통계 버튼이 여는 화면
 * 이미 Plus면 관리 화면(US3)
 */
@Composable
fun PlusRoute(onClose: () -> Unit, modifier: Modifier = Modifier, from: PlusBenefit? = null, appVersion: String = "") {
    val newBenefits = remember(appVersion) { newBenefitsIn(appVersion) }
    val viewModel = koinViewModel<PlusViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val plan by viewModel.plan.collectAsStateWithLifecycle()
    val toast = remember { DiaryToastState() }
    val scope = rememberCoroutineScope()
    val failed = stringResource(Res.string.plus_failed)
    val pending = stringResource(Res.string.plus_pending)
    val restored = stringResource(Res.string.plus_restored)
    val none = stringResource(Res.string.plus_restore_none)
    val restoreFailed = stringResource(Res.string.plus_restore_failed)
    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        viewModel.messageShown()
        scope.launch {
            toast.show(
                when (message) {
                    PlusMessage.Failed -> failed
                    PlusMessage.Pending -> pending
                    PlusMessage.Restored -> restored
                    PlusMessage.RestoreNone -> none
                    PlusMessage.RestoreFailed -> restoreFailed
                }
            )
        }
    }
    val openWeb = rememberOpenWebLink(toast)
    // 결제 화면의 아래 결제 칸 높이
    // 토스트가 버튼과 결제액을 가리지 않게 그 위에 띄움
    var buyHeight by remember { mutableIntStateOf(0) }
    var adFreeSheet by rememberSaveable { mutableStateOf(false) }
    var allBenefits by rememberSaveable { mutableStateOf(false) }
    Box(modifier.fillMaxSize()) {
        if (plan == Plan.PLUS) {
            PlusManageScreen(
                state.price(ProductId.PLUS_YEARLY)?.label(),
                onClose,
                onStore = { openWeb(viewModel.manageUrl) },
                onSeeAll = { allBenefits = true }
            )
        } else {
            PaywallScreen(
                price = state.price(ProductId.PLUS_YEARLY)?.label(),
                loading = state.loading,
                busy = state.busy,
                adFreeOwned = plan == Plan.AD_FREE,
                onClose = onClose,
                onRestore = { viewModel.restore() },
                onRetry = viewModel::load,
                onBuy = { viewModel.buy(ProductId.PLUS_YEARLY) },
                onBuyHeight = { buyHeight = it },
                onAdFree = { adFreeSheet = true },
                onLink = openWeb,
                from = from,
                newBenefits = newBenefits,
                onSeeAll = { allBenefits = true }
            )
        }
        val fixedBottom = if (plan == Plan.PLUS) Dp.Hairline else with(LocalDensity.current) { buyHeight.toDp() }
        DiaryToastHost(
            toast,
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = fixedBottom + DiarySpacing.x4)
        )
    }
    if (adFreeSheet) AdFreeSheet(onDismiss = { adFreeSheet = false })
    if (allBenefits) AllBenefitsSheet(onDismiss = { allBenefits = false }, newBenefits = newBenefits)
}

/**
 * 결제 화면(Figma P1)
 * 제목, 혜택 네 줄(무엇을 얻는지 한 문장씩), 결제액을 적은 버튼
 * 그림은 남는 자리에만 그리고, 버튼은 아래에 고정
 * 닫기는 처음부터 보이고 실제 결제액이 가장 큼(FR-005)
 * 작은 화면이나 큰 글자에서만 위쪽이 스크롤됨
 * 넓은 화면도 휴대폰과 같은 한 칸
 * 혜택과 가격이 한눈에 이어지게 가운데에 모음
 */
@Composable
internal fun PaywallScreen(
    price: String?,
    loading: Boolean,
    busy: Boolean,
    adFreeOwned: Boolean,
    onClose: () -> Unit,
    onRestore: () -> Unit,
    onRetry: () -> Unit,
    onBuy: () -> Unit,
    modifier: Modifier = Modifier,
    onBuyHeight: (Int) -> Unit = {},
    onAdFree: () -> Unit = {},
    onLink: (String) -> Unit = {},
    from: PlusBenefit? = null,
    newBenefits: Set<PlusBenefit> = emptySet(),
    onSeeAll: () -> Unit = {}
) {
    val colors = DiaryTheme.colors
    BoxWithConstraints(modifier.fillMaxSize().background(colors.canvas).statusBarsPadding().navigationBarsPadding()) {
        val wide = maxWidth >= DiaryWriteSize.twoPaneMin
        // 넓은 화면의 결제 칸은 가운데에 있어 화면 아래에 고정된 것이 없음
        LaunchedEffect(wide) { if (wide) onBuyHeight(0) }
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = DiarySpacing.x14).padding(horizontal = DiarySpacing.x1),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DiaryIconButton(DiaryIcons.Close, stringResource(Res.string.plus_close), onClose)
                Spacer(Modifier.weight(1f))
                DiaryButton(
                    stringResource(Res.string.plus_restore),
                    onRestore,
                    Modifier.padding(end = DiarySpacing.x0_5),
                    kind = DiaryButtonKind.QuietText,
                    size = DiaryButtonSize.Header
                )
            }
            val column = Modifier.widthIn(max = DiaryArtSize.paywallMax).fillMaxWidth().padding(horizontal = LocalScreenGutter.current)
            if (wide) {
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                    Column(
                        column.verticalScroll(rememberScrollState()).heightIn(min = maxHeight).padding(bottom = DiarySpacing.x4),
                        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x8, Alignment.CenterVertically)
                    ) {
                        PlusArt(DiaryArtSize.banner)
                        PaywallBenefits(adFreeOwned, from, newBenefits, onSeeAll)
                        PaywallBuy(price, loading, busy, onRetry, onBuy, adFreeOwned, onAdFree, onLink, Modifier.fillMaxWidth())
                    }
                }
            } else {
                PaywallTop(adFreeOwned, from, newBenefits, onSeeAll, column.weight(1f))
                // 약관 링크는 자동 갱신 고지 바로 아래(스펙 008 FR-021)
                PaywallBuy(
                    price,
                    loading,
                    busy,
                    onRetry,
                    onBuy,
                    adFreeOwned,
                    onAdFree,
                    onLink = onLink,
                    column.onSizeChanged { onBuyHeight(it.height) }.padding(top = DiarySpacing.x1_5, bottom = DiarySpacing.x3)
                )
            }
        }
    }
}

/**
 * 그림 칸
 * 그림은 칸에 맞춰 통째로 줄고 늘어 가운데에 놓임. 칸 높이(120~280, 폭 350:220까지)가 달라도 잘리거나 한쪽이 비지 않게(스펙 010)
 * 결제 화면과 구독 중 화면이 같이 씀
 */
@Composable
private fun PlusArt(height: Dp, modifier: Modifier = Modifier) {
    val colors = DiaryTheme.colors
    BoxWithConstraints(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(minOf(height, maxWidth / ART_RATIO))
                .clip(DiaryShapes.r20)
                .background(colors.selected.copy(alpha = ART_TINT))
        ) {
            DiaryLottie(
                { Res.readBytes("files/plus_benefits.json") },
                ART_MILLIS,
                active = true,
                Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * 그림과 혜택
 * 그림은 늘 보임. 글을 놓고 남는 높이만큼 키우고, 자리가 모자라면 최소 높이로 두고 내용이 스크롤됨
 */
@Composable
private fun PaywallTop(adFreeOwned: Boolean, from: PlusBenefit?, newBenefits: Set<PlusBenefit>, onSeeAll: () -> Unit, modifier: Modifier) {
    BoxWithConstraints(modifier) {
        val room = maxHeight
        val scroll = rememberScrollState()
        // 짧은 화면에서는 혜택 아래가 가려짐. 끝을 흐려 더 있다는 것을 보임
        SubcomposeLayout(Modifier.fillMaxWidth().diaryScrollFade(scroll, DiaryTheme.colors.canvas).verticalScroll(scroll)) { constraints ->
            val width = constraints.copy(minHeight = 0)
            val text = subcompose("text") { PaywallBenefits(adFreeOwned, from, newBenefits, onSeeAll) }.map { it.measure(width) }
            val textHeight = text.sumOf { it.height }
            val gap = DiarySpacing.x6.roundToPx()
            val top = DiarySpacing.x2.roundToPx()
            val left = room.roundToPx() - top - gap - textHeight
            val height = left.coerceIn(DiaryArtSize.paywallArtMin.roundToPx(), DiaryArtSize.banner.roundToPx()).toDp()
            val art = subcompose("art") { PlusArt(height) }.map { it.measure(width) }
            val artHeight = art.sumOf { it.height } + gap
            layout(constraints.maxWidth, maxOf(artHeight + textHeight, room.roundToPx())) {
                art.forEach { it.placeRelative(0, 0) }
                text.forEach { it.placeRelative(0, artHeight) }
            }
        }
    }
}

/**
 * 제목과 혜택
 * 핵심은 설명까지, 다른 혜택은 [모두 보기] 안내 한 줄(스펙 008 FR-018)
 * [모두 보기]는 모든 혜택을 설명과 함께 시트로 보임
 */
@Composable
private fun PaywallBenefits(adFreeOwned: Boolean, from: PlusBenefit?, newBenefits: Set<PlusBenefit>, onSeeAll: () -> Unit) {
    val colors = DiaryTheme.colors
    val typography = DiaryTheme.typography
    Column(Modifier.fillMaxWidth()) {
        BenefitsHeader(stringResource(Res.string.plus_title), typography.title1.copy(color = colors.textPrimary), onSeeAll)
        Spacer(Modifier.height(DiarySpacing.x6))
        CoreBenefits()
        enteredOther(from)?.let {
            Spacer(Modifier.height(DiarySpacing.x6))
            OtherBenefit(it, new = it in newBenefits)
        }
        Spacer(Modifier.height(DiarySpacing.x6))
        // 다른 혜택은 목록 대신 [모두 보기]로 이끔
        PhraseText(stringResource(Res.string.plus_others_in_all), style = typography.caption.copy(color = colors.textSecondary))
        if (adFreeOwned) {
            Spacer(Modifier.height(DiarySpacing.x3))
            PhraseText(stringResource(Res.string.plus_ad_free_owned), style = typography.caption.copy(color = colors.textSecondary))
        }
    }
}

@Composable
private fun PaywallLegal(onLink: (String) -> Unit) {
    val language = diaryLanguage()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        DiaryButton(
            stringResource(Res.string.plus_terms),
            { onLink(LegalLinks.terms(language)) },
            kind = DiaryButtonKind.QuietText,
            size = DiaryButtonSize.Small
        )
        DiaryButton(
            stringResource(Res.string.plus_privacy),
            { onLink(LegalLinks.privacy(language)) },
            kind = DiaryButtonKind.QuietText,
            size = DiaryButtonSize.Small
        )
    }
}

/** 묶음 머리와 오른쪽 [모두 보기] */
@Composable
private fun BenefitsHeader(title: String, style: TextStyle, onSeeAll: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        PhraseText(title, modifier = Modifier.weight(1f), style = style)
        DiaryButton(stringResource(Res.string.plus_see_all), onSeeAll, kind = DiaryButtonKind.Text, size = DiaryButtonSize.Header)
    }
}

/** 들어온 다른 혜택 한 줄. 핵심보다 가볍게 체크와 이름만 */
@Composable
private fun OtherBenefit(benefit: PlusBenefit, new: Boolean) {
    val colors = DiaryTheme.colors
    Row(
        Modifier.padding(start = DiarySpacing.x2),
        horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DiaryIcon(DiaryIcons.Check, contentDescription = null, tint = colors.accentText, size = DiaryIconSize.i20)
        BenefitName(benefit, DiaryTheme.typography.body.copy(color = colors.textPrimary), new)
    }
}

/** 혜택 이름. 새로 나온 혜택은 옆에 회색 "새 기능" 꼬리표 */
@Composable
private fun BenefitName(benefit: PlusBenefit, style: TextStyle, new: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x1_5), verticalAlignment = Alignment.CenterVertically) {
        PhraseText(stringResource(benefit.title), style = style)
        if (new) DiaryTag(stringResource(Res.string.plus_new_tag))
    }
}

/**
 * 실제 결제액, 버튼, 자동 갱신 고지(FR-005)
 * 가격을 못 받으면 지어내지 않고 다시 시도를 보임
 * 읽는 동안에는 결제액 자리를 비워 두고 버튼을 비활성으로 두어 칸이 뛰지 않음
 */
@Composable
private fun PaywallBuy(
    price: String?,
    loading: Boolean,
    busy: Boolean,
    onRetry: () -> Unit,
    onBuy: () -> Unit,
    adFreeOwned: Boolean,
    onAdFree: () -> Unit,
    onLink: ((String) -> Unit)?,
    modifier: Modifier
) {
    val colors = DiaryTheme.colors
    val typography = DiaryTheme.typography
    // 혜택이 먼저 보이게 아래 칸은 촘촘하게. 결제 버튼, 다른 선택, 고지가 한 묶음
    Column(modifier, verticalArrangement = Arrangement.spacedBy(DiarySpacing.x2)) {
        if (price != null || loading) {
            DiaryButton(
                // 요금이 하나뿐이라 고르는 칸 없이 버튼에 바로 적음
                price?.let { stringResource(Res.string.plus_start_price, it) } ?: stringResource(Res.string.plus_start),
                onBuy,
                Modifier.fillMaxWidth(),
                size = DiaryButtonSize.XLarge,
                enabled = price != null && !busy
            )
            // 광고의 ×를 찾지 않아도 살 수 있게 둠
            // 결제 버튼 바로 아래에 두어 다른 선택지로 읽히게 하고, 이미 산 사람에게는 보이지 않음
            if (!adFreeOwned) {
                DiaryButton(
                    stringResource(Res.string.plus_ads_only),
                    onAdFree,
                    Modifier.align(Alignment.CenterHorizontally),
                    size = DiaryButtonSize.Header,
                    kind = DiaryButtonKind.Text
                )
            }
            PhraseText(
                stringResource(Res.string.plus_notice),
                modifier = Modifier.fillMaxWidth(),
                style = typography.micro.copy(color = colors.textSecondary, textAlign = TextAlign.Center, lineBreak = CenteredLineBreak)
            )
            onLink?.let { PaywallLegal(it) }
        } else {
            PhraseText(stringResource(Res.string.plus_no_store), style = typography.body.copy(color = colors.textSecondary))
            DiaryButton(stringResource(Res.string.plus_retry), onRetry, Modifier.fillMaxWidth(), size = DiaryButtonSize.XLarge)
        }
    }
}

/** 핵심 혜택. 결제 화면과 구독 중 화면이 같은 말로 보임 */
@Composable
private fun CoreBenefits() {
    Column(verticalArrangement = Arrangement.spacedBy(DiarySpacing.x4)) {
        coreBenefits.forEach { OwnedBenefit(it) }
    }
}

/** 모든 혜택. 묶음 머리 없이 핵심 → 다른 혜택(스펙 008 FR-019) */
@Composable
internal fun AllBenefitsSheet(onDismiss: () -> Unit, newBenefits: Set<PlusBenefit> = emptySet()) {
    DiaryBottomSheet(onDismiss, title = stringResource(Res.string.plus_all_title)) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(DiarySpacing.x5)) {
            PlusBenefit.entries.forEach { OwnedBenefit(it, new = it in newBenefits) }
        }
    }
}

/**
 * 이미 Plus인 사람의 화면(Figma P4)
 * 권유하지 않고, 구독해 준 사람에게 지금 받는 것과 요금, 관리할 곳을 한곳에 보임
 * 요금은 스토어에서 받은 값만 쓰고 받지 못하면 줄을 뺌
 * 다음 결제일과 해지는 스토어가 맡음(HIG 인앱 구매)
 */
@Composable
internal fun PlusManageScreen(
    price: String?,
    onBack: () -> Unit,
    onStore: () -> Unit,
    modifier: Modifier = Modifier,
    onSeeAll: () -> Unit = {}
) {
    val colors = DiaryTheme.colors
    val typography = DiaryTheme.typography
    Column(modifier.fillMaxSize().background(colors.canvas).statusBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        DiaryTopBar(stringResource(Res.string.plus_manage_top), onBack = onBack)
        Column(
            Modifier
                .widthIn(max = DiaryWriteSize.bodyMax)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(top = DiarySpacing.x2, bottom = DiarySpacing.x6)
        ) {
            val gutter = Modifier.padding(horizontal = LocalScreenGutter.current)
            Column(gutter) {
                PlusArt(DiaryArtSize.card)
                Spacer(Modifier.height(DiarySpacing.x4))
                PhraseText(stringResource(Res.string.plus_manage_title), style = typography.title1.copy(color = colors.textPrimary))
                Spacer(Modifier.height(DiarySpacing.x1))
                PhraseText(stringResource(Res.string.plus_manage_thanks), style = typography.body.copy(color = colors.textSecondary))
                Spacer(Modifier.height(DiarySpacing.x6))
                BenefitsHeader(
                    stringResource(Res.string.plus_manage_benefits),
                    typography.label.copy(color = colors.textSecondary),
                    onSeeAll
                )
                Spacer(Modifier.height(DiarySpacing.x2))
                CoreBenefits()
                Spacer(Modifier.height(DiarySpacing.x4))
                PhraseText(stringResource(Res.string.plus_coming), style = typography.caption.copy(color = colors.textSecondary))
            }
            Spacer(Modifier.height(DiarySpacing.x4))
            if (price != null) {
                DiarySettingsValueRow(stringResource(Res.string.plus_manage_price), stringResource(Res.string.plus_price, price))
            }
            DiarySettingsRow(
                stringResource(Res.string.plus_manage_store),
                onClick = onStore,
                description = stringResource(Res.string.plus_manage_store_body)
            )
            PhraseText(
                stringResource(Res.string.plus_manage_note),
                modifier = gutter.padding(top = DiarySpacing.x2),
                style = typography.caption.copy(color = colors.textSecondary)
            )
        }
    }
}

/**
 * 혜택 한 줄
 * 무엇을 받는지 한 문장으로 덧붙임
 */
@Composable
private fun OwnedBenefit(benefit: PlusBenefit, new: Boolean = false) {
    val colors = DiaryTheme.colors
    val typography = DiaryTheme.typography
    Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x3), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(DiarySpacing.x10).clip(DiaryShapes.full).background(colors.selected),
            contentAlignment = Alignment.Center
        ) {
            DiaryIcon(benefit.icon, contentDescription = null, tint = colors.accentText, size = DiaryIconSize.i20)
        }
        Column(verticalArrangement = Arrangement.spacedBy(DiarySpacing.x0_5)) {
            BenefitName(benefit, typography.bodyStrong.copy(color = colors.textPrimary), new)
            PhraseText(stringResource(benefit.body), style = typography.caption.copy(color = colors.textSecondary))
        }
    }
}

private val PlusBenefit.icon: ImageVector
    get() = when (this) {
        PlusBenefit.AD_FREE -> DiaryIcons.ShieldCheck
        PlusBenefit.PHOTOS -> DiaryIcons.Image
        PlusBenefit.THEME -> DiaryIcons.Palette
        PlusBenefit.STATS -> DiaryIcons.TabInsight
        PlusBenefit.CHECKLIST -> DiaryIcons.Checklist
        PlusBenefit.LIST_FORMATTING -> DiaryIcons.TextColor
        PlusBenefit.MOOD_THEME -> DiaryIcons.Smile
    }

private val PlusBenefit.title: StringResource
    get() = when (this) {
        PlusBenefit.AD_FREE -> Res.string.plus_ads_title
        PlusBenefit.PHOTOS -> Res.string.plus_photos_title
        PlusBenefit.THEME -> Res.string.plus_theme_title
        PlusBenefit.STATS -> Res.string.plus_stats_title
        PlusBenefit.CHECKLIST -> Res.string.plus_checklist_title
        PlusBenefit.LIST_FORMATTING -> Res.string.plus_format_title
        PlusBenefit.MOOD_THEME -> Res.string.plus_mood_theme_title
    }

private val PlusBenefit.body: StringResource
    get() = when (this) {
        PlusBenefit.AD_FREE -> Res.string.plus_ads_body
        PlusBenefit.PHOTOS -> Res.string.plus_photos_body
        PlusBenefit.THEME -> Res.string.plus_theme_body
        PlusBenefit.STATS -> Res.string.plus_stats_body
        PlusBenefit.CHECKLIST -> Res.string.plus_checklist_body
        PlusBenefit.LIST_FORMATTING -> Res.string.plus_format_body
        PlusBenefit.MOOD_THEME -> Res.string.plus_mood_theme_body
    }
