package org.sjhstudio.diary.feature.write

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.ui.BasicRichTextEditor
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberCameraPickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.readBytes
import kotlin.math.roundToInt
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.component.DiaryBottomSheet
import org.sjhstudio.diary.core.designsystem.component.DiaryButton
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonKind
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonSize
import org.sjhstudio.diary.core.designsystem.component.DiaryChip
import org.sjhstudio.diary.core.designsystem.component.DiaryDialog
import org.sjhstudio.diary.core.designsystem.component.DiaryIcon
import org.sjhstudio.diary.core.designsystem.component.DiaryNotice
import org.sjhstudio.diary.core.designsystem.component.diaryPress
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryIconSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.model.ContextTip
import org.sjhstudio.diary.core.model.DraftPhoto
import org.sjhstudio.diary.core.model.InkColor
import org.sjhstudio.diary.core.model.MarkColor
import org.sjhstudio.diary.core.model.PhotoLimit
import org.sjhstudio.diary.core.model.PickedImage
import org.sjhstudio.diary.core.ui.ContextTipBar
import org.sjhstudio.diary.core.ui.DiaryPhotoCell
import org.sjhstudio.diary.core.ui.MoodFace
import org.sjhstudio.diary.core.ui.icon
import org.sjhstudio.diary.core.ui.moodName
import org.sjhstudio.diary.core.ui.rememberDiaryDateFormat
import org.sjhstudio.diary.core.ui.weatherName
import org.sjhstudio.diary.feature.write.resources.Res
import org.sjhstudio.diary.feature.write.resources.tip_write_tools
import org.sjhstudio.diary.feature.write.resources.write_add_photo
import org.sjhstudio.diary.feature.write.resources.write_camera
import org.sjhstudio.diary.feature.write.resources.write_close
import org.sjhstudio.diary.feature.write.resources.write_color_blue
import org.sjhstudio.diary.feature.write.resources.write_color_gray
import org.sjhstudio.diary.feature.write.resources.write_color_green
import org.sjhstudio.diary.feature.write.resources.write_color_orange
import org.sjhstudio.diary.feature.write.resources.write_color_pink
import org.sjhstudio.diary.feature.write.resources.write_color_purple
import org.sjhstudio.diary.feature.write.resources.write_color_red
import org.sjhstudio.diary.feature.write.resources.write_color_yellow
import org.sjhstudio.diary.feature.write.resources.write_discard_confirm
import org.sjhstudio.diary.feature.write.resources.write_discard_message
import org.sjhstudio.diary.feature.write.resources.write_discard_title
import org.sjhstudio.diary.feature.write.resources.write_format_bold
import org.sjhstudio.diary.feature.write.resources.write_format_color
import org.sjhstudio.diary.feature.write.resources.write_format_list
import org.sjhstudio.diary.feature.write.resources.write_format_mark
import org.sjhstudio.diary.feature.write.resources.write_format_underline
import org.sjhstudio.diary.feature.write.resources.write_hint
import org.sjhstudio.diary.feature.write.resources.write_ink_default
import org.sjhstudio.diary.feature.write.resources.write_limit_body
import org.sjhstudio.diary.feature.write.resources.write_limit_later
import org.sjhstudio.diary.feature.write.resources.write_limit_plus
import org.sjhstudio.diary.feature.write.resources.write_limit_title
import org.sjhstudio.diary.feature.write.resources.write_mark_none
import org.sjhstudio.diary.feature.write.resources.write_mood
import org.sjhstudio.diary.feature.write.resources.write_palette_back
import org.sjhstudio.diary.feature.write.resources.write_photo_count
import org.sjhstudio.diary.feature.write.resources.write_photo_limit
import org.sjhstudio.diary.feature.write.resources.write_photo_no_space
import org.sjhstudio.diary.feature.write.resources.write_photo_remove
import org.sjhstudio.diary.feature.write.resources.write_photo_unreadable
import org.sjhstudio.diary.feature.write.resources.write_place
import org.sjhstudio.diary.feature.write.resources.write_save
import org.sjhstudio.diary.feature.write.resources.write_save_failed
import org.sjhstudio.diary.feature.write.resources.write_weather

