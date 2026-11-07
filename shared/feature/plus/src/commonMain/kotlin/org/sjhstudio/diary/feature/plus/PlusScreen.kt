package org.sjhstudio.diary.feature.plus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.text.BasicText
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.sjhstudio.diary.core.designsystem.component.DiaryButton
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonKind
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonSize
import org.sjhstudio.diary.core.designsystem.component.DiaryIcon
import org.sjhstudio.diary.core.designsystem.component.DiaryIconButton
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsRow
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsValueRow
import org.sjhstudio.diary.core.designsystem.component.DiaryToastHost
import org.sjhstudio.diary.core.designsystem.component.DiaryToastState
import org.sjhstudio.diary.core.designsystem.component.DiaryTopBar
import org.sjhstudio.diary.core.designsystem.component.FirstLineIcon
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryArtSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryIconSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.designsystem.theme.LocalScreenGutter
import org.sjhstudio.diary.core.model.Plan
import org.sjhstudio.diary.core.model.ProductId
import org.sjhstudio.diary.core.ui.DiaryLottie
import org.sjhstudio.diary.feature.plus.resources.Res
import org.sjhstudio.diary.feature.plus.resources.plus_ad_free_owned
import org.sjhstudio.diary.feature.plus.resources.plus_ads_body
import org.sjhstudio.diary.feature.plus.resources.plus_ads_only
import org.sjhstudio.diary.feature.plus.resources.plus_ads_title
import org.sjhstudio.diary.feature.plus.resources.plus_checklist_body
import org.sjhstudio.diary.feature.plus.resources.plus_checklist_title
import org.sjhstudio.diary.feature.plus.resources.plus_close
import org.sjhstudio.diary.feature.plus.resources.plus_coming
import org.sjhstudio.diary.feature.plus.resources.plus_failed
import org.sjhstudio.diary.feature.plus.resources.plus_manage_benefits
import org.sjhstudio.diary.feature.plus.resources.plus_manage_note
import org.sjhstudio.diary.feature.plus.resources.plus_manage_price
import org.sjhstudio.diary.feature.plus.resources.plus_manage_store
import org.sjhstudio.diary.feature.plus.resources.plus_manage_store_body
import org.sjhstudio.diary.feature.plus.resources.plus_manage_thanks
import org.sjhstudio.diary.feature.plus.resources.plus_manage_title
import org.sjhstudio.diary.feature.plus.resources.plus_manage_top
import org.sjhstudio.diary.feature.plus.resources.plus_no_store
import org.sjhstudio.diary.feature.plus.resources.plus_notice
import org.sjhstudio.diary.feature.plus.resources.plus_photos_body
import org.sjhstudio.diary.feature.plus.resources.plus_photos_title
import org.sjhstudio.diary.feature.plus.resources.plus_price
import org.sjhstudio.diary.feature.plus.resources.plus_restore
import org.sjhstudio.diary.feature.plus.resources.plus_restore_failed
import org.sjhstudio.diary.feature.plus.resources.plus_restore_none
import org.sjhstudio.diary.feature.plus.resources.plus_restored
import org.sjhstudio.diary.feature.plus.resources.plus_retry
import org.sjhstudio.diary.feature.plus.resources.plus_start
import org.sjhstudio.diary.feature.plus.resources.plus_start_price
import org.sjhstudio.diary.feature.plus.resources.plus_stats_body
import org.sjhstudio.diary.feature.plus.resources.plus_stats_title
import org.sjhstudio.diary.feature.plus.resources.plus_title

private const val ART_MILLIS = 2_800
private const val ART_RATIO = 350f / 220f
private const val ART_TINT = 0.4f

/**
 * 설정 Plus 줄과 사진 한도, 고급 통계 미리보기가 여는 화면.
 * 이미 Plus면 관리 화면이다(US3).
 */
