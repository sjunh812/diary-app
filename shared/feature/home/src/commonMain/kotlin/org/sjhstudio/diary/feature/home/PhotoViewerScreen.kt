package org.sjhstudio.diary.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.sjhstudio.diary.core.designsystem.component.DiaryIcon
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.model.EntryPhoto
import org.sjhstudio.diary.feature.home.resources.Res
import org.sjhstudio.diary.feature.home.resources.detail_elsewhere_photo
import org.sjhstudio.diary.feature.home.resources.detail_missing_photo
import org.sjhstudio.diary.feature.home.resources.detail_photo_close
import org.sjhstudio.diary.feature.home.resources.detail_photo_page

private const val MAX_ZOOM = 4f

/** 사진 크게 보기(Figma "S3-a · 사진 크게"). 바탕은 사진을 위해 테마와 상관없이 검다. 좌우로 넘기고 두 손가락으로 키운다(FR-020). */
@Composable
fun PhotoViewerRoute(entryId: String, index: Int, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel = koinViewModel<EntryDetailViewModel>(key = entryId) { parametersOf(entryId) }
    val detail by viewModel.detail.collectAsStateWithLifecycle()
    val photos = detail?.photos.orEmpty()
    val colors = DiaryTheme.colors
    Box(modifier.fillMaxSize().background(colors.photoBackdrop)) {
        if (photos.isEmpty()) return@Box
        val pager = rememberPagerState(initialPage = index.coerceIn(photos.indices)) { photos.size }
        HorizontalPager(pager, Modifier.fillMaxSize()) { page -> ZoomablePhoto(photos[page]) }
        Box(Modifier.fillMaxWidth().systemBarsPadding().padding(horizontal = DiarySpacing.x1)) {
            val interaction = remember { MutableInteractionSource() }
            Box(
                Modifier
                    .size(DiarySpacing.minTouchTarget)
                    .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                DiaryIcon(DiaryIcons.Close, stringResource(Res.string.detail_photo_close), colors.onAccent)
            }
            BasicText(
                stringResource(Res.string.detail_photo_page, pager.currentPage + 1, photos.size),
                modifier = Modifier.align(Alignment.Center),
                style = DiaryTheme.typography.label.copy(color = colors.onAccent)
            )
        }
        Row(
            Modifier.align(Alignment.BottomCenter).systemBarsPadding().padding(bottom = DiarySpacing.x6),
            horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x1_5)
        ) {
            repeat(photos.size) { page ->
                Box(
                    Modifier
                        .size(DiarySpacing.x1_5)
                        .clip(DiaryShapes.full)
                        .background(if (page == pager.currentPage) colors.onAccent else colors.textTertiary)
                )
            }
        }
    }
}

/** 키우지 않았을 때는 손가락 하나를 넘기기에 넘긴다. 두 손가락이거나 이미 키웠을 때만 여기서 받는다. */
@Composable
private fun ZoomablePhoto(photo: EntryPhoto) {
    val colors = DiaryTheme.colors
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(photo.id) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val fingers = event.changes.count { it.pressed }
                        if (fingers >= 2 || scale > 1f) {
                            scale = (scale * event.calculateZoom()).coerceIn(1f, MAX_ZOOM)
                            offset = if (scale > 1f) offset + event.calculatePan() else Offset.Zero
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (photo.path == null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(DiarySpacing.x2)) {
                DiaryIcon(if (photo.pending) DiaryIcons.Image else DiaryIcons.ImageOff, contentDescription = null, tint = colors.onAccent)
                BasicText(
                    stringResource(if (photo.pending) Res.string.detail_elsewhere_photo else Res.string.detail_missing_photo),
                    style = DiaryTheme.typography.body.copy(color = colors.onAccent)
                )
            }
        } else {
            AsyncImage(
                model = photo.path,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier =
                    Modifier.fillMaxSize().graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
            )
        }
    }
}
