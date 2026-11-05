package org.sjhstudio.diary.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.sjhstudio.diary.core.designsystem.component.DiaryBottomSheet
import org.sjhstudio.diary.core.designsystem.component.DiaryIcon
import org.sjhstudio.diary.core.designsystem.component.DiarySheetAction
import org.sjhstudio.diary.core.designsystem.component.DiaryStatusBox
import org.sjhstudio.diary.core.designsystem.component.diaryPress
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryIconSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.model.EntryDetail
import org.sjhstudio.diary.core.model.EntryPhoto
import org.sjhstudio.diary.core.ui.DiaryRichText
import org.sjhstudio.diary.core.ui.MoodFace
import org.sjhstudio.diary.core.ui.WeatherIcon
import org.sjhstudio.diary.core.ui.diaryBodyTextStyle
import org.sjhstudio.diary.core.ui.moodName
import org.sjhstudio.diary.core.ui.rememberDiaryDateFormat
import org.sjhstudio.diary.core.ui.rememberFileFontFamily
import org.sjhstudio.diary.core.ui.rememberShareText
import org.sjhstudio.diary.core.ui.weatherName
import org.sjhstudio.diary.feature.home.resources.Res
import org.sjhstudio.diary.feature.home.resources.detail_back
import org.sjhstudio.diary.feature.home.resources.detail_edit
import org.sjhstudio.diary.feature.home.resources.detail_favorite
import org.sjhstudio.diary.feature.home.resources.detail_kept_both_body
import org.sjhstudio.diary.feature.home.resources.detail_kept_both_close
import org.sjhstudio.diary.feature.home.resources.detail_kept_both_other
import org.sjhstudio.diary.feature.home.resources.detail_kept_both_title
import org.sjhstudio.diary.feature.home.resources.detail_more
import org.sjhstudio.diary.feature.home.resources.detail_share
import org.sjhstudio.diary.feature.home.resources.detail_trash

/**
 * 일기 상세(Figma "일기 상세 (S3)"). 날짜, 기분·시각·날씨·장소 한 줄, 사진, 본문 순서다.
 * 공유는 서식 없는 글이다(FR-019). 넓은 화면의 오른쪽 칸에서는 [onBack]이 null이다.
 */
@Composable
fun EntryDetailRoute(
    entryId: String,
    onBack: (() -> Unit)?,
    onEdit: () -> Unit,
    onPhoto: (Int) -> Unit,
    onTrashed: () -> Unit,
    onOtherVersion: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel = koinViewModel<EntryDetailViewModel>(key = entryId) { parametersOf(entryId) }
    val detail by viewModel.detail.collectAsStateWithLifecycle()
    val look by viewModel.look.collectAsStateWithLifecycle()
    val trashed by rememberUpdatedState(onTrashed)
    LaunchedEffect(viewModel) { viewModel.trashed.collect { trashed() } }
    val share = rememberShareText()
    var more by remember { mutableStateOf(false) }
    val colors = DiaryTheme.colors
    val family = rememberFileFontFamily(look.fontPath)
    val current = detail
    Column(
        modifier.fillMaxSize().background(colors.canvas).statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = DiarySpacing.x1), verticalAlignment = Alignment.CenterVertically) {
            onBack?.let { ActionButton(DiaryIcons.ChevronLeft, stringResource(Res.string.detail_back), colors.textPrimary, it) }
            Box(Modifier.weight(1f))
            if (current != null) {
                val favorite = current.entry.isFavorite
                ActionButton(
                    if (favorite) DiaryIcons.StarFilled else DiaryIcons.StarOutline,
                    stringResource(Res.string.detail_favorite),
                    if (favorite) colors.accent else colors.textPrimary,
                    viewModel::toggleFavorite
                )
                ActionButton(DiaryIcons.Pen, stringResource(Res.string.detail_edit), colors.textPrimary, onEdit)
                ActionButton(DiaryIcons.More, stringResource(Res.string.detail_more), colors.textPrimary) { more = true }
            }
        }
        if (current != null) {
            Body(current, diaryBodyTextStyle(look.font, look.size) { family }, onPhoto, onOtherVersion, viewModel::dismissKeptBoth)
        }
    }
    if (more && current != null) {
        DiaryBottomSheet(onDismiss = { more = false }) {
            DiarySheetAction(DiaryIcons.Share, stringResource(Res.string.detail_share), colors.textPrimary) {
                more = false
                share(current.entry.plainText)
            }
            DiarySheetAction(DiaryIcons.Trash, stringResource(Res.string.detail_trash), colors.danger) {
                more = false
                viewModel.moveToTrash()
            }
        }
    }
}