@Composable
fun PlusRoute(onClose: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel = koinViewModel<PlusViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val plan by viewModel.plan.collectAsStateWithLifecycle()
    val toast = remember { DiaryToastState() }
    val scope = rememberCoroutineScope()
    val failed = stringResource(Res.string.plus_failed)
    val restored = stringResource(Res.string.plus_restored)
    val none = stringResource(Res.string.plus_restore_none)
    val restoreFailed = stringResource(Res.string.plus_restore_failed)
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            scope.launch {
                toast.show(
                    when (message) {
                        PlusMessage.Failed -> failed
                        PlusMessage.Restored -> restored
                        PlusMessage.RestoreNone -> none
                        PlusMessage.RestoreFailed -> restoreFailed
                    }
                )
            }
        }
    }
    val uri = LocalUriHandler.current
    // 결제 화면의 아래 결제 칸 높이.
    // 토스트가 버튼과 결제액을 가리지 않게 그 위에 띄운다.
    var buyHeight by remember { mutableIntStateOf(0) }
    var adFreeSheet by rememberSaveable { mutableStateOf(false) }
    Box(modifier.fillMaxSize()) {
        if (plan == Plan.PLUS) {
            PlusManageScreen(state.price(ProductId.PLUS_YEARLY), onClose, onStore = { uri.openUri(viewModel.manageUrl) })
        } else {
            PaywallScreen(
                price = state.price(ProductId.PLUS_YEARLY),
                loading = state.loading,
                busy = state.busy,
                adFreeOwned = plan == Plan.AD_FREE,
                onClose = onClose,
                onRestore = { viewModel.restore() },
                onRetry = viewModel::load,
                onBuy = { viewModel.buy(ProductId.PLUS_YEARLY) },
                onBuyHeight = { buyHeight = it },
                onAdFree = { adFreeSheet = true }
            )
        }
        val fixedBottom = if (plan == Plan.PLUS) Dp.Hairline else with(LocalDensity.current) { buyHeight.toDp() }
        DiaryToastHost(
            toast,
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = fixedBottom + DiarySpacing.x4)
        )
    }
    if (adFreeSheet) AdFreeSheet(onDismiss = { adFreeSheet = false })
}

/**
 * 결제 화면(Figma P1).
 * 제목, 혜택 네 줄(무엇을 얻는지 한 문장씩), 결제액을 적은 버튼을 둔다.
 * 그림은 남는 자리에만 그리고, 버튼은 아래에 고정된다.
 * 닫기는 처음부터 보이고 실제 결제액이 가장 크다(FR-005).
 * 작은 화면이나 큰 글자에서만 위쪽이 스크롤된다.
 * 넓은 화면도 폰과 같은 한 칸이다.
 * 혜택과 가격이 한눈에 이어지도록 가운데에 모은다.
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
    onAdFree: () -> Unit = {}
) {
    val colors = DiaryTheme.colors
    BoxWithConstraints(modifier.fillMaxSize().background(colors.canvas).statusBarsPadding().navigationBarsPadding()) {
        val wide = maxWidth >= DiaryWriteSize.twoPaneMin
        // 넓은 화면의 결제 칸은 가운데에 있어 화면 아래에 고정된 것이 없다.
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
                    size = DiaryButtonSize.Small
                )
            }
            val column = Modifier.widthIn(max = DiaryArtSize.paywallMax).fillMaxWidth().padding(horizontal = LocalScreenGutter.current)
            if (wide) {
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                    Column(
                        column.verticalScroll(rememberScrollState()).heightIn(min = maxHeight).padding(bottom = DiarySpacing.x4),
                        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x8, Alignment.CenterVertically)
                    ) {
                        PlusArt(DiaryArtSize.banner, Modifier.padding(top = DiarySpacing.x2))
                        PaywallBenefits(adFreeOwned)
                        PaywallBuy(price, loading, busy, onRetry, onBuy, adFreeOwned, onAdFree, Modifier.fillMaxWidth())
                    }
                }
            } else {
                PaywallTop(adFreeOwned, column.weight(1f))
                PaywallBuy(
                    price,
                    loading,
                    busy,
                    onRetry,
                    onBuy,
                    adFreeOwned,
                    onAdFree,
                    column.onSizeChanged { onBuyHeight(it.height) }.padding(vertical = DiarySpacing.x3)
                )
            }
        }
    }
}

/**
 * 그림 칸.
 * 폭을 채우고 그림은 가운데에 둔다.
 * 결제 화면과 구독 중 화면이 같이 쓴다.
 */
