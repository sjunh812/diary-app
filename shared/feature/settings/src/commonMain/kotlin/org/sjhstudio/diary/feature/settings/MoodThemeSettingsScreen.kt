package org.sjhstudio.diary.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.component.DiaryBottomSheet
import org.sjhstudio.diary.core.designsystem.component.DiaryButton
import org.sjhstudio.diary.core.designsystem.component.DiaryPlusChip
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsHeader
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsRow
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsSwitchRow
import org.sjhstudio.diary.core.designsystem.component.DiaryTopBar
import org.sjhstudio.diary.core.designsystem.component.diaryPress
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.designsystem.theme.LocalScreenGutter
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.MoodLook
import org.sjhstudio.diary.core.model.MoodTheme
import org.sjhstudio.diary.core.ui.LocalMoodLook
import org.sjhstudio.diary.core.ui.MoodFace
import org.sjhstudio.diary.core.ui.ShownMoodLook
import org.sjhstudio.diary.feature.settings.resources.Res
import org.sjhstudio.diary.feature.settings.resources.clay_look_value
import org.sjhstudio.diary.feature.settings.resources.mood_theme_basic
import org.sjhstudio.diary.feature.settings.resources.mood_theme_bubble
import org.sjhstudio.diary.feature.settings.resources.mood_theme_clay
import org.sjhstudio.diary.feature.settings.resources.mood_theme_clay_custom
import org.sjhstudio.diary.feature.settings.resources.mood_theme_doodle
import org.sjhstudio.diary.feature.settings.resources.mood_theme_dot
import org.sjhstudio.diary.feature.settings.resources.mood_theme_gouache
import org.sjhstudio.diary.feature.settings.resources.mood_theme_line
import org.sjhstudio.diary.feature.settings.resources.mood_theme_options
import org.sjhstudio.diary.feature.settings.resources.mood_theme_preview_body
import org.sjhstudio.diary.feature.settings.resources.mood_theme_shape
import org.sjhstudio.diary.feature.settings.resources.mood_theme_sticker
import org.sjhstudio.diary.feature.settings.resources.mood_theme_sticker_body
import org.sjhstudio.diary.feature.settings.resources.mood_theme_veggie
import org.sjhstudio.diary.feature.settings.resources.plus_badge
import org.sjhstudio.diary.feature.settings.resources.settings_mood_theme
import org.sjhstudio.diary.feature.settings.resources.settings_plus_action

/**
 * 설정 > 일기 > 기분 테마(Figma "기분 테마 (G5)", 스펙 013 US3·US4)
 * 두 칸 카드마다 테마 이름과 그 테마로 그린 얼굴 셋. 무료 사용자에게만 Plus 테마에 Plus 칩
 * 무료에서 Plus 테마를 누르면 미리보기 시트, 시트에서 결제 화면으로
 */
@Composable
fun MoodThemeSettingsScreen(
    look: MoodLook,
    canPlus: Boolean?,
    onTheme: (MoodTheme) -> Unit,
    onSticker: (Boolean) -> Unit,
    onClayLook: () -> Unit,
    onPlus: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DiaryTheme.colors
    val shown = look.shownTheme(canPlus == true)
    // 무료에서 Plus 테마를 누르면 먼저 그 테마로 15가지를 미리 보여 줌. 저장은 하지 않음
    var preview by rememberSaveable { mutableStateOf<String?>(null) }
    Column(modifier.fillMaxSize().background(colors.canvas).systemBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        DiaryTopBar(title = stringResource(Res.string.settings_mood_theme), onBack = onBack)
        Column(
            Modifier
                .widthIn(max = DiaryWriteSize.settingsMax)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = DiarySpacing.x2)
        ) {
            // 두 칸 카드. 고르는 기준인 얼굴을 크게 보이고 고른 카드만 칠함(강조 한 곳)
            BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = LocalScreenGutter.current, vertical = DiarySpacing.x2)) {
                // 좁은 폰에서는 카드 안에 들어가게 얼굴을 줄임(최대 33)
                // 카드 줄 높이를 맞추는 측정 안에서는 잴 수 없어 여기서 정함
                val cardInner = (maxWidth - DiarySpacing.x3) / 2 - DiarySpacing.x4 * 2
                val fit = (cardInner - FACE_GAP * (SAMPLE_MOODS.size - 1)) / SAMPLE_MOODS.size
                val faceSize = minOf(DiaryWriteSize.moodThemeFace, fit)
                Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(DiarySpacing.x3)) {
                    for (pair in MoodTheme.entries.chunked(2)) {
                        Row(
                            Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x3)
                        ) {
                            for (theme in pair) {
                                val locked = theme.plus && canPlus == false
                                MoodThemeCard(
                                    theme,
                                    look,
                                    selected = shown == theme,
                                    locked = locked,
                                    onClick = {
                                        when {
                                            // 요금제를 아직 모르면 기다림. 잠금 표시도 하지 않음
                                            theme.plus && canPlus == null -> Unit

                                            locked -> preview = theme.id

                                            else -> onTheme(theme)
                                        }
                                    },
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    faceSize = faceSize
                                )
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            // 고른 테마에 딸린 설정. 묶음 이름에 테마 이름을 넣어 어느 테마 것인지 바로 읽힘
            when (shown) {
                MoodTheme.SHAPE -> {
                    DiarySettingsHeader(stringResource(Res.string.mood_theme_options, stringResource(shown.label())), gapAbove = true)
                    DiarySettingsSwitchRow(
                        stringResource(Res.string.mood_theme_sticker),
                        checked = look.sticker,
                        onCheckedChange = onSticker,
                        description = stringResource(Res.string.mood_theme_sticker_body)
                    )
                }

                MoodTheme.CLAY -> {
                    DiarySettingsHeader(stringResource(Res.string.mood_theme_options, stringResource(shown.label())), gapAbove = true)
                    DiarySettingsRow(
                        stringResource(Res.string.mood_theme_clay_custom),
                        onClick = onClayLook,
                        value = stringResource(
                            Res.string.clay_look_value,
                            stringResource(look.hair.label()),
                            stringResource(look.skin.label())
                        )
                    )
                }

                else -> Unit
            }
        }
    }
    val previewTheme = MoodTheme.fromId(preview)
    if (previewTheme != null) {
        MoodThemePreviewSheet(previewTheme, look, onPlus = {
            preview = null
            onPlus()
        }, onDismiss = { preview = null })
    }
}

