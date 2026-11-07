package org.sjhstudio.diary.feature.write

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mohamedrejeb.richeditor.model.RichTextState
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberCameraPickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.readBytes
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.component.DiaryBottomSheet
import org.sjhstudio.diary.core.designsystem.component.DiaryButton
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonKind
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonPair
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
import org.sjhstudio.diary.core.designsystem.theme.LocalScreenGutter
import org.sjhstudio.diary.core.model.Checklist
import org.sjhstudio.diary.core.model.ChecklistEdit
import org.sjhstudio.diary.core.model.DraftPhoto
import org.sjhstudio.diary.core.model.InkColor
import org.sjhstudio.diary.core.model.MarkColor
import org.sjhstudio.diary.core.model.PhotoLimit
import org.sjhstudio.diary.core.model.PickedImage
import org.sjhstudio.diary.core.ui.DiaryPhotoCell
import org.sjhstudio.diary.core.ui.MoodFace
import org.sjhstudio.diary.core.ui.checkLineAt
import org.sjhstudio.diary.core.ui.checklistActions
import org.sjhstudio.diary.core.ui.checklistMarks
import org.sjhstudio.diary.core.ui.icon
import org.sjhstudio.diary.core.ui.moodName
import org.sjhstudio.diary.core.ui.rememberDiaryDateFormat
import org.sjhstudio.diary.core.ui.weatherName
import org.sjhstudio.diary.feature.write.resources.Res
import org.sjhstudio.diary.feature.write.resources.write_add_photo
import org.sjhstudio.diary.feature.write.resources.write_adding_photos
import org.sjhstudio.diary.feature.write.resources.write_camera
import org.sjhstudio.diary.feature.write.resources.write_checklist_plus_body
import org.sjhstudio.diary.feature.write.resources.write_checklist_plus_title
import org.sjhstudio.diary.feature.write.resources.write_close
import org.sjhstudio.diary.feature.write.resources.write_color_blue
import org.sjhstudio.diary.feature.write.resources.write_color_gray
import org.sjhstudio.diary.feature.write.resources.write_color_green
import org.sjhstudio.diary.feature.write.resources.write_color_orange
import org.sjhstudio.diary.feature.write.resources.write_color_pink
import org.sjhstudio.diary.feature.write.resources.write_color_purple
import org.sjhstudio.diary.feature.write.resources.write_color_red
import org.sjhstudio.diary.feature.write.resources.write_color_yellow
import org.sjhstudio.diary.feature.write.resources.write_discard
import org.sjhstudio.diary.feature.write.resources.write_discard_confirm
import org.sjhstudio.diary.feature.write.resources.write_discard_edit
import org.sjhstudio.diary.feature.write.resources.write_discard_edit_title
import org.sjhstudio.diary.feature.write.resources.write_discard_message
import org.sjhstudio.diary.feature.write.resources.write_discard_title
import org.sjhstudio.diary.feature.write.resources.write_entry_changed
import org.sjhstudio.diary.feature.write.resources.write_format_bold
import org.sjhstudio.diary.feature.write.resources.write_format_check
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

/**
 * 화면 동작 묶음.
 * 화면은 상태만 받아 그린다.
 */
internal class WriteActions(
    val viewModel: WriteViewModel,
    val rich: RichTextState,
    val colors: EditorColors,
    val mediaPath: (String) -> String,
    val onPlus: () -> Unit = {}
)

private enum class Sheet { Mood, Weather, Place, DateTime }

/** 끌고 있는 사진을 살짝 키워 집어 든 것처럼 보이게 한다. */
private const val DRAG_SCALE = 1.06f

private const val KEYBOARD_HIDE_MILLIS = 400L

/**
 * 쓰기 화면(Figma "쓰기 (S2)").
 * 위 줄, 첨부 칩, 사진 줄, 본문, 서식 도구 줄 순서다.
 * 폭 840부터는 본문을 가운데 두고 기분·날씨·사진·장소를 옆 칸으로 옮긴다.
 * 사진 고르기는 iOS 때문에 시트가 아닌 이 화면에서 기억한다(research R4).
 */
