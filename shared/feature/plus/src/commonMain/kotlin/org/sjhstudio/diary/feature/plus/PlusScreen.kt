package org.sjhstudio.diary.feature.plus

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.sjhstudio.diary.core.designsystem.component.DiaryButton
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonKind
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonSize
import org.sjhstudio.diary.core.designsystem.component.DiaryIcon
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsRow
import org.sjhstudio.diary.core.designsystem.component.DiaryToastHost
import org.sjhstudio.diary.core.designsystem.component.DiaryToastState
import org.sjhstudio.diary.core.designsystem.component.DiaryTopBar
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryArtSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.model.Plan
import org.sjhstudio.diary.core.model.ProductId
import org.sjhstudio.diary.core.ui.DiaryLottie
import org.sjhstudio.diary.feature.plus.resources.Res
import org.sjhstudio.diary.feature.plus.resources.plus_ad_free_owned
import org.sjhstudio.diary.feature.plus.resources.plus_ads_title
import org.sjhstudio.diary.feature.plus.resources.plus_close
import org.sjhstudio.diary.feature.plus.resources.plus_coming
import org.sjhstudio.diary.feature.plus.resources.plus_failed
import org.sjhstudio.diary.feature.plus.resources.plus_manage_note
import org.sjhstudio.diary.feature.plus.resources.plus_manage_store
import org.sjhstudio.diary.feature.plus.resources.plus_manage_title
import org.sjhstudio.diary.feature.plus.resources.plus_manage_top
import org.sjhstudio.diary.feature.plus.resources.plus_no_store
import org.sjhstudio.diary.feature.plus.resources.plus_notice
import org.sjhstudio.diary.feature.plus.resources.plus_photos_title
import org.sjhstudio.diary.feature.plus.resources.plus_price
import org.sjhstudio.diary.feature.plus.resources.plus_restore
import org.sjhstudio.diary.feature.plus.resources.plus_restore_none
import org.sjhstudio.diary.feature.plus.resources.plus_restored
import org.sjhstudio.diary.feature.plus.resources.plus_retry
import org.sjhstudio.diary.feature.plus.resources.plus_start
import org.sjhstudio.diary.feature.plus.resources.plus_stats_title
import org.sjhstudio.diary.feature.plus.resources.plus_title

private const val ART_MILLIS = 2_800

/** 설정 Plus 줄과 사진 한도, 고급 통계 미리보기가 여는 화면. 이미 Plus면 관리 화면이다(US3). */
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
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            scope.launch {
                toast.show(
                    when (message) {
                        PlusMessage.Failed -> failed
                        PlusMessage.Restored -> restored
                        PlusMessage.RestoreNone -> none
                    }
                )
            }
        }
    }
    val uri = LocalUriHandler.current
    Box(modifier.fillMaxSize()) {
        if (plan == Plan.PLUS) {
            PlusManageScreen(onClose, onStore = { uri.openUri(viewModel.manageUrl) }, onRestore = viewModel::restore)
        } else {
            PaywallScreen(
                price = state.price(ProductId.PLUS_YEARLY),
                loading = state.loading,
                busy = state.busy,
                adFreeOwned = plan == Plan.AD_FREE,
                onClose = onClose,
                onRestore = viewModel::restore,
                onRetry = viewModel::load,
                onBuy = { viewModel.buy(ProductId.PLUS_YEARLY) }
            )
        }
        DiaryToastHost(toast, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = DiarySpacing.x14 * 3))
    }
}

