package org.sjhstudio.diary.feature.trash

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.sjhstudio.diary.core.designsystem.component.DiaryBottomSheet
import org.sjhstudio.diary.core.designsystem.component.DiaryDialog
import org.sjhstudio.diary.core.designsystem.component.DiaryEmptyArea
import org.sjhstudio.diary.core.designsystem.component.DiaryEmptyState
import org.sjhstudio.diary.core.designsystem.component.DiaryNotice
import org.sjhstudio.diary.core.designsystem.component.DiarySheetAction
import org.sjhstudio.diary.core.designsystem.component.DiaryToastHost
import org.sjhstudio.diary.core.designsystem.component.DiaryToastState
import org.sjhstudio.diary.core.designsystem.component.DiaryTopBar
import org.sjhstudio.diary.core.designsystem.component.DiaryTopBarAction
import org.sjhstudio.diary.core.designsystem.component.diaryPress
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryIconSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.designsystem.theme.LocalScreenGutter
import org.sjhstudio.diary.core.model.TrashedEntry
import org.sjhstudio.diary.core.ui.MoodFace
import org.sjhstudio.diary.core.ui.WeatherIcon
import org.sjhstudio.diary.core.ui.moodName
import org.sjhstudio.diary.core.ui.rememberDiaryDateFormat
import org.sjhstudio.diary.core.ui.weatherName
import org.sjhstudio.diary.feature.trash.resources.Res
import org.sjhstudio.diary.feature.trash.resources.trash_confirm_body_all
import org.sjhstudio.diary.feature.trash.resources.trash_confirm_body_one
import org.sjhstudio.diary.feature.trash.resources.trash_confirm_delete
import org.sjhstudio.diary.feature.trash.resources.trash_confirm_title
import org.sjhstudio.diary.feature.trash.resources.trash_days_left
import org.sjhstudio.diary.feature.trash.resources.trash_delete_forever
import org.sjhstudio.diary.feature.trash.resources.trash_empty
import org.sjhstudio.diary.feature.trash.resources.trash_empty_body
import org.sjhstudio.diary.feature.trash.resources.trash_empty_title
import org.sjhstudio.diary.feature.trash.resources.trash_notice
import org.sjhstudio.diary.feature.trash.resources.trash_restore
import org.sjhstudio.diary.feature.trash.resources.trash_restored
import org.sjhstudio.diary.feature.trash.resources.trash_row_when
import org.sjhstudio.diary.feature.trash.resources.trash_title

/** 마지막 날이 다가오면 빨갛게 보인다. */
private const val URGENT_DAYS = 1

/**
 * 휴지통(Figma "휴지통 (S6-7)").
 * 줄을 누르면 복원하기와 완전히 지우기를 고른다.
 */
@Composable
fun TrashRoute(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel = koinViewModel<TrashViewModel>()
    val items by viewModel.items.collectAsStateWithLifecycle()
    var pickedId by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmOne by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmAll by rememberSaveable { mutableStateOf(false) }
    val toast = remember { DiaryToastState() }
    val scope = rememberCoroutineScope()
    val restored = stringResource(Res.string.trash_restored)
    val list = items
    Box(modifier.fillMaxSize()) {
        TrashScreen(list, onBack, onEmpty = { confirmAll = true }, onPick = { pickedId = it })
        DiaryToastHost(toast, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = DiarySpacing.x4))
    }
    pickedId?.let { id ->
        DiaryBottomSheet(onDismiss = { pickedId = null }) {
            DiarySheetAction(DiaryIcons.Restore, stringResource(Res.string.trash_restore), DiaryTheme.colors.textPrimary) {
                pickedId = null
                viewModel.restore(id)
                scope.launch { toast.show(restored) }
            }
            DiarySheetAction(DiaryIcons.Trash, stringResource(Res.string.trash_delete_forever), DiaryTheme.colors.danger) {
                pickedId = null
                confirmOne = id
            }
        }
    }
    confirmOne?.let { id ->
        DiaryDialog(
            title = stringResource(Res.string.trash_confirm_title),
            confirmLabel = stringResource(Res.string.trash_confirm_delete),
            onConfirm = {
                confirmOne = null
                viewModel.deleteForever(id)
            },
            onDismiss = { confirmOne = null },
            message = stringResource(Res.string.trash_confirm_body_one),
            destructive = true
        )
    }
    if (confirmAll) {
        val count = list?.size ?: 0
        DiaryDialog(
            title = stringResource(Res.string.trash_confirm_title),
            confirmLabel = stringResource(Res.string.trash_confirm_delete),
            onConfirm = {
                confirmAll = false
                viewModel.empty()
            },
            onDismiss = { confirmAll = false },
            message = pluralStringResource(Res.plurals.trash_confirm_body_all, count, count),
            destructive = true
        )
    }
}

