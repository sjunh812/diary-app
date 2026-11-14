package org.sjhstudio.diary.feature.write

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mohamedrejeb.richeditor.model.RichTextState
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberCameraPickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.source
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.io.buffered
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.component.DiaryBottomSheet
import org.sjhstudio.diary.core.designsystem.component.DiaryButton
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonKind
import org.sjhstudio.diary.core.designsystem.component.DiaryChip
import org.sjhstudio.diary.core.designsystem.component.DiaryDialog
import org.sjhstudio.diary.core.designsystem.component.DiaryIcon
import org.sjhstudio.diary.core.designsystem.component.DiaryLoadingDots
import org.sjhstudio.diary.core.designsystem.component.DiaryNotice
import org.sjhstudio.diary.core.designsystem.component.DiaryToastHost
import org.sjhstudio.diary.core.designsystem.component.DiaryToastResult
import org.sjhstudio.diary.core.designsystem.component.DiaryToastState
import org.sjhstudio.diary.core.designsystem.component.DiaryTopBarTextButton
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
import org.sjhstudio.diary.core.model.ExpectedLeave
import org.sjhstudio.diary.core.model.PickedImage
import org.sjhstudio.diary.core.ui.MoodFace
import org.sjhstudio.diary.core.ui.checkLineAt
import org.sjhstudio.diary.core.ui.checklistActions
import org.sjhstudio.diary.core.ui.checklistMarks
import org.sjhstudio.diary.core.ui.icon
import org.sjhstudio.diary.core.ui.moodName
import org.sjhstudio.diary.core.ui.rememberDiaryDateFormat
import org.sjhstudio.diary.core.ui.rememberReduceMotion
import org.sjhstudio.diary.core.ui.rememberToday
import org.sjhstudio.diary.core.ui.weatherName
import org.sjhstudio.diary.feature.write.resources.Res
import org.sjhstudio.diary.feature.write.resources.write_add_photo
import org.sjhstudio.diary.feature.write.resources.write_adding_photos
import org.sjhstudio.diary.feature.write.resources.write_camera_off_message
import org.sjhstudio.diary.feature.write.resources.write_camera_off_title
import org.sjhstudio.diary.feature.write.resources.write_close
import org.sjhstudio.diary.feature.write.resources.write_discard
import org.sjhstudio.diary.feature.write.resources.write_discard_confirm
import org.sjhstudio.diary.feature.write.resources.write_discard_edit
import org.sjhstudio.diary.feature.write.resources.write_discard_edit_title
import org.sjhstudio.diary.feature.write.resources.write_discard_message
import org.sjhstudio.diary.feature.write.resources.write_discard_title
import org.sjhstudio.diary.feature.write.resources.write_entry_changed
import org.sjhstudio.diary.feature.write.resources.write_hint
import org.sjhstudio.diary.feature.write.resources.write_mood
import org.sjhstudio.diary.feature.write.resources.write_open_failed
import org.sjhstudio.diary.feature.write.resources.write_photo_limit
import org.sjhstudio.diary.feature.write.resources.write_photo_no_space
import org.sjhstudio.diary.feature.write.resources.write_photo_removed
import org.sjhstudio.diary.feature.write.resources.write_photo_undo
import org.sjhstudio.diary.feature.write.resources.write_photo_unreadable
import org.sjhstudio.diary.feature.write.resources.write_place
import org.sjhstudio.diary.feature.write.resources.write_place_settings
import org.sjhstudio.diary.feature.write.resources.write_save
import org.sjhstudio.diary.feature.write.resources.write_save_failed
import org.sjhstudio.diary.feature.write.resources.write_weather

/** 화면 동작 묶음. 화면은 상태만 받아 그림 */
internal class WriteActions(
    val viewModel: WriteViewModel,
    val rich: RichTextState,
    val colors: EditorColors,
    val mediaPath: (String) -> String,
    val onPlus: () -> Unit = {}
)

private enum class Sheet { Mood, Weather, Place, DateTime }

/**
 * 쓰기 화면(Figma "쓰기 (S2)")
 * 위 줄, 첨부 칩, 사진 줄, 본문, 서식 도구 줄 순
 * 폭 840부터는 본문을 가운데 두고 기분·날씨·사진·장소를 옆 칸으로 옮김
 * 사진 고르기는 iOS 때문에 시트가 아닌 이 화면에서 기억함(research R4)
 */
