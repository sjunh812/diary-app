package org.sjhstudio.diary.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsHeader
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsSwitchRow
import org.sjhstudio.diary.core.designsystem.component.DiaryTopBar
import org.sjhstudio.diary.core.designsystem.component.diaryPress
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.designsystem.theme.LocalScreenGutter
import org.sjhstudio.diary.core.model.ClayHair
import org.sjhstudio.diary.core.model.ClaySkin
import org.sjhstudio.diary.core.model.Mood
import org.sjhstudio.diary.core.model.MoodLook
import org.sjhstudio.diary.core.model.MoodTheme
import org.sjhstudio.diary.core.ui.FaceLabel
import org.sjhstudio.diary.core.ui.LocalMoodLook
import org.sjhstudio.diary.core.ui.MoodFace
import org.sjhstudio.diary.core.ui.ShownMoodLook
import org.sjhstudio.diary.core.ui.tone
import org.sjhstudio.diary.feature.settings.resources.Res
import org.sjhstudio.diary.feature.settings.resources.clay_glasses
import org.sjhstudio.diary.feature.settings.resources.clay_hair_bob
import org.sjhstudio.diary.feature.settings.resources.clay_hair_bun
import org.sjhstudio.diary.feature.settings.resources.clay_hair_buzz
import org.sjhstudio.diary.feature.settings.resources.clay_hair_curly
import org.sjhstudio.diary.feature.settings.resources.clay_hair_dandy
import org.sjhstudio.diary.feature.settings.resources.clay_hair_long
import org.sjhstudio.diary.feature.settings.resources.clay_hair_part
import org.sjhstudio.diary.feature.settings.resources.clay_hair_ponytail
import org.sjhstudio.diary.feature.settings.resources.clay_hair_short
import org.sjhstudio.diary.feature.settings.resources.clay_hair_title
import org.sjhstudio.diary.feature.settings.resources.clay_preview_note
import org.sjhstudio.diary.feature.settings.resources.clay_skin_1
import org.sjhstudio.diary.feature.settings.resources.clay_skin_2
import org.sjhstudio.diary.feature.settings.resources.clay_skin_3
import org.sjhstudio.diary.feature.settings.resources.clay_skin_4
import org.sjhstudio.diary.feature.settings.resources.clay_skin_5
import org.sjhstudio.diary.feature.settings.resources.clay_skin_6
import org.sjhstudio.diary.feature.settings.resources.clay_skin_title
import org.sjhstudio.diary.feature.settings.resources.mood_theme_clay_custom

/**
 * 기분 테마 > 클레이 얼굴 꾸미기(Figma 프로토타입 P2, `docs/design.md` "클레이 꾸미기 고르기")
 * 위에 기분 다섯 미리보기, 머리 스타일 9, 피부톤 6, 안경. 고르면 그 자리에서 바뀌고 바로 저장
 * 머리색은 늘 기분 색이라 고르는 칸이 없음
 */
@Composable
fun ClayLookSettingsScreen(
    look: MoodLook,
    onHair: (ClayHair) -> Unit,
    onSkin: (ClaySkin) -> Unit,
    onGlasses: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DiaryTheme.colors
    val shown = ShownMoodLook(MoodTheme.CLAY, look.sticker, look.hair, look.skin, look.glasses)
    Column(modifier.fillMaxSize().background(colors.canvas).systemBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        DiaryTopBar(title = stringResource(Res.string.mood_theme_clay_custom), onBack = onBack)
        CompositionLocalProvider(LocalMoodLook provides shown) {
            Column(
                Modifier.widthIn(max = DiaryWriteSize.settingsMax).fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 그림이 얼굴보다 커서(28/23) 그림 칸 사이 10(Figma)이 얼굴 사이로는 20
                // 360dp 폰처럼 좁으면 화면 여백 안에 들어가게 얼굴을 줄임
                BoxWithConstraints(
                    Modifier.fillMaxWidth().padding(
                        top = DiarySpacing.x4,
                        start = LocalScreenGutter.current,
                        end = LocalScreenGutter.current
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    val fit = (maxWidth - DiarySpacing.x5 * (SAMPLE_MOODS.size - 1)) / SAMPLE_MOODS.size
                    val face = minOf(DiaryWriteSize.clayPreviewFace, fit)
                    Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x5)) {
                        for (mood in SAMPLE_MOODS) MoodFace(mood, face)
                    }
                }
                BasicText(
                    stringResource(Res.string.clay_preview_note),
                    Modifier.padding(top = DiarySpacing.x4, start = LocalScreenGutter.current, end = LocalScreenGutter.current),
                    style = DiaryTheme.typography.caption.copy(color = colors.textSecondary, textAlign = TextAlign.Center)
                )
                DiarySettingsHeader(stringResource(Res.string.clay_hair_title), gapAbove = true)
                HairGrid(look, onHair)
                DiarySettingsHeader(stringResource(Res.string.clay_skin_title), gapAbove = true)
                SkinRow(look.skin, onSkin)
                Spacer(Modifier.size(DiarySpacing.x4))
                DiarySettingsSwitchRow(stringResource(Res.string.clay_glasses), checked = look.glasses, onCheckedChange = onGlasses)
            }
        }
    }
}