/** 화면 동작 묶음. 화면은 상태만 받아 그린다. */
internal class WriteActions(
    val viewModel: WriteViewModel,
    val rich: RichTextState,
    val colors: EditorColors,
    val mediaPath: (String) -> String,
    val onPlus: () -> Unit = {}
)

private enum class Sheet { Mood, Weather, Place, DateTime }

/**
 * 쓰기 화면(Figma "쓰기 (S2)"). 위 줄, 첨부 칩, 사진 줄, 본문, 서식 도구 줄 순서다.
 * 폭 840부터는 본문을 가운데 두고 기분·날씨·사진·장소를 옆 칸으로 옮긴다.
 * 사진 고르기는 iOS 때문에 시트가 아닌 이 화면에서 기억한다(research R4).
 */
@Composable
internal fun WriteScreen(state: WriteUiState, actions: WriteActions, bodyStyle: TextStyle, modifier: Modifier = Modifier) {
    val vm = actions.viewModel
    val colors = DiaryTheme.colors
    var sheet by remember { mutableStateOf<Sheet?>(null) }
    var askDiscard by remember { mutableStateOf(false) }
    val photoLimit by vm.photoLimit.collectAsStateWithLifecycle()
    (state.photoNotice as? PhotoNotice.Limit)?.takeIf { it.canUpgrade }?.let { notice ->
        PhotoLimitSheet(notice.limit, actions.onPlus, vm::photoNoticeShown)
    }
    val remaining = (photoLimit - state.photos.size).coerceAtLeast(1)
    val pick =
        rememberFilePickerLauncher(type = FileKitType.Image, mode = FileKitMode.Multiple(maxItems = remaining)) { files ->
            if (files != null) vm.addPhotos(files.map { file -> PickedImage { file.readBytes() } })
        }
    val camera =
        rememberCameraPickerLauncher { file ->
            if (file != null) vm.addPhotos(listOf(PickedImage { file.readBytes() }))
        }
    val tools = remember(actions) { WriteTools(actions.rich, actions.colors) }
    // 하드웨어 키보드의 Ctrl/⌘+S로 저장한다(FR-039).
    val keyboard =
        Modifier.onPreviewKeyEvent { event ->
            val save = event.type == KeyEventType.KeyDown && (event.isCtrlPressed || event.isMetaPressed) && event.key == Key.S
            if (save && state.canSave) vm.save()
            save
        }
    BoxWithConstraints(modifier.then(keyboard).fillMaxSize().background(colors.canvas).systemBarsPadding().imePadding()) {
        val wide = maxWidth >= DiaryWriteSize.twoPaneMin
        Column(Modifier.fillMaxSize()) {
            TopRow(state, onClose = vm::close, onDate = { sheet = Sheet.DateTime }, onSave = vm::save)
            if (wide) {
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Editor(actions.rich, bodyStyle, Modifier.weight(1f).widthIn(max = DiaryWriteSize.bodyMax).fillMaxWidth())
                        ContextTipBar(ContextTip.WRITE_TOOLS, stringResource(Res.string.tip_write_tools))
                        tools.Toolbar(onPhoto = { pick.launch() }, onCamera = { camera.launch() })
                    }
                    Box(Modifier.width(DiarySpacing.hairline).fillMaxHeight().background(colors.line))
                    SidePanel(state, actions, onPick = { pick.launch() }, onPlace = { sheet = Sheet.Place })
                }
            } else {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    Chips(state, onMood = { sheet = Sheet.Mood }, onWeather = { sheet = Sheet.Weather }, onPlace = { sheet = Sheet.Place })
                    if (state.photos.isNotEmpty()) PhotoRow(state, actions, onPick = { pick.launch() })
                    Notices(state)
                    Editor(actions.rich, bodyStyle, Modifier.fillMaxWidth())
                }
                ContextTipBar(ContextTip.WRITE_TOOLS, stringResource(Res.string.tip_write_tools))
                tools.Toolbar(onPhoto = { pick.launch() }, onCamera = { camera.launch() })
            }
        }
    }
    when (sheet) {
        Sheet.Mood ->
            MoodSheet(
                date = state.date,
                mood = state.mood,
                onMood = vm::selectMood,
                onClear = {
                    state.mood?.let(vm::selectMood)
                    sheet = null
                },
                onDate = { sheet = Sheet.DateTime },
                onDismiss = { sheet = null }
            )

        Sheet.Weather ->
            WeatherSheet(
                weather = state.weather,
                onWeather = vm::selectWeather,
                onClear = {
                    state.weather?.let(vm::selectWeather)
                    sheet = null
                },
                onDismiss = { sheet = null }
            )

        Sheet.Place ->
            PlaceSheet(state.placeName, state.recentPlaces, onDone = {
                vm.setPlace(it)
                sheet = null
            }, onDismiss = { sheet = null })

        Sheet.DateTime ->
            DateTimeSheet(state.date, state.time, onDone = { date: LocalDate, time: LocalTime ->
                vm.setDate(date)
                vm.setTime(time)
                sheet = null
            }, onDismiss = { sheet = null })

        null -> Unit
    }
    state.resume?.let { offer ->
        ResumeSheet(offer, onContinue = vm::continueDraft, onFresh = vm::startFresh, onDiscard = { askDiscard = true })
    }
    if (askDiscard) {
        DiaryDialog(
            title = stringResource(Res.string.write_discard_title),
            message = stringResource(Res.string.write_discard_message),
            confirmLabel = stringResource(Res.string.write_discard_confirm),
            destructive = true,
            onConfirm = {
                askDiscard = false
                vm.discardOffered()
            },
            onDismiss = { askDiscard = false }
        )
    }
}