/** Plus 테마 미리보기 시트. 그 테마로 기분 15가지와 [Plus 알아보기] */
@Composable
private fun MoodThemePreviewSheet(theme: MoodTheme, look: MoodLook, onPlus: () -> Unit, onDismiss: () -> Unit) {
    DiaryBottomSheet(
        onDismiss = onDismiss,
        title = stringResource(theme.label()),
        description = stringResource(Res.string.mood_theme_preview_body)
    ) {
        CompositionLocalProvider(LocalMoodLook provides ShownMoodLook(theme, look.sticker, look.hair, look.skin, look.glasses)) {
            Column(
                Modifier.fillMaxWidth().padding(vertical = DiarySpacing.x4),
                verticalArrangement = Arrangement.spacedBy(DiarySpacing.x4),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                for (row in Mood.ordered.chunked(PREVIEW_COLUMNS)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x6)) {
                        for (mood in row) MoodFace(mood, DiaryWriteSize.moodFace)
                    }
                }
            }
        }
        DiaryButton(stringResource(Res.string.settings_plus_action), onPlus, Modifier.fillMaxWidth())
    }
}

private const val PREVIEW_COLUMNS = 5

/**
 * 테마 카드: 위에 이름(무료 사용자에게만 Plus 칩), 아래 가운데 그 테마로 그린 얼굴 셋
 * 고른 카드는 `selected` 면과 `accent` 테두리
 */
@Composable
private fun MoodThemeCard(
    theme: MoodTheme,
    look: MoodLook,
    selected: Boolean,
    locked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    faceSize: Dp = DiaryWriteSize.moodThemeFace
) {
    val colors = DiaryTheme.colors
    val typography = DiaryTheme.typography
    val interaction = remember { MutableInteractionSource() }
    val border = if (selected) Modifier.border(DiarySpacing.x0_5, colors.accent, DiaryShapes.r16) else Modifier
    Column(
        modifier
            .clip(DiaryShapes.r16)
            .background(if (selected) colors.selected else colors.sunken)
            .then(border)
            .diaryPress(interaction, colors.textPrimary, DiaryShapes.r16)
            .selectable(selected, interactionSource = interaction, indication = null, role = Role.RadioButton, onClick = onClick)
            .padding(DiarySpacing.x4),
        // 이웃 카드 이름이 두 줄이어도 얼굴 줄은 바닥에 맞춤
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x1_5), verticalAlignment = Alignment.CenterVertically) {
            BasicText(
                stringResource(theme.label()),
                Modifier.weight(1f, fill = false),
                style = (if (selected) typography.bodyStrong else typography.body).copy(color = colors.textPrimary),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (locked) DiaryPlusChip(stringResource(Res.string.plus_badge))
        }
        // 그 테마로 그림. 앱 전체 테마와 상관없이 이 카드 안에서만
        // 이름과 얼굴 사이 8은 카드 안쪽 16보다 좁아 한 묶음으로 읽힘
        CompositionLocalProvider(LocalMoodLook provides ShownMoodLook(theme, look.sticker, look.hair, look.skin, look.glasses)) {
            Row(
                Modifier.fillMaxWidth().padding(top = DiarySpacing.x2),
                horizontalArrangement = Arrangement.spacedBy(FACE_GAP, Alignment.CenterHorizontally)
            ) {
                for (mood in SAMPLE_MOODS) MoodFace(mood, faceSize)
            }
        }
    }
}

// 그림이 얼굴보다 커서(28/23) 그림 칸 사이 8(Figma)이 얼굴 사이로는 16
private val FACE_GAP = DiarySpacing.x4

// 좋음, 웃음, 쿨. 테마 색이 고르게 보이게 따뜻한 둘과 시원한 하나
private val SAMPLE_MOODS = listOf(Mood.GOOD, Mood.LAUGH, Mood.COOL)

internal fun MoodTheme.label(): StringResource = when (this) {
    MoodTheme.BASIC -> Res.string.mood_theme_basic
    MoodTheme.SHAPE -> Res.string.mood_theme_shape
    MoodTheme.GOUACHE -> Res.string.mood_theme_gouache
    MoodTheme.LINE -> Res.string.mood_theme_line
    MoodTheme.DOT -> Res.string.mood_theme_dot
    MoodTheme.BUBBLE -> Res.string.mood_theme_bubble
    MoodTheme.DOODLE -> Res.string.mood_theme_doodle
    MoodTheme.VEGGIE -> Res.string.mood_theme_veggie
    MoodTheme.CLAY -> Res.string.mood_theme_clay
}