@Composable
internal fun WriteScreen(state: WriteUiState, actions: WriteActions, bodyStyle: TextStyle, modifier: Modifier = Modifier) {
    val vm = actions.viewModel
    val colors = DiaryTheme.colors
    // 화면을 돌리거나 폴더블을 펴도 고르던 시트가 닫히지 않는다.
    var sheet by rememberSaveable { mutableStateOf<Sheet?>(null) }
    val locationAccess = rememberLocationAccess()
    // 키보드가 내려가는 동안 시트가 뜨면 키보드 높이에 맞춰 떴다가 출렁인다.
    // 키보드를 먼저 내리고 띄운다.
    val focus = LocalFocusManager.current
    val softKeyboard = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    var nextSheet by remember { mutableStateOf<Sheet?>(null) }
    val openSheet = { target: Sheet ->
        focus.clearFocus()
        softKeyboard?.hide()
        nextSheet = target
    }
    LaunchedEffect(nextSheet) {
        val target = nextSheet ?: return@LaunchedEffect
        withTimeoutOrNull(KEYBOARD_HIDE_MILLIS) { snapshotFlow { imeInsets.getBottom(density) }.first { it == 0 } }
        sheet = target
        nextSheet = null
    }
    var askDiscard by remember { mutableStateOf(false) }
    var askDiscardHere by rememberSaveable { mutableStateOf(false) }
    val editorFocus = remember { FocusRequester() }
    // 새 글은 바로 쓸 수 있게 처음 한 번만 커서를 준다.
    // 수정은 먼저 읽어 보는 때가 많아 키보드를 띄우지 않는다.
    var autoFocused by rememberSaveable { mutableStateOf(false) }
    val readyToWrite = !state.loading && state.resume == null && !state.isEdit
    LaunchedEffect(readyToWrite) {
        if (readyToWrite && !autoFocused) {
            autoFocused = true
            editorFocus.requestFocus()
        }
    }
    // 글 아래 빈 곳을 눌러도 쓰기로 이어진다.
    val tapToWrite =
        Modifier.pointerInput(actions.rich) {
            detectTapGestures {
                actions.rich.selection = TextRange(actions.rich.annotatedString.length)
                editorFocus.requestFocus()
                softKeyboard?.show()
            }
        }
    // 쓰는 동안에는 자리를 차지하지 않게 키보드가 내려갔을 때만 보인다.
    val discardRow: @Composable () -> Unit = {
        if (state.canDiscard && imeInsets.getBottom(density) == 0) DiscardButton(state.isEdit) { askDiscardHere = true }
    }
    var checklistLocked by remember { mutableStateOf(false) }
    val photoLimit by vm.photoLimit.collectAsStateWithLifecycle()
    (state.photoNotice as? PhotoNotice.Limit)?.takeIf { it.canUpgrade }?.let { notice ->
        PhotoLimitSheet(notice.limit, actions.onPlus, vm::photoNoticeShown)
    }
    if (checklistLocked) ChecklistPlusSheet(actions.onPlus) { checklistLocked = false }
    val remaining = (photoLimit - state.photos.size).coerceAtLeast(1)
    val pick =
        rememberFilePickerLauncher(
            type = FileKitType.Image,
            mode = FileKitMode.Multiple(maxItems = remaining),
            onError = { vm.photoPickFailed() }
        ) { files ->
            if (files != null) vm.addPhotos(files.map { file -> PickedImage { file.readBytes() } })
        }
    val camera =
        rememberCameraPickerLauncher(onError = { vm.photoPickFailed() }) { file ->
            if (file != null) vm.addPhotos(listOf(PickedImage { file.readBytes() }))
        }
    val tools = remember(actions) { WriteTools(actions.rich, actions.colors, { vm.checklistOpen.value }, { checklistLocked = true }) }
    // 하드웨어 키보드의 Ctrl/⌘+S로 저장한다(FR-039).
    val keyboard =
        Modifier.onPreviewKeyEvent { event ->
            val save = event.type == KeyEventType.KeyDown && (event.isCtrlPressed || event.isMetaPressed) && event.key == Key.S
            if (save && state.canSave && !state.saving) vm.save()
            save
        }
    BoxWithConstraints(modifier.then(keyboard).fillMaxSize().background(colors.canvas).systemBarsPadding().imePadding()) {
        val wide = maxWidth >= DiaryWriteSize.twoPaneMin
        Column(Modifier.fillMaxSize()) {
            TopRow(state, onClose = vm::close, onDate = { openSheet(Sheet.DateTime) }, onSave = vm::save)
            if (wide) {
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                        // 에디터가 스스로 스크롤하면 체크 칸 자리가 글과 어긋나서 바깥에서 스크롤한다.
                        Column(
                            Modifier.weight(1f).fillMaxWidth().then(tapToWrite).verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Editor(actions.rich, bodyStyle, editorFocus, Modifier.widthIn(max = DiaryWriteSize.bodyMax).fillMaxWidth())
                        }
                        SaveFailedNotice(state)
                        discardRow()
                        tools.Toolbar(onPhoto = { pick.launch() }, onCamera = { camera.launch() })
                    }
                    Box(Modifier.width(DiarySpacing.hairline).fillMaxHeight().background(colors.line))
                    SidePanel(state, actions, onPick = { pick.launch() }, onPlace = { openSheet(Sheet.Place) })
                }
            } else {
                Column(Modifier.weight(1f).then(tapToWrite).verticalScroll(rememberScrollState())) {
                    Chips(state, onMood = {
                        openSheet(Sheet.Mood)
                    }, onWeather = { openSheet(Sheet.Weather) }, onPlace = { openSheet(Sheet.Place) })
                    if (state.photos.isNotEmpty()) PhotoRow(state, actions, onPick = { pick.launch() })
                    Notices(state, Modifier.padding(horizontal = LocalScreenGutter.current, vertical = DiarySpacing.x2))
                    Editor(actions.rich, bodyStyle, editorFocus, Modifier.fillMaxWidth())
                }
                SaveFailedNotice(state)
                discardRow()
                tools.Toolbar(onPhoto = { pick.launch() }, onCamera = { camera.launch() })
            }
        }
    }
    when (sheet) {
        Sheet.Mood ->
            MoodSheet(
                mood = state.mood,
                onMood = {
                    vm.selectMood(it)
                    sheet = null
                },
                onClear = {
                    state.mood?.let(vm::selectMood)
                    sheet = null
                },
                onDismiss = { sheet = null }
            )

        Sheet.Weather ->
            WeatherSheet(
                weather = state.weather,
                onWeather = {
                    vm.selectWeather(it)
                    sheet = null
                },
                onClear = {
                    state.weather?.let(vm::selectWeather)
                    sheet = null
                },
                onDismiss = { sheet = null }
            )

        Sheet.Place ->
            PlaceSheet(
                initial = state.placeName,
                recent = state.recentPlaces,
                locating = state.locating,
                onLocate = { locationAccess.request(vm::findPlace) },
                onOpenSettings = locationAccess::openSettings,
                onDone = {
                    vm.setPlace(it)
                    vm.stopFindingPlace()
                    sheet = null
                },
                onDismiss = {
                    vm.stopFindingPlace()
                    sheet = null
                }
            )

        Sheet.DateTime ->
            DateTimeSheet(state.date, state.time, onDone = { date: LocalDate, time: LocalTime ->
                vm.setDate(date)
                vm.setTime(time)
                sheet = null
            }, onDismiss = { sheet = null })

        null -> Unit
    }
    state.resume?.let { offer ->
        ResumeSheet(offer, vm::continueDraft, vm::startFresh, onDiscard = { askDiscard = true }, onDismiss = vm::leaveOffer)
    }
    if (askDiscardHere) {
        DiaryDialog(
            title = stringResource(if (state.isEdit) Res.string.write_discard_edit_title else Res.string.write_discard_title),
            message = stringResource(Res.string.write_discard_message),
            confirmLabel = stringResource(Res.string.write_discard_confirm),
            destructive = true,
            onConfirm = {
                askDiscardHere = false
                vm.discard()
            },
            onDismiss = { askDiscardHere = false }
        )
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
private fun DiscardButton(isEdit: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        DiaryButton(
            stringResource(if (isEdit) Res.string.write_discard_edit else Res.string.write_discard),
            onClick,
            kind = DiaryButtonKind.QuietText
        )
    }
}

