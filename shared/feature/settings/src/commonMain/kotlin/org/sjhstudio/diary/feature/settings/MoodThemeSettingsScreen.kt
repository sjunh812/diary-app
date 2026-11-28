package org.sjhstudio.diary.feature.settings

import androidx.compose.foundation.background
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
import androidx.compose.foundation.selection.selectableGroup
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
import org.sjhstudio.diary.core.designsystem.component.DiaryTag
import org.sjhstudio.diary.core.designsystem.component.DiaryTopBar
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.theme.DiaryIconSize
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.MoodLook
import org.sjhstudio.diary.core.model.MoodTheme
import org.sjhstudio.diary.core.ui.LocalMoodLook
import org.sjhstudio.diary.core.ui.MoodFace
import org.sjhstudio.diary.core.ui.ShownMoodLook
import org.sjhstudio.diary.core.ui.moodArtBleed
import org.sjhstudio.diary.feature.settings.resources.Res
import org.sjhstudio.diary.feature.settings.resources.clay_look_value
import org.sjhstudio.diary.feature.settings.resources.mood_theme_basic
import org.sjhstudio.diary.feature.settings.resources.mood_theme_bubble
import org.sjhstudio.diary.feature.settings.resources.mood_theme_clay
import org.sjhstudio.diary.feature.settings.resources.mood_theme_clay_custom
import org.sjhstudio.diary.feature.settings.resources.mood_theme_clay_tag
import org.sjhstudio.diary.feature.settings.resources.mood_theme_doodle
import org.sjhstudio.diary.feature.settings.resources.mood_theme_dot
import org.sjhstudio.diary.feature.settings.resources.mood_theme_gouache
import org.sjhstudio.diary.feature.settings.resources.mood_theme_line
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
 * 줄마다 고름 표시, 테마 이름, 그 테마로 그린 얼굴 다섯. 무료 사용자에게만 Plus 테마에 Plus 칩
 * 고른 테마에 설정이 있으면 그 줄 바로 아래에 둠. 무료에서 Plus 테마를 누르면 미리보기 시트
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
                .selectableGroup()
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
                if (theme == shown) ThemeOptions(theme, look, onSticker, onClayLook)
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

/** 고른 테마에 딸린 설정. 위 테마 이름 글줄에 맞춰 들여 어느 테마 것인지 읽힘 */
@Composable
private fun ThemeOptions(theme: MoodTheme, look: MoodLook, onSticker: (Boolean) -> Unit, onClayLook: () -> Unit) {
    // 고름 표시 24 + 사이 12
    val indent = Modifier.padding(start = DiaryIconSize.i24 + DiarySpacing.x3)
    when (theme) {
        MoodTheme.SHAPE ->
            DiarySettingsSwitchRow(
                stringResource(Res.string.mood_theme_sticker),
                checked = look.sticker,
                onCheckedChange = onSticker,
                modifier = indent,
                description = stringResource(Res.string.mood_theme_sticker_body)
            )

        MoodTheme.CLAY ->
            DiarySettingsRow(
                stringResource(Res.string.mood_theme_clay_custom),
                onClick = onClayLook,
                modifier = indent,
                value = stringResource(Res.string.clay_look_value, stringResource(look.hair.label()), stringResource(look.skin.label()))
            )

        else -> Unit
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
        // 클레이는 꾸미기도 Plus에서 됨을 짚어 줌
        if (theme == MoodTheme.CLAY) DiaryTag(stringResource(Res.string.mood_theme_clay_tag))
        CompositionLocalProvider(LocalMoodLook provides ShownMoodLook(theme, look.sticker, look.hair, look.skin, look.glasses)) {
            // 그림 기준으로 위 글과 24, 그림끼리 12, 아래 버튼과 32. 시트 안 사이 12와 넘치는 그림 몫을 뺌
            val face = DiaryWriteSize.moodPreviewFace
            val edge = moodArtBleed(face) * 2
            Column(
                Modifier.fillMaxWidth().padding(top = DiarySpacing.x3 + edge / 2, bottom = DiarySpacing.x5 + edge / 2),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(PREVIEW_GAP + edge)
            ) {
                for (row in Mood.ordered.chunked(PREVIEW_COLUMNS)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(PREVIEW_GAP + edge)) {
                        for (mood in row) MoodFace(mood, face)
                    }
                }
            }
        }
        DiaryButton(stringResource(Res.string.settings_plus_action), onPlus, Modifier.fillMaxWidth())
    }
}

private const val PREVIEW_COLUMNS = 5
private val PREVIEW_GAP = DiarySpacing.x3

/** 테마 줄: 고름 표시, 이름(Plus 칩), 오른쪽 그 테마로 그린 얼굴 다섯 */
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
            // 그림끼리 보이는 간격 4(Figma 테마 줄)
            val face = DiaryWriteSize.moodThemeFace
            CompositionLocalProvider(LocalMoodLook provides ShownMoodLook(theme, look.sticker, look.hair, look.skin, look.glasses)) {
                Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x1 + moodArtBleed(face) * 2)) {
                    for (mood in SAMPLE_MOODS) MoodFace(mood, face)
                }
            }
        }
    )
}

/**
 * 고름 표시(TDS Checkbox.Line, SEED Checkbox ghost)
 * 자리는 24로 두어 이름 글줄을 맞추고 체크는 20(SEED 큰 칸 24에 체크 18)
 * 원을 줄마다 두면 아홉 줄에 회색 원이 되풀이돼 고른 줄만 색을 바꿈
 */
@Composable
private fun ChoiceMark(selected: Boolean) {
    val colors = DiaryTheme.colors
    Box(Modifier.size(DiaryIconSize.i24), contentAlignment = Alignment.Center) {
        // 같은 체크에 색만 바꿈(SEED ghost)
        DiaryIcon(
            DiaryIcons.CheckSelected,
            contentDescription = null,
            tint = if (selected) colors.accentText else colors.lineStrong,
            size = DiaryIconSize.i20
        )
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
