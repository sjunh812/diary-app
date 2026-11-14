package org.sjhstudio.diary.feature.home

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.sjhstudio.diary.core.designsystem.component.DiaryBottomSheet
import org.sjhstudio.diary.core.designsystem.component.DiaryEmptyArea
import org.sjhstudio.diary.core.designsystem.component.DiaryEmptyState
import org.sjhstudio.diary.core.designsystem.component.DiaryIcon
import org.sjhstudio.diary.core.designsystem.component.DiaryIconButton
import org.sjhstudio.diary.core.designsystem.component.DiarySheetAction
import org.sjhstudio.diary.core.designsystem.component.DiaryStatusBox
import org.sjhstudio.diary.core.designsystem.component.FirstLineIcon
import org.sjhstudio.diary.core.designsystem.component.diaryPress
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryIconSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.designsystem.theme.LocalScreenGutter
import org.sjhstudio.diary.core.model.EntryDetail
import org.sjhstudio.diary.core.model.EntryPhoto
import org.sjhstudio.diary.core.ui.DiaryRichText
import org.sjhstudio.diary.core.ui.LoadFailed
import org.sjhstudio.diary.core.ui.MoodFace
import org.sjhstudio.diary.core.ui.WeatherIcon
import org.sjhstudio.diary.core.ui.diaryBodyTextStyle
import org.sjhstudio.diary.core.ui.moodName
import org.sjhstudio.diary.core.ui.rememberDiaryDateFormat
import org.sjhstudio.diary.core.ui.rememberEntryShareText
import org.sjhstudio.diary.core.ui.rememberFileFontFamily
import org.sjhstudio.diary.core.ui.rememberShareText
import org.sjhstudio.diary.core.ui.weatherName
import org.sjhstudio.diary.feature.home.resources.Res
import org.sjhstudio.diary.feature.home.resources.detail_back
import org.sjhstudio.diary.feature.home.resources.detail_edit
import org.sjhstudio.diary.feature.home.resources.detail_favorite
import org.sjhstudio.diary.feature.home.resources.detail_kept_both_body
import org.sjhstudio.diary.feature.home.resources.detail_kept_both_close
import org.sjhstudio.diary.feature.home.resources.detail_kept_both_keep
import org.sjhstudio.diary.feature.home.resources.detail_kept_both_other
import org.sjhstudio.diary.feature.home.resources.detail_kept_both_title
import org.sjhstudio.diary.feature.home.resources.detail_missing_body
import org.sjhstudio.diary.feature.home.resources.detail_missing_title
import org.sjhstudio.diary.feature.home.resources.detail_more
import org.sjhstudio.diary.feature.home.resources.detail_share
import org.sjhstudio.diary.feature.home.resources.detail_trash
import org.sjhstudio.diary.feature.home.resources.detail_unfavorite

