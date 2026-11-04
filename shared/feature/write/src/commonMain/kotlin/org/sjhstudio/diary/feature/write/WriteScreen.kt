package org.sjhstudio.diary.feature.write

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
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
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.component.DiaryButton
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonKind
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
import org.sjhstudio.diary.core.model.DraftPhoto
import org.sjhstudio.diary.core.model.InkColor
import org.sjhstudio.diary.core.model.MarkColor
import org.sjhstudio.diary.core.model.PhotoLimit
import org.sjhstudio.diary.core.model.PickedImage
import org.sjhstudio.diary.core.ui.DiaryPhotoCell
import org.sjhstudio.diary.core.ui.MoodFace
import org.sjhstudio.diary.core.ui.icon
import org.sjhstudio.diary.core.ui.moodName
import org.sjhstudio.diary.core.ui.rememberDiaryDateFormat
import org.sjhstudio.diary.core.ui.weatherName
import org.sjhstudio.diary.feature.write.resources.Res
import org.sjhstudio.diary.feature.write.resources.write_add_photo
import org.sjhstudio.diary.feature.write.resources.write_camera
import org.sjhstudio.diary.feature.write.resources.write_close
import org.sjhstudio.diary.feature.write.resources.write_discard_confirm
import org.sjhstudio.diary.feature.write.resources.write_discard_message
import org.sjhstudio.diary.feature.write.resources.write_discard_title
import org.sjhstudio.diary.feature.write.resources.write_format_bold
import org.sjhstudio.diary.feature.write.resources.write_format_color
import org.sjhstudio.diary.feature.write.resources.write_format_list
import org.sjhstudio.diary.feature.write.resources.write_format_mark
import org.sjhstudio.diary.feature.write.resources.write_format_underline
import org.sjhstudio.diary.feature.write.resources.write_hint
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
    val mediaPath: (String) -> String
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
    val remaining = (PhotoLimit.FREE - state.photos.size).coerceAtLeast(1)
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
            is PhotoNotice.Limit -> stringResource(Res.string.write_photo_limit, notice.limit)
            PhotoNotice.NoSpace -> stringResource(Res.string.write_photo_no_space)
            PhotoNotice.Unreadable -> stringResource(Res.string.write_photo_unreadable)
            null -> if (state.saveFailed) stringResource(Res.string.write_save_failed) else null
        } ?: return
    DiaryNotice(text, Modifier.padding(horizontal = DiarySpacing.screenCompact, vertical = DiarySpacing.x2), icon = DiaryIcons.Info)
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
                    stringResource(Res.string.write_photo_count, state.photos.size, PhotoLimit.FREE),
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
                horizontalArrangement = Arrangement.spacedBy(if (palette != null) DiarySpacing.x2 else DiarySpacing.x0_5)
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
        ToolButton(DiaryIcons.Bold, stringResource(Res.string.write_format_bold), tint(bold)) {
            rich.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
        }
        val underline = style.textDecoration?.contains(TextDecoration.Underline) == true
        ToolButton(DiaryIcons.Underline, stringResource(Res.string.write_format_underline), tint(underline)) {
            rich.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline))
        }
        ToolButton(DiaryIcons.TextColor, stringResource(Res.string.write_format_color), tint(style.color.isSpecified)) {
            openPalette(PaletteKind.Ink)
        }
        ToolButton(DiaryIcons.Highlight, stringResource(Res.string.write_format_mark), tint(style.background.isSpecified)) {
            openPalette(PaletteKind.Mark)
        }
        ToolButton(DiaryIcons.BulletList, stringResource(Res.string.write_format_list), tint(rich.isUnorderedList)) {
            rich.toggleUnorderedList()
        }
    }

    @Composable
    private fun Palette(kind: PaletteKind, close: () -> Unit) {
        val theme = DiaryTheme.colors
        val style = rich.currentSpanStyle
        ToolButton(DiaryIcons.ChevronLeft, stringResource(Res.string.write_palette_back), theme.textPrimary, onClick = close)
        when (kind) {
            PaletteKind.Ink ->
                for (ink in InkColor.entries) {
                    val color = colors.ink.getValue(ink)
                    Swatch(color, DiaryShapes.full, selected = style.color == color) {
                        val span = SpanStyle(color = color)
                        if (style.color == color) rich.removeSpanStyle(span) else rich.addSpanStyle(span)
                    }
                }

            PaletteKind.Mark ->
                for (mark in MarkColor.entries) {
                    val color = colors.mark.getValue(mark)
                    Swatch(color, DiaryShapes.r8, selected = style.background == color) {
                        val span = SpanStyle(background = color)
                        if (style.background == color) rich.removeSpanStyle(span) else rich.addSpanStyle(span)
                    }
                }
        }
    }
}

@Composable
private fun Swatch(color: Color, shape: Shape, selected: Boolean, onClick: () -> Unit) {
    val theme = DiaryTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier
            .size(DiaryWriteSize.swatch)
            .clip(shape)
            .background(color)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) DiaryIcon(DiaryIcons.Check, contentDescription = null, tint = theme.textPrimary, size = DiaryIconSize.i16)
    }
}

@Composable
private fun ToolButton(icon: ImageVector, description: String, tint: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .size(DiarySpacing.minTouchTarget)
            .diaryPress(interaction, DiaryTheme.colors.textPrimary, DiaryShapes.full)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        DiaryIcon(icon, contentDescription = description, tint = tint)
    }
}