@Composable
internal fun WriteScreen(state: WriteUiState, actions: WriteActions, bodyStyle: TextStyle, modifier: Modifier = Modifier) {
    val vm = actions.viewModel
    val colors = DiaryTheme.colors
    // 화면을 돌리거나 폴더블을 펴도 고르던 시트가 닫히지 않게
    var sheet by rememberSaveable { mutableStateOf<Sheet?>(null) }
    val locationAccess = rememberLocationAccess()
    val cameraAccess = rememberCameraAccess()
    var cameraOff by remember { mutableStateOf(false) }
    // 시트가 뜰 때 키보드는 시트 부품이 내림(DiaryBottomSheet)
    // 키보드가 다 내려가길 기다리지 않고 바로 띄움
    // 쓰던 중에 시트를 열면 닫은 뒤 커서와 키보드를 돌려줌
    // 시트가 뜰 때 초점을 거둬, 돌려주지 않으면 본문을 다시 눌러야 하고 커서도 누른 자리로 옮겨짐
    var editorFocused by rememberSaveable { mutableStateOf(false) }
    // 회전처럼 화면을 다시 만들면 초점이 사라짐. 쓰던 중이었으면 돌려줌
    var refocus by remember { mutableStateOf(editorFocused) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    // 화면을 다시 만들려고 내려가며 잃는 초점은 기억하지 않음
    val onEditorFocus = { focused: Boolean ->
        if (focused || lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) editorFocused = focused
    }
    val openSheet = { target: Sheet ->
        refocus = editorFocused
        sheet = target
    }
    val softKeyboard = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    var askDiscard by remember { mutableStateOf(false) }
    var askDiscardHere by rememberSaveable { mutableStateOf(false) }
    val editorFocus = remember { FocusRequester() }
    // 새 글은 바로 쓸 수 있게 처음 한 번만 커서를 줌
    // 수정은 먼저 읽어 보는 때가 많아 키보드를 띄우지 않음
    var autoFocused by rememberSaveable { mutableStateOf(false) }
    val readyToWrite = !state.loading && state.resume == null && !state.isEdit
    LaunchedEffect(readyToWrite) {
        if (readyToWrite && !autoFocused) {
            autoFocused = true
            editorFocus.requestFocus()
        }
    }
    // 글 아래 빈 곳을 누르면 글 끝에서 이어 씀
    // 글 옆 여백이나 위를 누르면 그 줄에서 가장 가까운 자리에 커서를 둠
    // 어디든 글 끝으로 보내면 줄 앞을 누르려다 맨 아래로 끌려감
    val geometry = remember { EditorGeometry() }
    val tapToWrite =
        Modifier
            .onGloballyPositioned { geometry.viewport = it }
            .pointerInput(actions.rich) {
                detectTapGestures { tap ->
                    actions.rich.selection = TextRange(geometry.offsetAt(tap) ?: actions.rich.annotatedString.length)
                    editorFocus.requestFocus()
                    softKeyboard?.show()
                }
            }
    // 쓰는 동안에는 자리를 차지하지 않게 키보드가 내려갔을 때만 보임
    // 키보드가 움직이는 프레임마다 다시 그리지 않게 보임 여부가 바뀔 때만 읽음
    val imeHidden by remember(imeInsets, density) { derivedStateOf { imeInsets.getBottom(density) == 0 } }
    val discardRow: @Composable () -> Unit = {
        if (state.canDiscard && imeHidden) DiscardButton(state.isEdit) { askDiscardHere = true }
    }
    var checklistLocked by remember { mutableStateOf(false) }
    val photoLimit by vm.photoLimit.collectAsStateWithLifecycle()
    (state.photoNotice as? PhotoNotice.Limit)?.takeIf { it.canUpgrade }?.let { notice ->
        PhotoLimitSheet(notice.limit, actions.onPlus, vm::photoNoticeShown)
    }
    // Plus 화면으로 가면 편집기에 초점을 되돌리지 않음. 되돌리면 Plus 화면 위에 키보드가 떠 첫 뒤로 가기가 키보드 내리기에 쓰임
    if (checklistLocked) {
        ChecklistPlusSheet(
            onPlus = {
                refocus = false
                actions.onPlus()
            },
            onDismiss = { checklistLocked = false }
        )
    }
    val remaining = (photoLimit - state.photos.size).coerceAtLeast(1)
    val pick =
        rememberFilePickerLauncher(
            type = FileKitType.Image,
            mode = FileKitMode.Multiple(maxItems = remaining),
            onError = {
                ExpectedLeave.clear()
                vm.photoPickFailed()
            }
        ) { files ->
            ExpectedLeave.clear()
            if (files != null) vm.addPhotos(files.map { it.picked() })
        }
    val camera =
        rememberCameraPickerLauncher(onError = {
            ExpectedLeave.clear()
            vm.photoPickFailed()
        }) { file ->
            ExpectedLeave.clear()
            if (file != null) vm.addPhotos(listOf(file.picked()))
        }
    val openPicker = {
        ExpectedLeave.mark()
        pick.launch()
    }
    val openCamera = {
        if (cameraAccess.blocked()) {
            cameraOff = true
        } else {
            ExpectedLeave.mark()
            camera.launch()
        }
    }
    val tools = remember(actions) {
        WriteTools(actions.rich, actions.colors, { vm.checklistOpen.value }, {
            refocus = editorFocused
            checklistLocked = true
        })
    }
    LaunchedEffect(sheet, checklistLocked) {
        if (sheet == null && !checklistLocked && refocus) {
            refocus = false
            // 다시 만든 화면은 한 프레임 뒤에 편집기가 붙음
            withFrameNanos {}
            editorFocus.requestFocus()
            softKeyboard?.show()
        }
    }
    // 하드웨어 키보드의 Ctrl/⌘+S로 저장(FR-039)
    val keyboard =
        Modifier.onPreviewKeyEvent { event ->
            val save = event.type == KeyEventType.KeyDown && (event.isCtrlPressed || event.isMetaPressed) && event.key == Key.S
            if (save && state.canSave && !state.saving) vm.save()
            save
        }
    // 사진을 빼면 5초 동안 되돌릴 수 있음(TDS 토스트, 버튼이 있으면 5초)
    val toast = remember { DiaryToastState() }
    val removedMessage = stringResource(Res.string.write_photo_removed)
    val undoLabel = stringResource(Res.string.write_photo_undo)
    LaunchedEffect(vm) {
        vm.photoRemovals.collect { if (toast.show(removedMessage, undoLabel) == DiaryToastResult.ActionPerformed) vm.undoRemovePhoto() }
    }
    BoxWithConstraints(modifier.then(keyboard).fillMaxSize().background(colors.canvas).systemBarsPadding().imePadding()) {
        val wide = maxWidth >= DiaryWriteSize.twoPaneMin
        Column(Modifier.fillMaxSize()) {
            TopRow(state, onClose = vm::close, onDate = { openSheet(Sheet.DateTime) }, onSave = vm::save)
            if (wide) {
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                        // 에디터가 스스로 스크롤하면 체크 칸 자리가 글과 어긋나 바깥에서 스크롤
                        EditorScrollMargin(DiaryWriteSize.editorScrollMargin, Modifier.weight(1f).fillMaxWidth()) {
                            Column(
                                Modifier.fillMaxSize().then(tapToWrite).verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Editor(
                                    actions.rich,
                                    bodyStyle,
                                    editorFocus,
                                    geometry,
                                    Modifier.widthIn(max = DiaryWriteSize.bodyMax).fillMaxWidth().onFocusChanged {
                                        onEditorFocus(it.hasFocus)
                                    }
                                )
                            }
                        }
                        SaveFailedNotice(state)
                        discardRow()
                        tools.Toolbar(onPhoto = { openPicker() }, onCamera = { openCamera() })
                    }
                    Box(Modifier.width(DiarySpacing.hairline).fillMaxHeight().background(colors.line))
                    SidePanel(state, actions, onPick = { openPicker() }, onPlace = { openSheet(Sheet.Place) })
                }
            } else {
                EditorScrollMargin(DiaryWriteSize.editorScrollMargin, Modifier.weight(1f).fillMaxWidth()) {
                    Column(
                        Modifier.fillMaxSize().then(tapToWrite).verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 태블릿 세로 폭에서도 줄이 너무 길어지지 않게
                        Column(Modifier.widthIn(max = DiaryWriteSize.bodyMax).fillMaxWidth()) {
                            Chips(state, onMood = {
                                openSheet(Sheet.Mood)
                            }, onWeather = { openSheet(Sheet.Weather) }, onPlace = { openSheet(Sheet.Place) })
                            if (state.photos.isNotEmpty()) PhotoRow(state, actions, onPick = { openPicker() })
                            PhotoReorderTip(state, Modifier.padding(horizontal = LocalScreenGutter.current, vertical = DiarySpacing.x2))
                            Notices(state, Modifier.padding(horizontal = LocalScreenGutter.current, vertical = DiarySpacing.x2))
                            Editor(
                                actions.rich,
                                bodyStyle,
                                editorFocus,
                                geometry,
                                Modifier.fillMaxWidth().onFocusChanged { onEditorFocus(it.hasFocus) }
                            )
                        }
                    }
                }
                SaveFailedNotice(state)
                discardRow()
                tools.Toolbar(onPhoto = { openPicker() }, onCamera = { openCamera() })
            }
        }
        // 서식 도구 줄 위에 띄움
        DiaryToastHost(
            toast,
            Modifier.align(Alignment.BottomCenter).padding(bottom = DiaryWriteSize.toolbar + DiarySpacing.x3),
            reduceMotion = rememberReduceMotion()
        )
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
    if (cameraOff) {
        DiaryDialog(
            title = stringResource(Res.string.write_camera_off_title),
            message = stringResource(Res.string.write_camera_off_message),
            confirmLabel = stringResource(Res.string.write_place_settings),
            onConfirm = {
                cameraOff = false
                cameraAccess.openSettings()
            },
            onDismiss = { cameraOff = false }
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
    val today = rememberToday()
    val interaction = remember { MutableInteractionSource() }
    Layout(
        content = {
            ToolButton(
                DiaryIcons.Close,
                stringResource(Res.string.write_close),
                colors.textPrimary,
                onClick = onClose,
                modifier = Modifier.padding(start = DiarySpacing.x1)
            )
            Row(
                Modifier
                    // 수정하는 일기는 날짜를 읽기 전에 오늘이 비치지 않게
                    .alpha(if (state.loading && state.isEdit) 0f else 1f)
                    .heightIn(min = DiarySpacing.minTouchTarget)
                    .diaryPress(interaction, colors.textPrimary, DiaryShapes.r12)
                    .clip(DiaryShapes.r12)
                    .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onDate)
                    .padding(horizontal = DiarySpacing.x2),
                horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x1),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 올해가 아니면 연도까지. 요일만 보면 지난해 일기인지 알 수 없음
                BasicText(
                    if (state.date.year == today.year) dates.dayWithWeekday(state.date) else dates.fullDate(state.date),
                    modifier = Modifier.weight(1f, fill = false),
                    style = DiaryTheme.typography.headline.copy(color = colors.textPrimary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                DiaryIcon(DiaryIcons.ChevronDown, contentDescription = null, tint = colors.textSecondary, size = DiaryIconSize.i16)
            }
            // 글자 끝을 상단 바와 같은 화면 여백에 맞춤. 글자 버튼 안쪽이 12.
            // 저장이 0.3초를 넘기면 [저장] 자리에 점을 띄움(사진을 넣는 중이면 다 들어갈 때까지 기다림)
            // 글자만 회색이 되면 눌렸는지 알기 어려움(스켈레톤과 같은 0.3초 지연)
            var slowSave by remember { mutableStateOf(false) }
            LaunchedEffect(state.saving) {
                slowSave = false
                if (state.saving) {
                    delay(SAVE_PROGRESS_DELAY_MS)
                    slowSave = true
                }
            }
            Box(Modifier.padding(end = LocalScreenGutter.current - DiarySpacing.x3), contentAlignment = Alignment.Center) {
                DiaryTopBarTextButton(
                    stringResource(Res.string.write_save),
                    onSave,
                    Modifier.alpha(if (slowSave) 0f else 1f),
                    enabled = state.canSave && !state.saving
                )
                if (slowSave) DiaryLoadingDots(colors.accent)
            }
        },
        modifier = Modifier.fillMaxWidth().height(DiarySpacing.x14)
    ) { (close, title, save), constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val start = close.measure(loose)
        val end = save.measure(loose)
        // 날짜는 늘 화면 가운데
        // 양옆 중 넓은 쪽만큼 비워 긴 언어나 큰 글자에서도 저장 글자와 겹치지 않고 날짜 글자가 말줄임됨
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
            // 더 넣을 수 있는 길이 있으면 시트로 알림(Figma P3)
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
 * 저장 실패와 불러오기 실패는 글이 길어도 보이게 스크롤 밖에 둠
 * 안내가 맨 위에 있으면 닫기를 눌러도 아무 일이 없는 것처럼 보임
 */
@Composable
private fun SaveFailedNotice(state: WriteUiState) {
    val message =
        when {
            state.openFailed -> Res.string.write_open_failed
            state.saveFailed -> Res.string.write_save_failed
            else -> return
        }
    DiaryNotice(
        stringResource(message),
        Modifier.padding(horizontal = LocalScreenGutter.current, vertical = DiarySpacing.x2),
        failure = true
    )
}

@Composable
private fun Editor(state: RichTextState, style: TextStyle, focus: FocusRequester, geometry: EditorGeometry, modifier: Modifier) {
    val colors = DiaryTheme.colors
    Box(modifier.padding(horizontal = LocalScreenGutter.current, vertical = DiarySpacing.x3)) {
        if (state.annotatedString.isEmpty()) {
            BasicText(stringResource(Res.string.write_hint), style = style.copy(color = colors.placeholder))
        }
        var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
        // 서식 없는 본문은 글이 바뀔 때 한 번만 만듦
        // 체크 칸 그리기, 접근성 동작, 변화 감시가 각자 만들면 한 글자마다 문단 트리를 4번 넘게 순회
        val plain = remember(state) { derivedStateOf { state.annotatedString.let { state.toText() } } }
        val touch = with(LocalDensity.current) { DiarySpacing.minTouchTarget.toPx() }
        LaunchedEffect(state) {
            var before = plain.value
            snapshotFlow { plain.value }.collect { after ->
                val edit = Checklist.afterEdit(before, after, state.selection.min)
                edit?.let(state::applyEdit)
                before = if (edit == null) after else state.toText()
            }
        }
        // 선택 핸들을 끄는 동안 움직이는 끝을 가장자리에서 띄워 보이게
        // 편집기는 스스로 스크롤하지 않아 핸들을 화면 끝으로 끌면 도구 줄·키보드 밑으로 숨어 더 끌 수 없음
        // 바깥 스크롤이 [EditorScrollMargin]의 여백만큼 따라 올라와 이어서 끌 수 있음
        val reveal = remember { BringIntoViewRequester() }
        LaunchedEffect(state) {
            var before = state.selection
            snapshotFlow { state.selection }.collectLatest { selection ->
                val moving = movingEdge(before, selection)
                before = selection
                val text = layout ?: return@collectLatest
                if (moving == null || moving > text.layoutInput.text.length) return@collectLatest
                reveal.bringIntoView(text.getCursorRect(moving))
            }
        }
        val actions =
            checklistActions(plain.value) { item, _ ->
                state.applyEdit(Checklist.toggle(state.toText(), item.start))
            }
        DiaryRichEditor(
            state = state,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = DiaryWriteSize.editorMin)
                    .onGloballyPositioned { geometry.field = it }
                    .bringIntoViewRequester(reveal)
                    .focusRequester(focus)
                    .semantics { customActions = actions }
                    .checklistMarks({ plain.value }, { layout })
                    .pointerInput(state) {
                        // 표시를 누르면 커서를 옮기지 않고 체크만 바꿈
                        // 글자를 누르면 평소처럼 커서가 감
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
            onTextLayout = {
                layout = it
                geometry.layout = it
            }
        )
    }
}

internal fun RichTextState.applyEdit(edit: ChecklistEdit) = when {
    edit.start == edit.end -> addTextAtIndex(edit.start, edit.text)
    edit.text.isEmpty() -> removeTextRange(TextRange(edit.start, edit.end))
    else -> replaceTextRange(TextRange(edit.start, edit.end), edit.text)
}

/** 넓은 화면 옆 칸: 기분, 날씨, 사진, 장소(Figma "S2 · 태블릿") */
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
        Section(stringResource(Res.string.write_add_photo)) {
            PhotoRow(state, actions, onPick, inset = null)
            PhotoReorderTip(state)
        }
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

/** 한 번에 읽는 조각 크기 */
private const val PHOTO_CHUNK = 64 * 1024

// 원본을 통째로 메모리에 올리지 않고 조각으로 넘김. 수십 MB 사진 여러 장을 고르면 메모리가 모자람
private fun PlatformFile.picked() = PickedImage { write ->
    source().buffered().use { source ->
        val chunk = ByteArray(PHOTO_CHUNK)
        while (true) {
            val read = source.readAtMostTo(chunk)
            if (read < 0) break
            write(chunk, read)
        }
    }
}