@Composable
internal fun Body(
    detail: EntryDetail,
    bodyStyle: androidx.compose.ui.text.TextStyle,
    onPhoto: (Int) -> Unit,
    onOtherVersion: (String) -> Unit = {},
    onDismissKeptBoth: () -> Unit = {}
) {
    val colors = DiaryTheme.colors
    val dates = rememberDiaryDateFormat()
    val entry = detail.entry
    Column(
        Modifier
            .widthIn(max = DiaryWriteSize.bodyMax)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = DiarySpacing.screenCompact, vertical = DiarySpacing.x2),
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x4)
    ) {
        BasicText(dates.dayWithWeekday(entry.date), style = DiaryTheme.typography.title1.copy(color = colors.textPrimary))
        // 아이콘과 글자는 한 묶음으로 붙이고 묶음 사이를 띄운다. 긴 장소 이름은 다음 줄로 내려간다.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x3),
            verticalArrangement = Arrangement.spacedBy(DiarySpacing.x1),
            itemVerticalAlignment = Alignment.CenterVertically
        ) {
            val caption = DiaryTheme.typography.caption.copy(color = colors.textSecondary)
            val strong = DiaryTheme.typography.captionStrong.copy(color = colors.textSecondary)
            entry.mood?.let {
                MetaItem {
                    MoodFace(it, DiaryIconSize.i20)
                    BasicText(moodName(it), style = strong, maxLines = 1)
                }
            }
            BasicText(dates.time(entry.time), style = strong, maxLines = 1)
            entry.weather?.let {
                MetaItem {
                    WeatherIcon(it, DiaryIconSize.i16, colors.textSecondary)
                    BasicText(weatherName(it), style = caption, maxLines = 1)
                }
            }
            entry.placeName?.let {
                MetaItem {
                    DiaryIcon(DiaryIcons.Pin, contentDescription = null, tint = colors.textSecondary, size = DiaryIconSize.i16)
                    BasicText(it, style = caption, maxLines = 1)
                }
            }
        }
        detail.keptBothWith?.let { other ->
            DiaryStatusBox(
                DiaryIcons.Copy,
                stringResource(Res.string.detail_kept_both_title),
                stringResource(Res.string.detail_kept_both_body),
                actionLabel = stringResource(Res.string.detail_kept_both_other),
                onAction = { onOtherVersion(other) },
                secondaryLabel = stringResource(Res.string.detail_kept_both_close),
                onSecondary = onDismissKeptBoth
            )
        }
        if (detail.photos.isNotEmpty()) Gallery(detail.photos, onPhoto)
        DiaryRichText(entry.bodyHtml, bodyStyle, Modifier.padding(bottom = DiarySpacing.x10))
    }
}

/** 사진 격자: 1장 꽉, 2장 나란히, 3장 큰 칸 + 둘, 4장 이상 셋째 칸에 +N. 칸 사이 2. */
@Composable
private fun Gallery(photos: List<EntryPhoto>, onPhoto: (Int) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(GALLERY_RATIO).clip(DiaryShapes.r16)) {
        val gap = DiarySpacing.x0_5
        when (photos.size) {
            1 -> Cell(photos[0], Modifier.fillMaxSize()) { onPhoto(0) }

            2 ->
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    Cell(photos[0], Modifier.weight(1f)) { onPhoto(0) }
                    Cell(photos[1], Modifier.weight(1f)) { onPhoto(1) }
                }

            else ->
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    Cell(photos[0], Modifier.weight(2f)) { onPhoto(0) }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(gap)) {
                        Cell(photos[1], Modifier.weight(1f)) { onPhoto(1) }
                        Box(Modifier.weight(1f).fillMaxWidth()) {
                            Cell(photos[2], Modifier.fillMaxSize()) { onPhoto(2) }
                            if (photos.size > 3) {
                                BasicText(
                                    "+${photos.size - 3}",
                                    style = DiaryTheme.typography.label.copy(color = DiaryTheme.colors.onAccent),
                                    modifier =
                                        Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(DiarySpacing.x1)
                                            .clip(DiaryShapes.r8)
                                            .background(DiaryTheme.colors.overlay)
                                            .padding(horizontal = DiarySpacing.x1_5, vertical = DiarySpacing.x0_5)
                                )
                            }
                        }
                    }
                }
        }
    }
}

@Composable
private fun Cell(photo: EntryPhoto, modifier: Modifier, onClick: () -> Unit) {
    val colors = DiaryTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .fillMaxSize()
            .background(colors.sunken)
            .clickable(interactionSource = interaction, indication = null, role = Role.Image, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (photo.path == null) {
            DiaryIcon(if (photo.pending) DiaryIcons.Image else DiaryIcons.ImageOff, contentDescription = null, tint = colors.textTertiary)
        } else {
            AsyncImage(model = photo.path, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
    }
}

@Composable
private fun ActionButton(icon: ImageVector, description: String, tint: Color, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier
            .size(DiarySpacing.minTouchTarget)
            .diaryPress(interaction, DiaryTheme.colors.textPrimary, DiaryShapes.full)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        DiaryIcon(icon, contentDescription = description, tint = tint)
    }
}

/** 폰 350×230, 태블릿 600×396과 같은 비율. */
private const val GALLERY_RATIO = 1.52f

@Composable
private fun MetaItem(content: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x1), verticalAlignment = Alignment.CenterVertically) { content() }
}