@Composable
private fun TopRow(state: WriteUiState, onClose: () -> Unit, onDate: () -> Unit, onSave: () -> Unit) {
    val colors = DiaryTheme.colors
    val dates = rememberDiaryDateFormat()
    val interaction = remember { MutableInteractionSource() }
    Layout(
        content = {
            ToolButton(
                DiaryIcons.Close,
                stringResource(Res.string.write_close),
                colors.textPrimary,
                Modifier.padding(start = DiarySpacing.x1),
                onClick = onClose
            )
            Row(
                Modifier
                    // 고치는 일기는 날짜를 읽기 전에 오늘이 비치지 않게 한다.
                    .alpha(if (state.loading && state.isEdit) 0f else 1f)
                    .heightIn(min = DiarySpacing.minTouchTarget)
                    .diaryPress(interaction, colors.textPrimary, DiaryShapes.r12)
                    .clip(DiaryShapes.r12)
                    .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onDate)
                    .padding(horizontal = DiarySpacing.x2),
                horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x1),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicText(
                    dates.dayWithWeekday(state.date),
                    modifier = Modifier.weight(1f, fill = false),
                    style = DiaryTheme.typography.headline.copy(color = colors.textPrimary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                DiaryIcon(DiaryIcons.ChevronDown, contentDescription = null, tint = colors.textSecondary, size = DiaryIconSize.i16)
            }
            // 글자 끝을 상단 바와 같은 화면 여백에 맞춘다.
            // 글자 버튼 안쪽이 16이다.
            DiaryButton(
                stringResource(Res.string.write_save),
                onSave,
                Modifier.padding(end = LocalScreenGutter.current - DiarySpacing.x4),
                kind = DiaryButtonKind.Text,
                enabled = state.canSave && !state.saving
            )
        },
        modifier = Modifier.fillMaxWidth().height(DiarySpacing.x14)
    ) { (close, title, save), constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val start = close.measure(loose)
        val end = save.measure(loose)
        // 날짜는 늘 화면 가운데다.
        // 양옆 가운데 넓은 쪽만큼 비워서 긴 언어나 큰 글자에서도 저장 글자와 겹치지 않고 날짜 글자가 말줄임된다.
        val side = maxOf(start.width, end.width)
        val middle = title.measure(loose.copy(maxWidth = (constraints.maxWidth - side * 2).coerceAtLeast(0)))
        val height = constraints.maxHeight
        layout(constraints.maxWidth, height) {
            start.placeRelative(0, (height - start.height) / 2)
            middle.placeRelative((constraints.maxWidth - middle.width) / 2, (height - middle.height) / 2)
            end.placeRelative(constraints.maxWidth - end.width, (height - end.height) / 2)
        }
    }
}