/**
 * 일기 상세(Figma "일기 상세 (S3)")
 * 날짜, 기분·시각·날씨·장소 한 줄, 사진, 본문 순. 공유는 서식 없는 글(FR-019)
 * 넓은 화면 오른쪽 칸에서는 [onBack]이 null
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
    val state by viewModel.state.collectAsStateWithLifecycle()
    val look by viewModel.look.collectAsStateWithLifecycle()
    val trashed by rememberUpdatedState(onTrashed)
    LaunchedEffect(viewModel) { viewModel.trashed.collect { trashed() } }
    val share = rememberShareText()
    val shareText = rememberEntryShareText()
    var more by remember { mutableStateOf(false) }
    val colors = DiaryTheme.colors
    val family = rememberFileFontFamily(look.fontPath)
    val current = (state as? EntryDetailState.Ready)?.detail
    val scroll = rememberScrollState()
    val lines = remember { DetailScrollLines() }
    Column(
        modifier.fillMaxSize().background(colors.canvas).statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 두 칸 오른쪽(뒤로 단추 없음)은 본문이 칸 여백에서 시작해 작은 날짜도 그 선에 맞춤
        val dateStart = if (onBack == null) LocalScreenGutter.current - DiarySpacing.x1 else DiarySpacing.x1
        Row(
            Modifier.fillMaxWidth().heightIn(min = DiarySpacing.x14).padding(start = DiarySpacing.x1, end = DiarySpacing.x2),
            verticalAlignment = Alignment.CenterVertically
        ) {
            onBack?.let { DiaryIconButton(DiaryIcons.ChevronLeft, stringResource(Res.string.detail_back), it) }
            Box(Modifier.weight(1f).padding(start = dateStart)) {
                if (current != null) SmallDate(current.entry.date, { lines.collapsed(scroll.value) })
            }
            if (current != null) {
                val favorite = current.entry.isFavorite
                DiaryIconButton(
                    if (favorite) DiaryIcons.StarFilled else DiaryIcons.StarOutline,
                    stringResource(if (favorite) Res.string.detail_unfavorite else Res.string.detail_favorite),
                    viewModel::toggleFavorite,
                    tint = if (favorite) colors.accent else colors.textPrimary
                )
                DiaryIconButton(DiaryIcons.EditText, stringResource(Res.string.detail_edit), onEdit)
                DiaryIconButton(DiaryIcons.More, stringResource(Res.string.detail_more), { more = true })
            }
        }
        when {
            current != null ->
                Body(
                    current,
                    diaryBodyTextStyle(look.font, look.size) { family },
                    onPhoto,
                    scroll,
                    lines,
                    // 두 칸 오른쪽은 Figma "S3 · 태블릿"처럼 칸을 채우고 좌우 여백만, 한 화면은 가운데 읽기 칸
                    if (onBack == null) Dp.Unspecified else DiaryWriteSize.bodyMax,
                    onOtherVersion,
                    viewModel::keepThisVersion,
                    viewModel::dismissKeptBoth,
                    viewModel::toggleCheck
                )

            state == EntryDetailState.Missing -> EntryMissing()

            state == EntryDetailState.Failed -> LoadFailed(viewModel::retry)
        }
    }
    if (more && current != null) {
        DiaryBottomSheet(onDismiss = { more = false }) {
            DiarySheetAction(DiaryIcons.Share, stringResource(Res.string.detail_share), colors.textPrimary) {
                more = false
                share(shareText(current.entry))
            }
            DiarySheetAction(DiaryIcons.Trash, stringResource(Res.string.detail_trash), colors.danger) {
                more = false
                viewModel.moveToTrash()
            }
        }
    }
}

/** 삭제했거나 휴지통으로 옮겨 없는 일기. 알림이나 위젯으로 들어오면 만날 수 있음 */
@Composable
internal fun EntryMissing() {
    DiaryEmptyArea {
        DiaryEmptyState(
            DiaryIcons.BookOpen,
            stringResource(Res.string.detail_missing_title),
            description = stringResource(Res.string.detail_missing_body)
        )
    }
}

