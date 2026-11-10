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
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.focus.onFocusChanged
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
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
import org.sjhstudio.diary.core.model.DraftPhoto
import org.sjhstudio.diary.core.model.ExpectedLeave
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
import org.sjhstudio.diary.feature.write.resources.write_open_failed
import org.sjhstudio.diary.feature.write.resources.write_palette_back
import org.sjhstudio.diary.feature.write.resources.write_photo_count
import org.sjhstudio.diary.feature.write.resources.write_photo_limit
import org.sjhstudio.diary.feature.write.resources.write_photo_no_space
import org.sjhstudio.diary.feature.write.resources.write_photo_remove
import org.sjhstudio.diary.feature.write.resources.write_photo_removed
import org.sjhstudio.diary.feature.write.resources.write_photo_undo
import org.sjhstudio.diary.feature.write.resources.write_photo_unreadable
import org.sjhstudio.diary.feature.write.resources.write_place
import org.sjhstudio.diary.feature.write.resources.write_save
import org.sjhstudio.diary.feature.write.resources.write_save_failed
import org.sjhstudio.diary.feature.write.resources.write_weather

// 쓰기 화면의 Plus 안내 시트

/** 체크리스트는 Plus 기능(Figma P9). 쓰던 글은 그대로라고 말하고 막지 않음 */
@Composable
internal fun ChecklistPlusSheet(onPlus: () -> Unit, onDismiss: () -> Unit) {
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
 * 사진 한도 시트(Figma P3)
 * 고른 사진과 글은 그대로라고 먼저 말하고 쓰는 일을 막지 않음
 */
@Composable
internal fun PhotoLimitSheet(limit: Int, onPlus: () -> Unit, onDismiss: () -> Unit) {
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