@Composable
private fun Chips(state: WriteUiState, onMood: () -> Unit, onWeather: () -> Unit, onPlace: () -> Unit) {
    FlowRow(
        Modifier.padding(horizontal = LocalScreenGutter.current),
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
            leadingIcon = weather?.icon() ?: DiaryIcons.WeatherSunny
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
private fun Notices(state: WriteUiState, modifier: Modifier = Modifier) {
    val text =
        when (val notice = state.photoNotice) {
            // 더 넣을 수 있는 길이 있으면 시트로 알린다(Figma P3).
            is PhotoNotice.Limit -> if (notice.canUpgrade) null else stringResource(Res.string.write_photo_limit, notice.limit)

            PhotoNotice.NoSpace -> stringResource(Res.string.write_photo_no_space)

            PhotoNotice.Unreadable -> stringResource(Res.string.write_photo_unreadable)

            null ->
                when {
                    state.addingPhotos -> stringResource(Res.string.write_adding_photos)
                    state.entryChanged -> stringResource(Res.string.write_entry_changed)
                    else -> null
                }
        } ?: return
    DiaryNotice(text, modifier, icon = DiaryIcons.Info)
}

/**
 * 저장 실패는 글이 길어도 보이게 스크롤 밖에 둔다.
 * 안내가 맨 위에 있으면 닫기를 눌러도 아무 일이 없는 것처럼 보인다.
 */
@Composable
private fun SaveFailedNotice(state: WriteUiState) {
    if (!state.saveFailed) return
    DiaryNotice(
        stringResource(Res.string.write_save_failed),
        Modifier.padding(horizontal = LocalScreenGutter.current, vertical = DiarySpacing.x2),
        icon = DiaryIcons.Info
    )
}

/**
 * 체크리스트는 Plus 기능이다(Figma P9).
 * 쓰던 글은 그대로라고 말하고 막지 않는다.
 */
@Composable
private fun ChecklistPlusSheet(onPlus: () -> Unit, onDismiss: () -> Unit) {
    DiaryBottomSheet(
        onDismiss,
        title = stringResource(Res.string.write_checklist_plus_title),
        description = stringResource(Res.string.write_checklist_plus_body)
    ) {
        DiaryButtonPair(
            secondary = stringResource(Res.string.write_limit_later),
            onSecondary = onDismiss,
            primary = stringResource(Res.string.write_limit_plus),
            onPrimary = {
                onDismiss()
                onPlus()
            },
            size = DiaryButtonSize.XLarge,
            secondaryKind = DiaryButtonKind.Weak
        )
    }
}

/**
 * 사진 한도 시트(Figma P3).
 * 고른 사진과 글은 그대로라고 먼저 말하고, 쓰는 일을 막지 않는다.
 */
@Composable
private fun PhotoLimitSheet(limit: Int, onPlus: () -> Unit, onDismiss: () -> Unit) {
    DiaryBottomSheet(
        onDismiss,
        title = stringResource(Res.string.write_limit_title, limit),
        description = stringResource(Res.string.write_limit_body, PhotoLimit.PLUS)
    ) {
        DiaryButtonPair(
            secondary = stringResource(Res.string.write_limit_later),
            onSecondary = onDismiss,
            primary = stringResource(Res.string.write_limit_plus),
            onPrimary = {
                onDismiss()
                onPlus()
            },
            size = DiaryButtonSize.XLarge,
            secondaryKind = DiaryButtonKind.Weak
        )
    }
}

/** 길게 눌러 옆으로 끌면 순서가 바뀐다(FR-007). */
@Composable
private fun PhotoRow(state: WriteUiState, actions: WriteActions, onPick: () -> Unit, inset: Dp? = LocalScreenGutter.current) {
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
    val haptic = LocalHapticFeedback.current
    var shift by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    Box(
        Modifier
            .zIndex(if (dragging) 1f else 0f)
            .graphicsLayer {
                // 끄는 동안 손을 따라오게 그린다.
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
                        onMoved(by)
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
                    .background(colors.overlay),
                contentAlignment = Alignment.Center
            ) {
                DiaryIcon(DiaryIcons.Close, stringResource(Res.string.write_photo_remove), colors.onAccent, size = DiaryIconSize.i12)
            }
        }
    }
}

@Composable
private fun Editor(state: RichTextState, style: TextStyle, focus: FocusRequester, modifier: Modifier) {
    val colors = DiaryTheme.colors
    Box(modifier.padding(horizontal = LocalScreenGutter.current, vertical = DiarySpacing.x3)) {
        if (state.annotatedString.isEmpty()) {
            BasicText(stringResource(Res.string.write_hint), style = style.copy(color = colors.placeholder))
        }
        var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
        val touch = with(LocalDensity.current) { DiarySpacing.minTouchTarget.toPx() }
        LaunchedEffect(state) {
            var before = state.toText()
            snapshotFlow { state.annotatedString.let { state.toText() } }.collect { after ->
                val edit = Checklist.afterEdit(before, after, state.selection.min)
                edit?.let(state::applyEdit)
                before = if (edit == null) after else state.toText()
            }
        }
        val actions =
            checklistActions(state.annotatedString.let { state.toText() }) { item, _ ->
                state.applyEdit(Checklist.toggle(state.toText(), item.start))
            }
        DiaryRichEditor(
            state = state,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = DiaryWriteSize.editorMin)
                    .focusRequester(focus)
                    .semantics { customActions = actions }
                    .checklistMarks({ state.annotatedString.let { state.toText() } }, { layout })
                    .pointerInput(state) {
                        // 표시를 누르면 커서를 옮기지 않고 체크만 바꾼다.
                        // 글자를 누르면 평소처럼 커서가 간다.
                        awaitEachGesture {
                            val down = awaitFirstDown(pass = PointerEventPass.Initial)
                            val start = layout?.checkLineAt(down.position, touch, state.toText(), editing = true) ?: return@awaitEachGesture
                            down.consume()
                            val up = waitForUpOrCancellation(PointerEventPass.Initial) ?: return@awaitEachGesture
                            up.consume()
                            val cursor = state.selection
                            state.applyEdit(Checklist.toggle(state.toText(), start))
                            state.selection = cursor
                        }
                    },
            textStyle = style.copy(color = colors.textPrimary),
            cursorBrush = SolidColor(colors.accent),
            onTextLayout = { layout = it }
        )
    }
}