@Composable
internal fun Body(
    detail: EntryDetail,
    bodyStyle: TextStyle,
    onPhoto: (Int) -> Unit,
    scroll: ScrollState = rememberScrollState(),
    lines: DetailScrollLines = remember { DetailScrollLines() },
    contentMax: Dp = DiaryWriteSize.bodyMax,
    onOtherVersion: (String) -> Unit = {},
    onKeepThis: () -> Unit = {},
    onDismissKeptBoth: () -> Unit = {},
    onCheck: (Int) -> Unit = {}
) {
    val colors = DiaryTheme.colors
    val dates = rememberDiaryDateFormat()
    val entry = detail.entry
    // 스크롤은 칸 전체 폭이라 넓은 화면 양옆 여백에서도 스크롤되고, 붙는 줄 바탕과 선이 위 줄과 같은 폭
    val content = Modifier.widthIn(max = contentMax).fillMaxWidth().padding(horizontal = LocalScreenGutter.current)
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(scroll)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 위 12를 안쪽 여백이 아니라 칸으로 둬, 붙는 자리 계산(positionInParent)이 스크롤 내용 좌표와 같음
        Spacer(Modifier.height(DiarySpacing.x3))
        // 큰 날짜에는 연도가 없어 몇 해 전 일기인지 알 수 없음. 날짜 위에 작게 둠
        BasicText(
            dates.year(entry.date),
            modifier = content,
            style = DiaryTheme.typography.caption.copy(color = colors.textSecondary)
        )
        BasicText(
            dates.dayWithWeekday(entry.date),
            modifier = content.onPlaced { lines.titleBottom = it.positionInParent().y + it.size.height },
            style = DiaryTheme.typography.title1.copy(color = colors.textPrimary)
        )
        // 줄 안쪽(위 4, 아래 8)은 붙었을 때 위 줄·본문과 띄우는 자리. 위 줄 아래에 이미 빈 곳이 있어 위는 좁게
        // 쉴 때 날짜·줄·사진 사이는 전과 같은 16
        Spacer(Modifier.height(DiarySpacing.x3))
        Box(
            Modifier
                .fillMaxWidth()
                .onPlaced { lines.metaTop = it.positionInParent().y }
                .stickToTop(scroll, lines, colors.canvas, colors.line),
            contentAlignment = Alignment.TopCenter
        ) {
            MetaRow(detail, content)
        }
        Spacer(Modifier.height(DiarySpacing.x2))
        Column(content, verticalArrangement = Arrangement.spacedBy(DiarySpacing.x4)) {
            detail.keptBothWith?.let { other ->
                DiaryStatusBox(
                    DiaryIcons.Copy,
                    stringResource(Res.string.detail_kept_both_title),
                    stringResource(Res.string.detail_kept_both_body),
                    actionLabel = stringResource(Res.string.detail_kept_both_other),
                    onAction = { onOtherVersion(other) },
                    secondaryLabel = stringResource(Res.string.detail_kept_both_keep),
                    onSecondary = onKeepThis,
                    quietLabel = stringResource(Res.string.detail_kept_both_close),
                    onQuiet = onDismissKeptBoth
                )
            }
            if (detail.photos.isNotEmpty()) Gallery(detail.photos, onPhoto)
            DiaryRichText(entry.bodyHtml, bodyStyle, Modifier.padding(bottom = DiarySpacing.x10 + DiarySpacing.x2), onCheck)
        }
    }
}

/**
 * 기분·시각·날씨·장소 한 줄. 값이 있는 것만
 * 아이콘과 글자는 한 묶음으로 붙이고 묶음 사이를 띄움. 긴 장소 이름은 다음 줄로 내려가 두 줄에서 말줄임
 */