@Composable
private fun TopRow(state: WriteUiState, onClose: () -> Unit, onDate: () -> Unit, onSave: () -> Unit) {
    val colors = DiaryTheme.colors
    val dates = rememberDiaryDateFormat()
    Box(Modifier.fillMaxWidth().height(DiarySpacing.x14)) {
        ToolButton(
            DiaryIcons.Close,
            stringResource(Res.string.write_close),
            colors.textPrimary,
            Modifier.align(Alignment.CenterStart).padding(start = DiarySpacing.x1),
            onClick = onClose
        )
        val interaction = remember { MutableInteractionSource() }
        Row(
            Modifier
                .align(Alignment.Center)
                .clip(DiaryShapes.r8)
                .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onDate)
                .padding(DiarySpacing.x1),
            horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x1),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicText(
                dates.dayWithWeekday(state.date),
                style = DiaryTheme.typography.headline.copy(color = colors.textPrimary),
                maxLines = 1
            )
            DiaryIcon(DiaryIcons.ChevronDown, contentDescription = null, tint = colors.textSecondary, size = DiaryIconSize.i20)
        }
        DiaryButton(
            stringResource(Res.string.write_save),
            onSave,
            Modifier.align(Alignment.CenterEnd),
            kind = DiaryButtonKind.Text,
            enabled = state.canSave
        )
    }
}

@Composable
private fun Chips(state: WriteUiState, onMood: () -> Unit, onWeather: () -> Unit, onPlace: () -> Unit) {
    Row(
        Modifier.padding(horizontal = DiarySpacing.screenCompact, vertical = DiarySpacing.x2),
        horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2)
    ) {
        val mood = state.mood
        DiaryChip(
            text = mood?.let { moodName(it) } ?: stringResource(Res.string.write_mood),
            selected = mood != null,
            onClick = onMood,
            leadingIcon = if (mood == null) DiaryIcons.Smile else null,
            leading = mood?.let { { MoodFace(it, DiaryIconSize.i20) } }
        )
        val weather = state.weather
        DiaryChip(
            text = weather?.let { weatherName(it) } ?: stringResource(Res.string.write_weather),
            selected = weather != null,
            onClick = onWeather,
            leadingIcon = weather?.icon() ?: DiaryIcons.WeatherMostlyCloudy
        )
        DiaryChip(
            text = state.placeName.ifEmpty { stringResource(Res.string.write_place) },
            selected = state.placeName.isNotEmpty(),
            onClick = onPlace,
            leadingIcon = DiaryIcons.Pin
        )
    }
}

