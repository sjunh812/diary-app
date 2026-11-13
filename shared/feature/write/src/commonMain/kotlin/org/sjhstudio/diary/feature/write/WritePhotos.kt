package org.sjhstudio.diary.feature.write

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

/**
 * 길게 눌러 옆으로 끌면 순서가 바뀜(FR-007)
 * 끄는 동안 옆 사진이 비켜 놓일 자리가 보이고, 놓을 때 한 번 저장
 */
@Composable
internal fun PhotoRow(state: WriteUiState, actions: WriteActions, onPick: () -> Unit, inset: Dp? = LocalScreenGutter.current) {
    val colors = DiaryTheme.colors
    val vm = actions.viewModel
    val haptic = LocalHapticFeedback.current
    val list = rememberLazyListState()
    // 끄는 동안의 순서(저장된 목록의 자리 번호). 저장된 목록이 바뀌면 그 순서를 다시 따름
    var order by remember(state.photos) { mutableStateOf(state.photos.indices.toList()) }
    val keyed = remember { PhotoKeys() }
    val keys = remember(state.photos) { keyed.assign(state.photos) }
    val drag = rememberPhotoDrag(
        list = list,
        count = { order.size },
        onSwap = { from, to ->
            order = order.toMutableList().apply { add(to, removeAt(from)) }
            keyed.moved(from, to)
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        },
        onDrop = { from, to -> vm.movePhoto(from, to - from) }
    )
    LazyRow(
        modifier = Modifier.pointerInput(drag) {
            detectDragGesturesAfterLongPress(
                onDragStart = { at -> if (drag.start(at.x)) haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                onDragEnd = drag::drop,
                onDragCancel = drag::drop
            ) { change, amount ->
                change.consume()
                drag.dragBy(amount.x)
            }
        },
        state = list,
        contentPadding = inset?.let { PaddingValues(horizontal = it, vertical = DiarySpacing.x2) } ?: PaddingValues(),
        horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2)
    ) {
        itemsIndexed(order, key = { _, saved -> keys[saved] }) { index, saved ->
            val dragging = drag.dragging == index
            PhotoCell(
                photo = state.photos[saved],
                mediaPath = actions.mediaPath,
                dragging = dragging,
                // 끄는 칸은 손을, 놓은 칸은 제자리로 돌아오는 움직임을 따름. 나머지는 비키는 움직임
                modifier = when {
                    dragging -> Modifier.zIndex(1f).graphicsLayer { translationX = drag.offset }
                    drag.settling == index -> Modifier.zIndex(1f).graphicsLayer { translationX = drag.settleOffset }
                    else -> Modifier.animateItem()
                },
                onRemove = { vm.removePhoto(saved) }
            )
        }
        item(key = ADD_KEY) {
            val interaction = remember { MutableInteractionSource() }
            val photoLimit by actions.viewModel.photoLimit.collectAsStateWithLifecycle()
            Column(
                Modifier
                    .animateItem()
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

private const val ADD_KEY = "add"

/**
 * 사진 줄의 목록 키
 * 같은 사진을 두 번 붙일 수 있어 내용 지문만으로는 키가 겹침
 * 순서를 바꿔도 사진마다 키가 그대로여야 비키는 움직임이 이어지고 줄이 튀지 않음
 */
internal class PhotoKeys {
    // 화면에 보이는 순서대로의 (내용 지문, 키)
    private var shown: List<Pair<String, String>> = emptyList()

    /** 저장된 목록에 키를 붙임. 보이던 사진은 보이던 순서대로 같은 키를 이어받음 */
    fun assign(photos: List<DraftPhoto>): List<String> {
        val kept = shown.groupBy({ it.first }, { it.second }).mapValues { ArrayDeque(it.value) }
        val taken = shown.mapTo(HashSet()) { it.second }
        val keys = photos.map { photo ->
            kept[photo.sha256]?.removeFirstOrNull() ?: generateSequence(0) { it + 1 }.map { "${photo.sha256}-$it" }.first(taken::add)
        }
        shown = photos.map { it.sha256 }.zip(keys)
        return keys
    }

    /** 끄는 동안 보이는 순서가 바뀜 */
    fun moved(from: Int, to: Int) {
        shown = shown.toMutableList().apply { add(to, removeAt(from)) }
    }
}

@Composable
internal fun PhotoCell(
    photo: DraftPhoto,
    mediaPath: (String) -> String,
    dragging: Boolean,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DiaryTheme.colors
    val scale by animateFloatAsState(if (dragging) DRAG_SCALE else 1f)
    Box(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .size(DiaryWriteSize.photo)
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