@Composable
private fun PlusArt(height: Dp, modifier: Modifier = Modifier) {
    val colors = DiaryTheme.colors
    Box(
        modifier.fillMaxWidth().clip(DiaryShapes.r20).background(colors.selected.copy(alpha = ART_TINT)),
        contentAlignment = Alignment.Center
    ) {
        DiaryLottie(
            { Res.readBytes("files/plus_benefits.json") },
            ART_MILLIS,
            active = true,
            Modifier.heightIn(max = height).aspectRatio(ART_RATIO)
        )
    }
}

/**
 * 혜택이 한눈에 보이는 것이 먼저다.
 * 글을 다 놓고 남는 높이만큼만 그림을 그린다.
 * 남는 자리가 작으면 그림을 빼고, 그래도 넘칠 때만 스크롤된다.
 */
@Composable
private fun PaywallTop(adFreeOwned: Boolean, modifier: Modifier) {
    BoxWithConstraints(modifier) {
        val room = maxHeight
        SubcomposeLayout(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) { constraints ->
            val width = constraints.copy(minHeight = 0)
            val text = subcompose("text") { PaywallBenefits(adFreeOwned) }.map { it.measure(width) }
            val textHeight = text.sumOf { it.height }
            val gap = DiarySpacing.x4.roundToPx()
            val left = room.roundToPx() - textHeight - gap - DiarySpacing.x2.roundToPx()
            val art =
                if (left < DiaryArtSize.paywallArtMin.roundToPx()) {
                    emptyList()
                } else {
                    val height = minOf(left, DiaryArtSize.banner.roundToPx()).toDp()
                    subcompose("art") { PlusArt(height, Modifier.padding(top = DiarySpacing.x2)) }.map { it.measure(width) }
                }
            val artHeight = art.sumOf { it.height } + if (art.isEmpty()) 0 else gap
            layout(constraints.maxWidth, artHeight + textHeight) {
                art.forEach { it.placeRelative(0, 0) }
                text.forEach { it.placeRelative(0, artHeight) }
            }
        }
    }
}

/** 제목과 혜택. */
@Composable
private fun PaywallBenefits(adFreeOwned: Boolean) {
    val colors = DiaryTheme.colors
    val typography = DiaryTheme.typography
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(DiarySpacing.x4)) {
        BasicText(stringResource(Res.string.plus_title), style = typography.title1.copy(color = colors.textPrimary))
        PlusBenefits()
        if (adFreeOwned) {
            BasicText(stringResource(Res.string.plus_ad_free_owned), style = typography.caption.copy(color = colors.accentText))
        }
    }
}