private fun RichTextState.applyEdit(edit: ChecklistEdit) = when {
    edit.start == edit.end -> addTextAtIndex(edit.start, edit.text)
    edit.text.isEmpty() -> removeTextRange(TextRange(edit.start, edit.end))
    else -> replaceTextRange(TextRange(edit.start, edit.end), edit.text)
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
        BasicText(title, style = DiaryTheme.typography.label.copy(color = DiaryTheme.colors.textSecondary))
        content()
    }
}

/**
 * 도구 줄이 바뀌는 팔레트.
 * 글자색과 형광펜은 쓰임이 달라 각자의 색만 보인다(2026-11-05 사용자 지적).
 */
private enum class PaletteKind { Ink, Mark }

/**
 * 키보드 위 서식 도구(Figma "서식 도구 줄").
 * 글자색·형광펜을 누르면 줄이 그 팔레트로 바뀐다.
 */
private class WriteTools(
    val rich: RichTextState,
    val colors: EditorColors,
    val checklistOpen: () -> Boolean,
    val onChecklistLocked: () -> Unit
) {
    @Composable
    fun Toolbar(onPhoto: (() -> Unit)?, onCamera: (() -> Unit)?) {
        var palette by remember { mutableStateOf<PaletteKind?>(null) }
        val theme = DiaryTheme.colors
        Column(Modifier.fillMaxWidth().background(theme.canvas), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth().height(DiarySpacing.hairline).background(theme.line))
            Row(
                Modifier
                    .widthIn(max = DiaryWriteSize.bodyMax)
                    .fillMaxWidth()
                    .height(DiaryWriteSize.toolbar)
                    .padding(horizontal = DiarySpacing.x1),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x0_5)
            ) {
                val open = palette
                if (open != null) Palette(open) { palette = null } else Tools(onPhoto, onCamera) { palette = it }
            }
        }
    }

    @Composable
    private fun RowScope.Tools(onPhoto: (() -> Unit)?, onCamera: (() -> Unit)?, openPalette: (PaletteKind) -> Unit) {
        val theme = DiaryTheme.colors
        val style = rich.currentSpanStyle
        // 좁은 폰에서는 칸을 고르게 줄여 도구가 모두 보이게 한다.
        val slot = Modifier.weight(1f, fill = false)
        val checked = Checklist.isItemAt(rich.annotatedString.let { rich.toText() }, rich.selection.min)
        fun tint(active: Boolean) = if (active) theme.accentText else theme.textPrimary
        onPhoto?.let { ToolButton(DiaryIcons.Image, stringResource(Res.string.write_add_photo), theme.textPrimary, slot, onClick = it) }
        onCamera?.let { ToolButton(DiaryIcons.Camera, stringResource(Res.string.write_camera), theme.textPrimary, slot, onClick = it) }
        val bold = style.fontWeight == FontWeight.Bold
        ToolButton(DiaryIcons.Bold, stringResource(Res.string.write_format_bold), tint(bold), slot, active = bold) {
            rich.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
        }
        val underline = style.textDecoration?.contains(TextDecoration.Underline) == true
        ToolButton(DiaryIcons.Underline, stringResource(Res.string.write_format_underline), tint(underline), slot, active = underline) {
            rich.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline))
        }
        // 글자색 도구는 지금 고른 색을 그대로 보인다.
        ToolButton(
            DiaryIcons.TextColor,
            stringResource(Res.string.write_format_color),
            style.color.takeOrElse {
                theme.textPrimary
            },
            slot
        ) {
            openPalette(PaletteKind.Ink)
        }
        val marked = style.background.isSpecified
        ToolButton(DiaryIcons.Highlight, stringResource(Res.string.write_format_mark), tint(marked), slot, active = marked) {
            openPalette(PaletteKind.Mark)
        }
        ToolButton(
            DiaryIcons.BulletList,
            stringResource(Res.string.write_format_list),
            tint(rich.isUnorderedList),
            slot,
            active = rich.isUnorderedList
        ) {
            if (checked) toggleChecklist()
            rich.toggleUnorderedList()
        }
        ToolButton(DiaryIcons.Checklist, stringResource(Res.string.write_format_check), tint(checked), slot, active = checked) {
            // 이미 있는 항목을 푸는 것은 언제나 된다.
            // 새로 만드는 것만 Plus다.
            if (!checked && !checklistOpen()) {
                onChecklistLocked()
                return@ToolButton
            }
            if (rich.isUnorderedList) rich.toggleUnorderedList()
            toggleChecklist()
        }
    }

    private fun toggleChecklist() {
        Checklist.toggleLines(rich.toText(), rich.selection.min, rich.selection.max).forEach { rich.applyEdit(it) }
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

/**
 * 고른 색은 액센트 고리로 보인다.
 * 고리와 색 사이를 띄워서 보라 칸에서도 고리가 구별된다.
 */
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
        Box(
            Modifier
                .size(DiaryWriteSize.swatch + DiarySpacing.x2)
                .diaryPress(interaction, theme.textPrimary, ring)
                .then(if (selected) Modifier.border(DiarySpacing.x0_5, theme.accent, ring) else Modifier),
            contentAlignment = Alignment.Center
        ) { chip() }
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