@Composable
private fun MetaRow(detail: EntryDetail, modifier: Modifier = Modifier) {
    val colors = DiaryTheme.colors
    val dates = rememberDiaryDateFormat()
    val entry = detail.entry
    FlowRow(
        modifier.fillMaxWidth().padding(top = DiarySpacing.x1, bottom = DiarySpacing.x2),
        horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x3),
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x1),
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        val caption = DiaryTheme.typography.caption.copy(color = colors.textSecondary)
        val strong = DiaryTheme.typography.captionStrong.copy(color = colors.textSecondary)
        entry.mood?.let {
            MetaItem {
                MoodFace(it, DiaryIconSize.i20)
                BasicText(moodName(it), style = strong, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        BasicText(dates.time(entry.time), style = strong, maxLines = 1, overflow = TextOverflow.Ellipsis)
        entry.weather?.let {
            MetaItem {
                WeatherIcon(it, DiaryIconSize.i16, colors.textSecondary)
                BasicText(weatherName(it), style = caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        entry.placeName?.let {
            Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x1)) {
                FirstLineIcon(DiaryIcons.Pin, caption, colors.textSecondary)
                BasicText(
                    it,
                    modifier = Modifier.weight(1f, fill = false),
                    style = caption,
                    maxLines = PLACE_LINES,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 스크롤 안 제목과 기분 줄의 자리(px). 그려질 때 정해짐
 * 위 줄의 작은 날짜와 붙는 기분 줄이 이 값으로 스크롤한 만큼 바뀜
 */
@Stable
internal class DetailScrollLines {
    var titleBottom by mutableFloatStateOf(0f)
    var metaTop by mutableFloatStateOf(0f)

    /** 0(큰 날짜가 다 보임)에서 1(위 줄 밑으로 다 들어감)까지 */
    fun collapsed(scrolled: Int): Float = if (titleBottom <= 0f) 0f else (scrolled / titleBottom).coerceIn(0f, 1f)
}

/**
 * 스크롤해 위 줄에 닿으면 그 자리에 머무름
 * 바탕은 칸 전체 폭으로 칠해 지나가는 본문을 가리고, 붙은 동안만 아래 가는 선
 * 스크롤 값은 그리기 단계에서만 읽어 스크롤마다 다시 구성하지 않음
 */
private fun Modifier.stickToTop(scroll: ScrollState, lines: DetailScrollLines, background: Color, line: Color): Modifier = zIndex(1f)
    .graphicsLayer { translationY = (scroll.value - lines.metaTop).coerceAtLeast(0f) }
    .drawBehind {
        drawRect(background)
        if (scroll.value > lines.metaTop) {
            val thin = DiarySpacing.hairline.toPx()
            drawRect(line, topLeft = Offset(0f, size.height - thin), size = Size(size.width, thin))
        }
    }

/**
 * 큰 날짜가 반 넘게 위 줄 밑으로 들어가면 위 줄에 나타나는 작은 날짜
 * 탭 상단 바와 같은 규칙(`docs/design.md` 탭 상단 바). 반 전에는 화면 읽기에서 뺌
 */
@Composable
private fun SmallDate(date: LocalDate, collapsed: () -> Float) {
    val dates = rememberDiaryDateFormat()
    val progress by rememberUpdatedState(collapsed)
    val half by remember { derivedStateOf { progress() >= HALF } }
    // 접히면 큰 날짜 위의 연도가 가려짐. 좁은 자리라 요일을 빼고 연도를 넣음
    BasicText(
        dates.fullDate(date),
        modifier = Modifier
            .graphicsLayer { alpha = ((progress() - HALF) / HALF).coerceIn(0f, 1f) }
            .then(if (half) Modifier else Modifier.clearAndSetSemantics {}),
        style = DiaryTheme.typography.headline.copy(color = DiaryTheme.colors.textPrimary),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

private const val HALF = 0.5f

/** 사진 격자: 1장 꽉, 2장 나란히, 3장 큰 칸 + 둘, 4장 이상 셋째 칸에 +N. 칸 사이 2 */
@Composable
private fun Gallery(photos: List<EntryPhoto>, onPhoto: (Int) -> Unit) {
    val window = LocalWindowInfo.current.containerSize
    val windowHeight = with(LocalDensity.current) { window.height.toDp() }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val height = minOf(maxWidth / galleryRatio(photos), windowHeight * GALLERY_MAX_HEIGHT)
        GalleryCells(photos, onPhoto, Modifier.fillMaxWidth().height(height).clip(DiaryShapes.r12))
    }
}

@Composable
private fun GalleryCells(photos: List<EntryPhoto>, onPhoto: (Int) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier) {
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
                                    style = DiaryTheme.typography.micro.copy(color = DiaryTheme.colors.onAccent),
                                    modifier =
                                        Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(DiarySpacing.x1)
                                            .clip(DiaryShapes.r8)
                                            .background(DiaryTheme.colors.onPhoto)
                                            .padding(horizontal = DiarySpacing.x1, vertical = DiarySpacing.x0_5)
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
            .diaryPress(interaction, colors.textPrimary)
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

/**
 * 사진 묶음의 가로÷세로
 * 한 장은 사진 비율 그대로라 전체 보기와 같은 장면. 세로는 정사각형까지만 키워 첫 화면에 본문이 남게 함
 * 여러 장이나 크기를 모르는 사진은 격자 비율
 */
internal fun galleryRatio(photos: List<EntryPhoto>): Float {
    val only = photos.singleOrNull()
    val width = only?.width ?: 0
    val height = only?.height ?: 0
    return if (width > 0 && height > 0) (width.toFloat() / height).coerceIn(TALLEST_RATIO, WIDEST_RATIO) else GALLERY_RATIO
}

/** 폰 350×230, 태블릿 600×396과 같은 비율 */
internal const val GALLERY_RATIO = 1.52f

/**
 * 사진 높이는 창 높이의 45%까지. 낮은 창(태블릿·폴드 가로)에서 첫 화면에 본문이 여러 줄 남게 넘는 만큼만 자름
 * 폰 세로·태블릿 세로는 폭으로 정한 높이가 더 낮아 그대로
 */
private const val GALLERY_MAX_HEIGHT = 0.45f
private const val TALLEST_RATIO = 1f
private const val WIDEST_RATIO = 1.91f

/** 장소 이름 최대 줄 수 */
private const val PLACE_LINES = 2

@Composable
private fun MetaItem(content: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x1), verticalAlignment = Alignment.CenterVertically) { content() }
}
