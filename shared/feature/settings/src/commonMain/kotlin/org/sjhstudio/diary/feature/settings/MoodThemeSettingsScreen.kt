package org.sjhstudio.diary.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.component.DiaryBottomSheet
import org.sjhstudio.diary.core.designsystem.component.DiaryButton
import org.sjhstudio.diary.core.designsystem.component.DiaryIcon
import org.sjhstudio.diary.core.designsystem.component.DiaryPlusChip
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsChoiceRow
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsRow
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsSwitchRow
import org.sjhstudio.diary.core.designsystem.component.DiaryTopBar
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryIconSize
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.MoodGroup
import org.sjhstudio.diary.core.model.MoodLook
import org.sjhstudio.diary.core.model.MoodTheme
import org.sjhstudio.diary.core.ui.LocalMoodLook
import org.sjhstudio.diary.core.ui.MoodFace
import org.sjhstudio.diary.core.ui.ShownMoodLook
import org.sjhstudio.diary.feature.settings.resources.Res
import org.sjhstudio.diary.feature.settings.resources.mood_theme_basic
import org.sjhstudio.diary.feature.settings.resources.mood_theme_bubble
import org.sjhstudio.diary.feature.settings.resources.mood_theme_clay
import org.sjhstudio.diary.feature.settings.resources.mood_theme_clay_custom
import org.sjhstudio.diary.feature.settings.resources.mood_theme_doodle
import org.sjhstudio.diary.feature.settings.resources.mood_theme_dot
import org.sjhstudio.diary.feature.settings.resources.mood_theme_gouache
import org.sjhstudio.diary.feature.settings.resources.mood_theme_line
import org.sjhstudio.diary.feature.settings.resources.mood_theme_preview_body
import org.sjhstudio.diary.feature.settings.resources.mood_theme_shape
import org.sjhstudio.diary.feature.settings.resources.mood_theme_sticker
import org.sjhstudio.diary.feature.settings.resources.mood_theme_veggie
import org.sjhstudio.diary.feature.settings.resources.plus_badge
import org.sjhstudio.diary.feature.settings.resources.settings_mood_theme
import org.sjhstudio.diary.feature.settings.resources.settings_plus_action

/**
 * 설정 > 일기 > 기분 테마(Figma "기분 테마 (G5)", 스펙 013 US3·US4)
 * 줄마다 그 테마로 그린 대표 얼굴 다섯. 무료 사용자에게만 Plus 테마에 Plus 칩
 * 무료에서 Plus 테마를 누르면 결제 화면으로
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
            for (theme in MoodTheme.entries) {
                val locked = theme.plus && canPlus == false
                MoodThemeRow(theme, look, selected = shown == theme, locked = locked) {
                    when {
                        // 요금제를 아직 모르면 기다림. 잠금 표시도 하지 않음
                        theme.plus && canPlus == null -> Unit

                        locked -> preview = theme.id

                        else -> onTheme(theme)
                    }
                }
                if (theme == MoodTheme.CLAY && shown == MoodTheme.CLAY) {
                    DiarySettingsRow(
                        stringResource(Res.string.mood_theme_clay_custom),
                        onClick = onClayLook,
                        // 위 테마 이름 글줄에 맞춤(고름 표시 24 + 사이 12)
                        modifier = Modifier.padding(start = DiaryIconSize.i24 + DiarySpacing.x3)
                    )
                }
                if (theme == MoodTheme.SHAPE && shown == MoodTheme.SHAPE) {
                    DiarySettingsSwitchRow(
                        stringResource(Res.string.mood_theme_sticker),
                        checked = look.sticker,
                        onCheckedChange = onSticker,
                        // 위 테마 이름 글줄에 맞춤(고름 표시 24 + 사이 12)
                        modifier = Modifier.padding(start = DiaryIconSize.i24 + DiarySpacing.x3)
                    )
                }
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
                for (row in MoodGroup.entries.flatMap { it.moods }.chunked(PREVIEW_COLUMNS)) {
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

/** 테마 줄: 고름 표시, 이름(Plus 칩), 오른쪽 대표 얼굴 다섯 */
@Composable
private fun MoodThemeRow(theme: MoodTheme, look: MoodLook, selected: Boolean, locked: Boolean, onClick: () -> Unit) {
    val colors = DiaryTheme.colors
    val typography = DiaryTheme.typography
    val name = stringResource(theme.label())
    DiarySettingsChoiceRow(
        label = name,
        selected = selected,
        onClick = onClick,
        title = {
            Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x3), verticalAlignment = Alignment.CenterVertically) {
                ChoiceMark(selected)
                Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2), verticalAlignment = Alignment.CenterVertically) {
                    BasicText(
                        name,
                        Modifier.weight(1f, fill = false),
                        style = (if (selected) typography.bodyStrong else typography.body).copy(color = colors.textPrimary),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (locked) DiaryPlusChip(stringResource(Res.string.plus_badge))
                }
            }
        },
        trailing = {
            // 그 테마로 그림. 앱 전체 테마와 상관없이 이 줄 안에서만
            CompositionLocalProvider(LocalMoodLook provides ShownMoodLook(theme, look.sticker, look.hair, look.skin, look.glasses)) {
                Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x1)) {
                    for (mood in SAMPLE_MOODS) MoodFace(mood, DiaryWriteSize.moodThemeFace)
                }
            }
        }
    )
}

/** 고름 표시 24. 고른 줄은 채운 체크, 아니면 빈 원 */
@Composable
private fun ChoiceMark(selected: Boolean) {
    val colors = DiaryTheme.colors
    if (selected) {
        Box(Modifier.size(DiaryIconSize.i24).clip(DiaryShapes.full).background(colors.accent), contentAlignment = Alignment.Center) {
            DiaryIcon(DiaryIcons.Check, contentDescription = null, tint = colors.onAccent, size = DiaryIconSize.i16)
        }
    } else {
        Box(Modifier.size(DiaryIconSize.i24).clip(DiaryShapes.full).border(DiarySpacing.x0_5, colors.lineStrong, DiaryShapes.full))
    }
}

private val SAMPLE_MOODS = listOf(Mood.GOOD, Mood.LAUGH, Mood.COOL, Mood.SAD, Mood.ANGRY)

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
