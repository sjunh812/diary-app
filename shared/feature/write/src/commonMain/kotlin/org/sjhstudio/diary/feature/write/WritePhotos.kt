package org.sjhstudio.diary.feature.write

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.component.DiaryIcon
import org.sjhstudio.diary.core.designsystem.component.diaryPress
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryIconSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.designsystem.theme.LocalScreenGutter
import org.sjhstudio.diary.core.model.DraftPhoto
import org.sjhstudio.diary.core.ui.DiaryPhotoCell
import org.sjhstudio.diary.feature.write.resources.Res
import org.sjhstudio.diary.feature.write.resources.write_add_photo
import org.sjhstudio.diary.feature.write.resources.write_photo_count
import org.sjhstudio.diary.feature.write.resources.write_photo_remove

// 쓰기 화면의 사진 칸

/** 끄는 사진을 살짝 키워 집어 든 것처럼 */
private const val DRAG_SCALE = 1.06f

/** 길게 눌러 옆으로 끌면 순서가 바뀜(FR-007) */
@Composable
internal fun PhotoRow(state: WriteUiState, actions: WriteActions, onPick: () -> Unit, inset: Dp? = LocalScreenGutter.current) {
    val colors = DiaryTheme.colors
    val vm = actions.viewModel
    val step = DiaryWriteSize.photo + DiarySpacing.x2
    LazyRow(
        contentPadding = inset?.let { PaddingValues(horizontal = it, vertical = DiarySpacing.x2) } ?: PaddingValues(),
        horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2)
    ) {
        itemsIndexed(state.photos, key = { index, photo -> "$index-${photo.sha256}" }) { index, photo ->
            PhotoCell(
                photo = photo,
                mediaPath = actions.mediaPath,
                step = step,
                onRemove = { vm.removePhoto(index) },
                onMove = { by -> vm.movePhoto(index, by) }
            )
        }
        item {
            val interaction = remember { MutableInteractionSource() }
            val photoLimit by actions.viewModel.photoLimit.collectAsStateWithLifecycle()
            Column(
                Modifier
                    .size(DiaryWriteSize.photo)
                    .diaryPress(interaction, colors.textPrimary, DiaryShapes.r12)
                    .clip(DiaryShapes.r12)
                    .background(colors.sunken)
                    .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onPick),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                DiaryIcon(DiaryIcons.Image, stringResource(Res.string.write_add_photo), colors.textSecondary, size = DiaryIconSize.i20)
                BasicText(
                    stringResource(Res.string.write_photo_count, state.photos.size, photoLimit),
                    style = DiaryTheme.typography.caption.copy(color = colors.textSecondary)
                )
            }
        }
    }
}

@Composable
internal fun PhotoCell(photo: DraftPhoto, mediaPath: (String) -> String, step: Dp, onRemove: () -> Unit, onMove: (Int) -> Unit) {
    val colors = DiaryTheme.colors
    val haptic = LocalHapticFeedback.current
    var shift by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    Box(
        Modifier
            .zIndex(if (dragging) 1f else 0f)
            .graphicsLayer {
                // 끄는 동안 손을 따라오게 그림
                translationX = shift
                scaleX = if (dragging) DRAG_SCALE else 1f
                scaleY = if (dragging) DRAG_SCALE else 1f
            }
            .size(DiaryWriteSize.photo)
            .pointerInput(photo.sha256) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        shift = 0f
                        dragging = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDragEnd = {
                        val by = (shift / step.toPx()).roundToInt()
                        shift = 0f
                        dragging = false
                        onMove(by)
                    },
                    onDragCancel = {
                        shift = 0f
                        dragging = false
                    }
                ) { change, amount ->
                    change.consume()
                    shift += amount.x
                }
            }
    ) {
        Box(Modifier.fillMaxSize().clip(DiaryShapes.r12)) { DiaryPhotoCell(listOf(mediaPath(photo.fileName))) }
        val interaction = remember { MutableInteractionSource() }
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .size(DiarySpacing.minTouchTarget)
                .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onRemove),
            contentAlignment = Alignment.TopEnd
        ) {
            Box(
                Modifier
                    .padding(DiarySpacing.x1)
                    .size(DiaryWriteSize.removeBadge)
                    .diaryPress(interaction, colors.onAccent, DiaryShapes.full)
                    .clip(DiaryShapes.full)
                    .background(colors.onPhoto),
                contentAlignment = Alignment.Center
            ) {
                DiaryIcon(DiaryIcons.Close, stringResource(Res.string.write_photo_remove), colors.onAccent, size = DiaryIconSize.i12)
            }
        }
    }
}