@Composable
private fun Notices(state: WriteUiState) {
    val text =
        when (val notice = state.photoNotice) {
            // 더 넣을 수 있는 길이 있으면 시트로 알린다(Figma P3).
            is PhotoNotice.Limit -> if (notice.canUpgrade) null else stringResource(Res.string.write_photo_limit, notice.limit)

            PhotoNotice.NoSpace -> stringResource(Res.string.write_photo_no_space)

            PhotoNotice.Unreadable -> stringResource(Res.string.write_photo_unreadable)

            null -> if (state.saveFailed) stringResource(Res.string.write_save_failed) else null
        } ?: return
    DiaryNotice(text, Modifier.padding(horizontal = DiarySpacing.screenCompact, vertical = DiarySpacing.x2), icon = DiaryIcons.Info)
}

/** 사진 한도 시트(Figma P3). 고른 사진과 글은 그대로라고 먼저 말하고, 쓰는 일을 막지 않는다. */
@Composable
private fun PhotoLimitSheet(limit: Int, onPlus: () -> Unit, onDismiss: () -> Unit) {
    val colors = DiaryTheme.colors
    DiaryBottomSheet(onDismiss, title = stringResource(Res.string.write_limit_title, limit)) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(DiarySpacing.x6)) {
            BasicText(
                stringResource(Res.string.write_limit_body, PhotoLimit.PLUS),
                style = DiaryTheme.typography.body.copy(color = colors.textSecondary)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x3)) {
                DiaryButton(
                    stringResource(Res.string.write_limit_later),
                    onDismiss,
                    Modifier.weight(1f),
                    kind = DiaryButtonKind.Weak,
                    size = DiaryButtonSize.XLarge
                )
                DiaryButton(
                    stringResource(Res.string.write_limit_plus),
                    {
                        onDismiss()
                        onPlus()
                    },
                    Modifier.weight(2f),
                    size = DiaryButtonSize.XLarge
                )
            }
        }
    }
}

/** 길게 눌러 옆으로 끌면 순서가 바뀐다(FR-007). */
@Composable
private fun PhotoRow(state: WriteUiState, actions: WriteActions, onPick: () -> Unit, inset: Dp? = DiarySpacing.screenCompact) {
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
                onMoved = { by -> vm.movePhoto(index, (index + by).coerceIn(0, state.photos.lastIndex)) }
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
private fun PhotoCell(photo: DraftPhoto, mediaPath: (String) -> String, step: Dp, onRemove: () -> Unit, onMoved: (Int) -> Unit) {
    val colors = DiaryTheme.colors
    Box(
        Modifier
            .size(DiaryWriteSize.photo)
            .pointerInput(photo.sha256) {
                var shift = 0f
                detectDragGesturesAfterLongPress(
                    onDragStart = { shift = 0f },
                    onDragEnd = { onMoved((shift / step.toPx()).roundToInt()) }
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
                .size(DiarySpacing.x8)
                .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onRemove),
            contentAlignment = Alignment.TopEnd
        ) {
            Box(
                Modifier.padding(DiarySpacing.x1).size(DiaryWriteSize.removeBadge).clip(DiaryShapes.full).background(colors.overlay),
                contentAlignment = Alignment.Center
            ) {
                DiaryIcon(DiaryIcons.Close, stringResource(Res.string.write_photo_remove), colors.onAccent, size = DiaryIconSize.i16)
            }
        }
    }
}

@Composable
private fun Editor(state: RichTextState, style: TextStyle, modifier: Modifier) {
    val colors = DiaryTheme.colors
    Box(modifier.padding(horizontal = DiarySpacing.screenCompact, vertical = DiarySpacing.x3)) {
        if (state.annotatedString.isEmpty()) {
            BasicText(stringResource(Res.string.write_hint), style = style.copy(color = colors.placeholder))
        }
        BasicRichTextEditor(
            state = state,
            modifier = Modifier.fillMaxWidth().heightIn(min = DiaryWriteSize.editorMin),
            textStyle = style.copy(color = colors.textPrimary),
            cursorBrush = SolidColor(colors.accent)
        )
    }
}

/** 넓은 화면 옆 칸: 기분, 날씨, 사진, 장소(Figma "S2 · 태블릿"). */
@Composable
private fun SidePanel(state: WriteUiState, actions: WriteActions, onPick: () -> Unit, onPlace: () -> Unit) {
    val vm = actions.viewModel
    Column(
        Modifier
            .width(DiaryWriteSize.sidePanel)
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(DiarySpacing.x6),
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x6)
    ) {
        Section(stringResource(Res.string.write_mood)) { MoodGrid(state.mood, vm::selectMood) }
        Section(stringResource(Res.string.write_weather)) { WeatherGrid(state.weather, vm::selectWeather) }
        Section(stringResource(Res.string.write_add_photo)) { PhotoRow(state, actions, onPick, inset = null) }
        Section(stringResource(Res.string.write_place)) {
            DiaryChip(
                text = state.placeName.ifEmpty { stringResource(Res.string.write_place) },
                selected = state.placeName.isNotEmpty(),
                onClick = onPlace,
                leadingIcon = DiaryIcons.Pin
            )
        }
        Notices(state)
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(DiarySpacing.x2)) {
        BasicText(title, style = DiaryTheme.typography.captionStrong.copy(color = DiaryTheme.colors.textSecondary))
        content()
    }
}