@Composable
internal fun TrashScreen(items: List<TrashedEntry>?, onBack: () -> Unit, onEmpty: () -> Unit, onPick: (String) -> Unit) {
    val colors = DiaryTheme.colors
    Column(Modifier.fillMaxSize().background(colors.canvas).statusBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        DiaryTopBar(
            title = stringResource(Res.string.trash_title),
            onBack = onBack,
            action =
                if (items.isNullOrEmpty()) {
                    null
                } else {
                    DiaryTopBarAction(stringResource(Res.string.trash_empty), danger = true, onClick = onEmpty)
                }
        )
        when {
            items == null -> Unit

            items.isEmpty() ->
                DiaryEmptyArea {
                    DiaryEmptyState(
                        DiaryIcons.Trash,
                        stringResource(Res.string.trash_empty_title),
                        description = stringResource(Res.string.trash_empty_body)
                    )
                }

            else -> {
                // 마지막 줄이 시스템 내비게이션에 가리지 않게 아래를 비운다.
                val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + DiarySpacing.x6
                LazyColumn(Modifier.widthIn(max = DiaryWriteSize.bodyMax).fillMaxWidth(), contentPadding = PaddingValues(bottom = bottom)) {
                    item {
                        DiaryNotice(
                            stringResource(Res.string.trash_notice),
                            Modifier.padding(horizontal = LocalScreenGutter.current, vertical = DiarySpacing.x2),
                            icon = DiaryIcons.Trash
                        )
                    }
                    itemsIndexed(items, key = { _, item ->
                        item.row.id
                    }) { index, item -> TrashRow(item, dividerAbove = index > 0) { onPick(item.row.id) } }
                }
            }
        }
    }
}

@Composable
private fun TrashRow(item: TrashedEntry, dividerAbove: Boolean, onClick: () -> Unit) {
    val colors = DiaryTheme.colors
    val typography = DiaryTheme.typography
    val dates = rememberDiaryDateFormat()
    val row = item.row
    val interaction = remember { MutableInteractionSource() }
    Column(
        Modifier
            .fillMaxWidth()
            .diaryPress(interaction, colors.textPrimary, DiaryShapes.r12)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
    ) {
        if (dividerAbove) {
            Box(
                Modifier
                    .padding(horizontal = LocalScreenGutter.current)
                    .fillMaxWidth()
                    .height(DiarySpacing.hairline)
                    .background(colors.line)
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = LocalScreenGutter.current, vertical = DiarySpacing.x4),
            horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x3)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(DiarySpacing.x1)) {
                Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x1_5), verticalAlignment = Alignment.CenterVertically) {
                    row.mood?.let { MoodFace(it, DiaryIconSize.i16, contentDescription = moodName(it)) }
                    BasicText(
                        stringResource(Res.string.trash_row_when, dates.monthDay(row.date), dates.time(row.time)),
                        style = typography.captionStrong.copy(color = colors.textSecondary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    row.weather?.let { WeatherIcon(it, DiaryIconSize.i16, colors.textSecondary, contentDescription = weatherName(it)) }
                    row.placeName?.let {
                        // 장소가 길어도 앞의 시각과 날씨를 밀어내지 않는다.
                        BasicText(
                            it,
                            modifier = Modifier.weight(1f, fill = false),
                            style = typography.caption.copy(color = colors.textSecondary),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (row.preview.isNotEmpty()) {
                    BasicText(
                        row.preview,
                        style = typography.body.copy(color = colors.textPrimary),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            BasicText(
                pluralStringResource(Res.plurals.trash_days_left, item.daysLeft, item.daysLeft),
                style = typography.caption.copy(color = if (item.daysLeft <= URGENT_DAYS) colors.danger else colors.textSecondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