/**
 * 실제 결제액, 버튼, 자동 갱신 고지(FR-005).
 * 가격을 못 받으면 지어내지 않고 다시 시도를 보인다.
 * 읽는 동안에는 결제액 자리를 비워 두고 버튼을 비활성으로 두어 칸이 뛰지 않는다.
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
    modifier: Modifier
) {
    val colors = DiaryTheme.colors
    val typography = DiaryTheme.typography
    Column(modifier, verticalArrangement = Arrangement.spacedBy(DiarySpacing.x3)) {
        if (price != null || loading) {
            DiaryButton(
                // 요금은 하나뿐이라 고르는 칸을 두지 않고 버튼에 바로 적는다.
                price?.let { stringResource(Res.string.plus_start_price, it) } ?: stringResource(Res.string.plus_start),
                onBuy,
                Modifier.fillMaxWidth(),
                size = DiaryButtonSize.XLarge,
                enabled = price != null && !busy
            )
            BasicText(stringResource(Res.string.plus_notice), style = typography.caption.copy(color = colors.textSecondary))
            // 광고의 ×를 찾지 않아도 살 수 있게 둔다.
            // 이미 산 사람에게는 보이지 않는다.
            if (!adFreeOwned) {
                DiaryButton(
                    stringResource(Res.string.plus_ads_only),
                    onAdFree,
                    Modifier.align(Alignment.CenterHorizontally),
                    size = DiaryButtonSize.Medium,
                    kind = DiaryButtonKind.Text
                )
            }
        } else {
            BasicText(stringResource(Res.string.plus_no_store), style = typography.body.copy(color = colors.textSecondary))
            DiaryButton(stringResource(Res.string.plus_retry), onRetry, Modifier.fillMaxWidth(), size = DiaryButtonSize.XLarge)
        }
    }
}

/** 혜택 네 가지. 결제 화면과 구독 중 화면이 같은 말로 보인다. */
@Composable
private fun PlusBenefits() {
    Column(verticalArrangement = Arrangement.spacedBy(DiarySpacing.x4)) {
        OwnedBenefit(DiaryIcons.ShieldCheck, Res.string.plus_ads_title, Res.string.plus_ads_body)
        OwnedBenefit(DiaryIcons.Image, Res.string.plus_photos_title, Res.string.plus_photos_body)
        OwnedBenefit(DiaryIcons.Checklist, Res.string.plus_checklist_title, Res.string.plus_checklist_body)
        OwnedBenefit(DiaryIcons.TabInsight, Res.string.plus_stats_title, Res.string.plus_stats_body)
    }
}

/**
 * 이미 Plus인 사람의 화면(Figma P4).
 * 권유하지 않고, 구독해 준 사람에게 지금 받는 것과 요금, 관리할 곳을 한곳에 보인다.
 * 요금은 스토어에서 받은 값만 쓰고 받지 못하면 줄을 뺀다.
 * 다음 결제일과 해지는 스토어가 맡는다(HIG 인앱 구매).
 */
@Composable
internal fun PlusManageScreen(price: String?, onBack: () -> Unit, onStore: () -> Unit, modifier: Modifier = Modifier) {
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
                BasicText(stringResource(Res.string.plus_manage_title), style = typography.title1.copy(color = colors.textPrimary))
                Spacer(Modifier.height(DiarySpacing.x1))
                BasicText(stringResource(Res.string.plus_manage_thanks), style = typography.body.copy(color = colors.textSecondary))
                Spacer(Modifier.height(DiarySpacing.x6))
                BasicText(stringResource(Res.string.plus_manage_benefits), style = typography.label.copy(color = colors.textSecondary))
                Spacer(Modifier.height(DiarySpacing.x3))
                PlusBenefits()
                Spacer(Modifier.height(DiarySpacing.x4))
                BasicText(stringResource(Res.string.plus_coming), style = typography.caption.copy(color = colors.textSecondary))
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
            BasicText(
                stringResource(Res.string.plus_manage_note),
                gutter.padding(top = DiarySpacing.x2),
                style = typography.caption.copy(color = colors.textSecondary)
            )
        }
    }
}

/**
 * 혜택 한 줄.
 * 무엇을 받는지 한 문장으로 덧붙인다.
 */
@Composable
private fun OwnedBenefit(icon: ImageVector, title: StringResource, body: StringResource) {
    val colors = DiaryTheme.colors
    val typography = DiaryTheme.typography
    Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x3), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(DiarySpacing.x10).clip(DiaryShapes.full).background(colors.selected),
            contentAlignment = Alignment.Center
        ) {
            DiaryIcon(icon, contentDescription = null, tint = colors.accentText, size = DiaryIconSize.i20)
        }
        Column(verticalArrangement = Arrangement.spacedBy(DiarySpacing.x0_5)) {
            BasicText(stringResource(title), style = typography.bodyStrong.copy(color = colors.textPrimary))
            BasicText(stringResource(body), style = typography.caption.copy(color = colors.textSecondary))
        }
    }
}