/** 도구 줄이 바뀌는 팔레트. 글자색과 형광펜은 쓰임이 달라 각자의 색만 보인다(2026-11-05 사용자 지적). */
private enum class PaletteKind { Ink, Mark }

/** 키보드 위 서식 도구(Figma "서식 도구 줄"). 글자색·형광펜을 누르면 줄이 그 팔레트로 바뀐다. */
private class WriteTools(val rich: RichTextState, val colors: EditorColors) {
    @Composable
    fun Toolbar(onPhoto: (() -> Unit)?, onCamera: (() -> Unit)?) {
        var palette by remember { mutableStateOf<PaletteKind?>(null) }
        val theme = DiaryTheme.colors
        Column(Modifier.fillMaxWidth().background(theme.canvas)) {
            Box(Modifier.fillMaxWidth().height(DiarySpacing.hairline).background(theme.line))
            Row(
                Modifier.fillMaxWidth().height(DiaryWriteSize.toolbar).padding(horizontal = DiarySpacing.x1),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x0_5)
            ) {
                val open = palette
                if (open != null) Palette(open) { palette = null } else Tools(onPhoto, onCamera) { palette = it }
            }
        }
    }

    @Composable
    private fun Tools(onPhoto: (() -> Unit)?, onCamera: (() -> Unit)?, openPalette: (PaletteKind) -> Unit) {
        val theme = DiaryTheme.colors
        val style = rich.currentSpanStyle
        fun tint(active: Boolean) = if (active) theme.accentText else theme.textPrimary
        onPhoto?.let { ToolButton(DiaryIcons.Image, stringResource(Res.string.write_add_photo), theme.textPrimary, onClick = it) }
        onCamera?.let { ToolButton(DiaryIcons.Camera, stringResource(Res.string.write_camera), theme.textPrimary, onClick = it) }
        val bold = style.fontWeight == FontWeight.Bold
        ToolButton(DiaryIcons.Bold, stringResource(Res.string.write_format_bold), tint(bold), active = bold) {
            rich.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
        }
        val underline = style.textDecoration?.contains(TextDecoration.Underline) == true
        ToolButton(DiaryIcons.Underline, stringResource(Res.string.write_format_underline), tint(underline), active = underline) {
            rich.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline))
        }
        // 글자색 도구는 지금 고른 색을 그대로 보인다.
        ToolButton(DiaryIcons.TextColor, stringResource(Res.string.write_format_color), style.color.takeOrElse { theme.textPrimary }) {
            openPalette(PaletteKind.Ink)
        }
        val marked = style.background.isSpecified
        ToolButton(DiaryIcons.Highlight, stringResource(Res.string.write_format_mark), tint(marked), active = marked) {
            openPalette(PaletteKind.Mark)
        }
        ToolButton(
            DiaryIcons.BulletList,
            stringResource(Res.string.write_format_list),
            tint(rich.isUnorderedList),
            active = rich.isUnorderedList
        ) {
            rich.toggleUnorderedList()
        }
    }

    @Composable
    private fun RowScope.Palette(kind: PaletteKind, close: () -> Unit) {
        val theme = DiaryTheme.colors
        val style = rich.currentSpanStyle
        ToolButton(DiaryIcons.ChevronLeft, stringResource(Res.string.write_palette_back), theme.textPrimary, onClick = close)
        // 고른 색이 몇 개든 한 번에 풀어서, 여러 색이 섞인 글도 기본으로 돌아간다.
        fun clearInk() = colors.ink.values.forEach { rich.removeSpanStyle(SpanStyle(color = it)) }
        fun clearMark() = colors.mark.values.forEach { rich.removeSpanStyle(SpanStyle(background = it)) }
        Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
            when (kind) {
                PaletteKind.Ink -> {
                    Swatch(stringResource(Res.string.write_ink_default), selected = style.color.isUnspecified, onClick = ::clearInk) {
                        ColorChip(theme.textPrimary, DiaryShapes.full)
                    }
                    for (ink in InkColor.entries) {
                        val color = colors.ink.getValue(ink)
                        Swatch(stringResource(ink.label()), selected = style.color == color, onClick = {
                            clearInk()
                            rich.addSpanStyle(SpanStyle(color = color))
                        }) { ColorChip(color, DiaryShapes.full) }
                    }
                }

                PaletteKind.Mark -> {
                    Swatch(
                        stringResource(Res.string.write_mark_none),
                        selected = style.background.isUnspecified,
                        ring = DiaryShapes.r12,
                        onClick = ::clearMark
                    ) {
                        Box(
                            Modifier.size(DiaryWriteSize.swatch).border(DiarySpacing.hairline, theme.lineStrong, DiaryShapes.r8),
                            contentAlignment = Alignment.Center
                        ) {
                            DiaryIcon(DiaryIcons.Close, contentDescription = null, tint = theme.textTertiary, size = DiaryIconSize.i16)
                        }
                    }
                    for (mark in MarkColor.entries) {
                        val color = colors.mark.getValue(mark)
                        Swatch(stringResource(mark.label()), selected = style.background == color, ring = DiaryShapes.r12, onClick = {
                            clearMark()
                            rich.addSpanStyle(SpanStyle(background = color))
                        }) { ColorChip(color, DiaryShapes.r8) }
                    }
                }
            }
        }
    }
}