/**
 * 결제 화면(Figma P1). 한 화면에서 스크롤 없이 끝나게 그림, 제목, 혜택 세 줄, 결제액, 버튼만 둔다.
 * 닫기는 처음부터 보이고 실제 결제액이 가장 크다(FR-005). 작은 화면이나 큰 글자에서만 위쪽이 스크롤된다.
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
    modifier: Modifier = Modifier
) {
    val colors = DiaryTheme.colors
    val typography = DiaryTheme.typography
    Column(
        modifier.fillMaxSize().background(colors.canvas).statusBarsPadding().navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(Modifier.widthIn(max = DiaryWriteSize.bodyMax).fillMaxHeight()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = DiarySpacing.x1), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(DiarySpacing.minTouchTarget).clip(DiaryShapes.full).clickable(role = Role.Button, onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    DiaryIcon(DiaryIcons.Close, stringResource(Res.string.plus_close), colors.textPrimary)
                }
                Spacer(Modifier.weight(1f))
                DiaryButton(stringResource(Res.string.plus_restore), onRestore, kind = DiaryButtonKind.Text)
            }
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = DiarySpacing.screenCompact),
                verticalArrangement = Arrangement.spacedBy(DiarySpacing.x6, Alignment.CenterVertically)
            ) {
                DiaryLottie(
                    { Res.readBytes("files/plus_benefits.json") },
                    ART_MILLIS,
                    active = true,
                    Modifier.fillMaxWidth().height(DiaryArtSize.banner)
                )
                BasicText(stringResource(Res.string.plus_title), style = typography.title1.copy(color = colors.textPrimary))
                Column(verticalArrangement = Arrangement.spacedBy(DiarySpacing.x3)) {
                    Benefit(Res.string.plus_ads_title)
                    Benefit(Res.string.plus_photos_title)
                    Benefit(Res.string.plus_stats_title)
                }
                BasicText(stringResource(Res.string.plus_coming), style = typography.caption.copy(color = colors.textSecondary))
                if (adFreeOwned) {
                    BasicText(stringResource(Res.string.plus_ad_free_owned), style = typography.caption.copy(color = colors.accentText))
                }
            }
            Column(
                Modifier.fillMaxWidth().padding(horizontal = DiarySpacing.screenCompact, vertical = DiarySpacing.x3),
                verticalArrangement = Arrangement.spacedBy(DiarySpacing.x3)
            ) {
                if (price != null) {
                    BasicText(stringResource(Res.string.plus_price, price), style = typography.title2.copy(color = colors.textPrimary))
                    DiaryButton(
                        stringResource(Res.string.plus_start),
                        onBuy,
                        Modifier.fillMaxWidth(),
                        size = DiaryButtonSize.XLarge,
                        enabled = !busy
                    )
                    BasicText(stringResource(Res.string.plus_notice), style = typography.caption.copy(color = colors.textSecondary))
                } else if (!loading) {
                    BasicText(stringResource(Res.string.plus_no_store), style = typography.body.copy(color = colors.textSecondary))
                    DiaryButton(stringResource(Res.string.plus_retry), onRetry, Modifier.fillMaxWidth(), size = DiaryButtonSize.XLarge)
                }
            }
        }
    }
}

@Composable
private fun Benefit(title: StringResource) {
    val colors = DiaryTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x3), verticalAlignment = Alignment.CenterVertically) {
        DiaryIcon(DiaryIcons.CircleCheck, contentDescription = null, tint = colors.accent)
        BasicText(stringResource(title), style = DiaryTheme.typography.reading.copy(color = colors.textPrimary))
    }
}

/** 이미 Plus인 사람의 화면(Figma P4). 권유하지 않는다(HIG 인앱 구매). */
@Composable
internal fun PlusManageScreen(onBack: () -> Unit, onStore: () -> Unit, onRestore: () -> Unit, modifier: Modifier = Modifier) {
    val colors = DiaryTheme.colors
    val typography = DiaryTheme.typography
    Column(modifier.fillMaxSize().background(colors.canvas).statusBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        DiaryTopBar(stringResource(Res.string.plus_manage_top), onBack = onBack)
        Column(Modifier.widthIn(max = DiaryWriteSize.bodyMax).fillMaxWidth().verticalScroll(rememberScrollState())) {
            Column(
                Modifier.padding(horizontal = DiarySpacing.screenCompact, vertical = DiarySpacing.x4),
                verticalArrangement = Arrangement.spacedBy(DiarySpacing.x3)
            ) {
                BasicText(stringResource(Res.string.plus_manage_title), style = typography.title1.copy(color = colors.textPrimary))
                listOf(Res.string.plus_ads_title, Res.string.plus_photos_title, Res.string.plus_stats_title).forEach { item ->
                    Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x3), verticalAlignment = Alignment.CenterVertically) {
                        DiaryIcon(DiaryIcons.Check, contentDescription = null, tint = colors.accent)
                        BasicText(stringResource(item), style = typography.reading.copy(color = colors.textPrimary))
                    }
                }
            }
            DiarySettingsRow(stringResource(Res.string.plus_manage_store), onClick = onStore)
            DiarySettingsRow(stringResource(Res.string.plus_restore), onClick = onRestore)
            BasicText(
                stringResource(Res.string.plus_manage_note),
                Modifier.padding(horizontal = DiarySpacing.screenCompact, vertical = DiarySpacing.x4),
                style = typography.caption.copy(color = colors.textSecondary)
            )
        }
    }
}