/** 머리 스타일 칸. 화면 폭을 다섯으로 나누고 얼굴 48과 이름. 고른 칸은 `selected` 면(기분 칸과 같음) */
@Composable
private fun HairGrid(look: MoodLook, onHair: (ClayHair) -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = LocalScreenGutter.current).selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x1)
    ) {
        for (row in ClayHair.entries.chunked(HAIR_COLUMNS)) {
            // 이름이 두 줄인 칸이 있어도 한 줄의 고른 면 높이를 맞춤
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                for (hair in row) {
                    HairCell(
                        hair,
                        selected = look.hair == hair,
                        look = look,
                        onClick = { onHair(hair) },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
                repeat(HAIR_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun HairCell(hair: ClayHair, selected: Boolean, look: MoodLook, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = DiaryTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier
            .padding(horizontal = DiarySpacing.x0_5)
            .diaryPress(interaction, colors.textPrimary, DiaryShapes.r16)
            .clip(DiaryShapes.r16)
            .then(if (selected) Modifier.background(colors.selected) else Modifier)
            .selectable(selected, interactionSource = interaction, indication = null, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = DiarySpacing.x2),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x1_5)
    ) {
        CompositionLocalProvider(
            LocalMoodLook provides ShownMoodLook(MoodTheme.CLAY, hair = hair, skin = look.skin, glasses = look.glasses)
        ) {
            MoodFace(Mood.GOOD, DiaryWriteSize.clayHairFace)
        }
        FaceLabel(stringResource(hair.label()), selected, color = if (selected) colors.accentText else colors.textSecondary)
    }
}

/** 피부톤: 지름 36 원을 누르는 칸 48 안에, 사이 8, 고른 원은 `accent` 고리 2 */
@Composable
private fun SkinRow(selected: ClaySkin, onSkin: (ClaySkin) -> Unit) {
    val colors = DiaryTheme.colors
    val skins = ClaySkin.entries
    // 누르는 칸 48은 늘 지킴. 보이는 원(36)이 칸 안쪽에 있어 양끝 칸은 화면 여백 쪽으로 6 내보냄
    // 그래서 원 가장자리는 화면 여백에 맞고, 320dp 폰(가용 292)에도 48 여섯 칸(288)이 한 줄에 들어감
    val inset = (DiaryWriteSize.claySkinTouch - DiaryWriteSize.claySkinDot) / 2
    val widest = DiaryWriteSize.claySkinTouch * skins.size + DiarySpacing.x2 * (skins.size - 1)
    // 화면 분할처럼 창이 더 좁으면 칸을 줄이지 않고 다음 줄로 넘김
    FlowRow(
        Modifier
            .padding(horizontal = LocalScreenGutter.current - inset)
            .widthIn(max = widest)
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x2)
    ) {
        for (skin in skins) {
            val name = stringResource(skin.label())
            val ring = if (selected == skin) Modifier.border(DiarySpacing.x0_5, colors.accent, DiaryShapes.full) else Modifier
            Box(
                Modifier
                    .size(DiaryWriteSize.claySkinTouch)
                    .clip(DiaryShapes.full)
                    .selectable(selected == skin, role = Role.RadioButton) { onSkin(skin) }
                    .semantics { contentDescription = name }
                    .then(ring),
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.size(DiaryWriteSize.claySkinDot).clip(DiaryShapes.full).background(skin.tone.face))
            }
        }
    }
}

private const val HAIR_COLUMNS = 5

internal fun ClayHair.label(): StringResource = when (this) {
    ClayHair.BOB -> Res.string.clay_hair_bob
    ClayHair.LONG -> Res.string.clay_hair_long
    ClayHair.SHORT -> Res.string.clay_hair_short
    ClayHair.DANDY -> Res.string.clay_hair_dandy
    ClayHair.PART -> Res.string.clay_hair_part
    ClayHair.BUN -> Res.string.clay_hair_bun
    ClayHair.PONYTAIL -> Res.string.clay_hair_ponytail
    ClayHair.CURLY -> Res.string.clay_hair_curly
    ClayHair.BUZZ -> Res.string.clay_hair_buzz
}

internal fun ClaySkin.label(): StringResource = when (this) {
    ClaySkin.SKIN1 -> Res.string.clay_skin_1
    ClaySkin.SKIN2 -> Res.string.clay_skin_2
    ClaySkin.SKIN3 -> Res.string.clay_skin_3
    ClaySkin.SKIN4 -> Res.string.clay_skin_4
    ClaySkin.SKIN5 -> Res.string.clay_skin_5
    ClaySkin.SKIN6 -> Res.string.clay_skin_6
}