private fun InkColor.label(): StringResource = when (this) {
    InkColor.GRAY -> Res.string.write_color_gray
    InkColor.RED -> Res.string.write_color_red
    InkColor.ORANGE -> Res.string.write_color_orange
    InkColor.GREEN -> Res.string.write_color_green
    InkColor.BLUE -> Res.string.write_color_blue
    InkColor.PURPLE -> Res.string.write_color_purple
    InkColor.PINK -> Res.string.write_color_pink
}

private fun MarkColor.label(): StringResource = when (this) {
    MarkColor.YELLOW -> Res.string.write_color_yellow
    MarkColor.GREEN -> Res.string.write_color_green
    MarkColor.BLUE -> Res.string.write_color_blue
    MarkColor.PURPLE -> Res.string.write_color_purple
    MarkColor.PINK -> Res.string.write_color_pink
}

@Composable
private fun ColorChip(color: Color, shape: Shape) {
    Box(Modifier.size(DiaryWriteSize.swatch).background(color, shape))
}

/** 고른 색은 액센트 고리로 보인다. 고리와 색 사이를 띄워서 보라 칸에서도 고리가 구별된다. */
@Composable
private fun Swatch(
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    ring: Shape = DiaryShapes.full,
    chip: @Composable () -> Unit
) {
    val theme = DiaryTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier
            .size(width = DiaryWriteSize.swatch + DiarySpacing.x3, height = DiarySpacing.minTouchTarget)
            .selectable(selected, interactionSource = interaction, indication = null, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        if (selected) Box(Modifier.size(DiaryWriteSize.swatch + DiarySpacing.x2).border(DiarySpacing.x0_5, theme.accent, ring))
        chip()
    }
}

@Composable
private fun ToolButton(
    icon: ImageVector,
    description: String,
    tint: Color,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    onClick: () -> Unit
) {
    val theme = DiaryTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .size(DiarySpacing.minTouchTarget)
            .diaryPress(interaction, theme.textPrimary, DiaryShapes.full)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // 켜진 도구는 색만이 아니라 바탕으로도 알린다.
        if (active) Box(Modifier.size(DiaryWriteSize.swatch + DiarySpacing.x2).background(theme.selected, DiaryShapes.r12))
        DiaryIcon(icon, contentDescription = description, tint = tint)
    }
}
