package org.sjhstudio.diary.core.designsystem.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.component.DiaryButton
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonKind
import org.sjhstudio.diary.core.designsystem.component.DiaryButtonSize
import org.sjhstudio.diary.core.designsystem.component.DiaryChip
import org.sjhstudio.diary.core.designsystem.component.DiaryEmptyState
import org.sjhstudio.diary.core.designsystem.component.DiaryEntryRow
import org.sjhstudio.diary.core.designsystem.component.DiaryHeroBadge
import org.sjhstudio.diary.core.designsystem.component.DiaryMonthHeader
import org.sjhstudio.diary.core.designsystem.component.DiaryNote
import org.sjhstudio.diary.core.designsystem.component.DiaryNoteBox
import org.sjhstudio.diary.core.designsystem.component.DiaryNotice
import org.sjhstudio.diary.core.designsystem.component.DiaryNumberPad
import org.sjhstudio.diary.core.designsystem.component.DiaryPadKey
import org.sjhstudio.diary.core.designsystem.component.DiaryPinDots
import org.sjhstudio.diary.core.designsystem.component.DiaryProgressBar
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsHeader
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsRow
import org.sjhstudio.diary.core.designsystem.component.DiarySettingsSwitchRow
import org.sjhstudio.diary.core.designsystem.component.DiaryStatusBox
import org.sjhstudio.diary.core.designsystem.component.DiaryTimeWheel
import org.sjhstudio.diary.core.designsystem.component.DiaryTopBar
import org.sjhstudio.diary.core.designsystem.component.DiaryValueRow
import org.sjhstudio.diary.core.designsystem.icon.DiaryIcons
import org.sjhstudio.diary.core.designsystem.resources.Res
import org.sjhstudio.diary.core.designsystem.resources.ds_catalog_colors
import org.sjhstudio.diary.core.designsystem.resources.ds_catalog_components
import org.sjhstudio.diary.core.designsystem.resources.ds_catalog_spacing
import org.sjhstudio.diary.core.designsystem.resources.ds_catalog_title
import org.sjhstudio.diary.core.designsystem.resources.ds_catalog_type_sample
import org.sjhstudio.diary.core.designsystem.resources.ds_catalog_typography
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.DiaryWriteSize
import org.sjhstudio.diary.core.designsystem.theme.LocalScreenGutter

/**
 * 디자인 시스템 견본(FR-029).
 * 토큰(색, 글자, 간격, 모서리)과 컴포넌트를 실제 코드로 한 화면에서 본다.
 * 개발 빌드의 설정 > 개발에서만 들어간다.
 * 출시 빌드에는 이 화면으로 가는 길이 없어서 안드로이드는 R8이 코드를 뺀다.
 * 컴포넌트 안의 견본 글은 Figma 화면과 같은 지어낸 글이고 화면 문구가 아니다(문구는 각 기능의 문자열 리소스).
 * 프리뷰(`preview/ComponentPreviews.kt`)가 같은 묶음을 쓴다.
 */
@Composable
fun DesignCatalogScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = DiaryTheme.colors
    Column(modifier.fillMaxSize().background(colors.canvas).systemBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        DiaryTopBar(title = stringResource(Res.string.ds_catalog_title), onBack = onBack)
        Column(
            Modifier
                .widthIn(max = DiaryWriteSize.settingsMax)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = DiarySpacing.x8),
            verticalArrangement = Arrangement.spacedBy(DiarySpacing.x4)
        ) {
            DiarySettingsHeader(stringResource(Res.string.ds_catalog_colors))
            CatalogColors()
            DiarySettingsHeader(stringResource(Res.string.ds_catalog_typography))
            CatalogTypography()
            DiarySettingsHeader(stringResource(Res.string.ds_catalog_spacing))
            CatalogSpacing()
            DiarySettingsHeader(stringResource(Res.string.ds_catalog_components))
            CatalogButtons()
            Column { CatalogSettingsRows() }
            CatalogBoxes()
            CatalogTimeWheel()
            Column { CatalogEntries() }
            CatalogKeypad()
        }
    }
}

/**
 * 색 토큰 전부.
 * 이름은 `DiaryColors`의 필드 이름이고 값은 지금 모드(라이트·다크)의 것이다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CatalogColors() {
    val c = DiaryTheme.colors
    val swatches =
        listOf(
            "canvas" to c.canvas,
            "surfaceCard" to c.surfaceCard,
            "surface" to c.surface,
            "paper" to c.paper,
            "sunken" to c.sunken,
            "selected" to c.selected,
            "line" to c.line,
            "lineStrong" to c.lineStrong,
            "lilac" to c.lilac,
            "accent" to c.accent,
            "accentText" to c.accentText,
            "onAccent" to c.onAccent,
            "textPrimary" to c.textPrimary,
            "textSecondary" to c.textSecondary,
            "textTertiary" to c.textTertiary,
            "placeholder" to c.placeholder,
            "danger" to c.danger,
            "onDanger" to c.onDanger,
            "toast" to c.toast,
            "onToast" to c.onToast,
            "toastAction" to c.toastAction,
            "scrim" to c.scrim,
            "overlay" to c.overlay,
            "shadow" to c.shadow,
            "mood.angry" to c.mood.angry,
            "mood.cool" to c.mood.cool,
            "mood.sad" to c.mood.sad,
            "mood.sick" to c.mood.sick,
            "mood.laugh" to c.mood.laugh,
            "mood.meh" to c.mood.meh,
            "mood.bad" to c.mood.bad,
            "mood.good" to c.mood.good,
            "mood.tired" to c.mood.tired,
            "ink.red" to c.ink.red,
            "ink.orange" to c.ink.orange,
            "ink.green" to c.ink.green,
            "ink.blue" to c.ink.blue,
            "ink.gray" to c.ink.gray,
            "ink.purple" to c.ink.purple,
            "ink.pink" to c.ink.pink,
            "mark.yellow" to c.mark.yellow,
            "mark.green" to c.mark.green,
            "mark.blue" to c.mark.blue,
            "mark.purple" to c.mark.purple,
            "mark.pink" to c.mark.pink
        )
    FlowRow(
        Modifier.padding(horizontal = LocalScreenGutter.current),
        horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2),
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x3)
    ) {
        swatches.forEach { (name, color) -> Swatch(name, color) }
    }
}

@Composable
private fun Swatch(name: String, color: Color) {
    val colors = DiaryTheme.colors
    Column(Modifier.width(SwatchWidth), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(
            Modifier
                .size(DiarySpacing.x10)
                .clip(DiaryShapes.r12)
                .background(color)
                .border(1.dp, colors.line, DiaryShapes.r12)
        )
        Caption(name, colors.textSecondary)
        Caption(color.hex(), colors.textTertiary)
    }
}

/**
 * 글자 토큰 13단계.
 * 이름 뒤에 크기/줄 높이와 굵기를 붙인다.
 */
@Composable
internal fun CatalogTypography() {
    val t = DiaryTheme.typography
    val styles =
        listOf(
            "display" to t.display,
            "title1" to t.title1,
            "title2" to t.title2,
            "numeral" to t.numeral,
            "keypad" to t.keypad,
            "headline" to t.headline,
            "reading" to t.reading,
            "body" to t.body,
            "bodyStrong" to t.bodyStrong,
            "label" to t.label,
            "caption" to t.caption,
            "captionStrong" to t.captionStrong,
            "micro" to t.micro
        )
    val sample = stringResource(Res.string.ds_catalog_type_sample)
    Column(Modifier.padding(horizontal = LocalScreenGutter.current), verticalArrangement = Arrangement.spacedBy(DiarySpacing.x3)) {
        styles.forEach { (name, style) -> TypeSample(name, style, sample) }
    }
}

@Composable
private fun TypeSample(name: String, style: TextStyle, sample: String) {
    val colors = DiaryTheme.colors
    Column {
        BasicText(sample, style = style.copy(color = colors.textPrimary), maxLines = 1)
        val spec = "$name · ${style.fontSize.value.toInt()}/${style.lineHeight.value.toInt()} ${style.fontWeight?.weight ?: ""}"
        BasicText(spec, style = DiaryTheme.typography.caption.copy(color = colors.textTertiary))
    }
}

/**
 * 간격 단계와 모서리 단계.
 * 모서리 견본은 56이라 r28과 full이 다르게 보인다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CatalogSpacing() {
    val colors = DiaryTheme.colors
    val spacing =
        listOf(
            "x1" to DiarySpacing.x1,
            "x2" to DiarySpacing.x2,
            "x3" to DiarySpacing.x3,
            "x4" to DiarySpacing.x4,
            "x5" to DiarySpacing.x5,
            "x6" to DiarySpacing.x6,
            "x8" to DiarySpacing.x8,
            "x10" to DiarySpacing.x10,
            "x14" to DiarySpacing.x14
        )
    val radius =
        listOf(
            "r8" to DiaryShapes.r8,
            "r10" to DiaryShapes.r10,
            "r12" to DiaryShapes.r12,
            "r14" to DiaryShapes.r14,
            "r16" to DiaryShapes.r16,
            "r20" to DiaryShapes.r20,
            "r28" to DiaryShapes.r28,
            "full" to DiaryShapes.full
        )
    Column(Modifier.padding(horizontal = LocalScreenGutter.current), verticalArrangement = Arrangement.spacedBy(DiarySpacing.x4)) {
        Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2), verticalAlignment = Alignment.Bottom) {
            spacing.forEach { (name, value) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.width(DiarySpacing.x4).height(value).background(colors.accent))
                    Caption(name, colors.textSecondary)
                    Caption(value.value.toInt().toString(), colors.textTertiary)
                }
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2),
            verticalArrangement = Arrangement.spacedBy(DiarySpacing.x2)
        ) {
            radius.forEach { (name, shape) -> RadiusSample(name, shape) }
        }
    }
}

@Composable
private fun RadiusSample(name: String, shape: Shape) {
    val colors = DiaryTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.size(DiarySpacing.x14).clip(shape).background(colors.sunken).border(1.dp, colors.lineStrong, shape))
        Caption(name, colors.textSecondary)
    }
}

@Composable
internal fun CatalogButtons() {
    Column(Modifier.padding(horizontal = LocalScreenGutter.current), verticalArrangement = Arrangement.spacedBy(DiarySpacing.x3)) {
        DiaryButton("시작하기", {}, size = DiaryButtonSize.XLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2)) {
            DiaryButton("첨부 없이 문의", {}, Modifier.weight(1f), kind = DiaryButtonKind.Secondary)
            DiaryButton("첨부해서 문의", {}, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2)) {
            DiaryButton("오늘 일기 쓰기", {}, size = DiaryButtonSize.Medium, kind = DiaryButtonKind.Weak)
            DiaryButton("백업", {}, size = DiaryButtonSize.Small, kind = DiaryButtonKind.Weak)
            DiaryButton("지우기", {}, size = DiaryButtonSize.Medium, kind = DiaryButtonKind.Danger)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2)) {
            DiaryChip("좋음", selected = true, onClick = {}, leadingIcon = DiaryIcons.StarFilled)
            DiaryChip("기분", selected = false, onClick = {})
        }
    }
}

/**
 * 설정 줄 묶음.
 * 세로로 쌓는 Column 안에서 부른다.
 */
@Composable
internal fun CatalogSettingsRows() {
    DiaryTopBar(title = "설정", onBack = {})
    DiarySettingsHeader("백업")
    DiarySettingsRow("백업과 복원", {}, value = "오늘 오후 9:30")
    DiarySettingsRow("원본 파일 내보내기", {}, leadingIcon = DiaryIcons.Download)
    DiarySettingsSwitchRow("자동 백업", checked = true, onCheckedChange = {}, description = "하루 한 번, 바뀐 것만 올려요")
    DiaryValueRow("옮긴 일기", "1,284편")
    DiaryValueRow("찾을 수 없는 사진", "33장")
}

@Composable
internal fun CatalogBoxes() {
    Column(Modifier.padding(horizontal = LocalScreenGutter.current), verticalArrangement = Arrangement.spacedBy(DiarySpacing.x4)) {
        DiaryHeroBadge(DiaryIcons.CircleCheck)
        DiaryNoteBox(
            listOf(
                DiaryNote(DiaryIcons.ImageOff, "찾을 수 없는 사진 33장", "예전 앱이 임시 공간에 두었다가 지워진 사진이에요. 일기 안에 자리를 남겨 뒀어요."),
                DiaryNote(DiaryIcons.CalendarCheck, "날짜를 바로잡은 일기 2편", "쓸 때 고른 날짜로 고쳤어요.")
            )
        )
        DiaryNotice("예전 앱 파일 일부가 깨져 일기 12편을 읽지 못했어요. 원래 기록은 휴대폰 안에 그대로 있어요.", icon = DiaryIcons.ShieldCheck)
        DiaryProgressBar(0.62f)
    }
}

/** 상태 상자와 시각 고르기 휠(오후 10:00). */
@Composable
internal fun CatalogTimeWheel() {
    Column(Modifier.padding(horizontal = LocalScreenGutter.current), verticalArrangement = Arrangement.spacedBy(DiarySpacing.x4)) {
        DiaryStatusBox(
            DiaryIcons.Bell,
            "알림이 꺼져 있어요",
            "휴대폰 설정에서 1일 1일기 알림을 허용해야 매일 알림이 와요.",
            actionLabel = "허용하기",
            onAction = {}
        )
        DiaryTimeWheel(minuteOfDay = 22 * 60, onChange = {}, is24Hour = false)
        DiaryTimeWheel(minuteOfDay = 22 * 60, onChange = {}, is24Hour = true)
    }
}

/**
 * 일기 줄과 빈 상태.
 * 세로로 쌓는 Column 안에서 부른다.
 */
@Composable
internal fun CatalogEntries() {
    DiaryEntryRow(
        weekday = "토",
        day = "31",
        preview = "퇴근길에 한강까지 걸었다. 바람이 차가워졌는데 그래서 더 좋았다.",
        favorite = true,
        dividerAbove = false,
        meta = { CatalogMeta("오후 9:40") }
    )
    DiaryEntryRow(weekday = null, day = null, preview = "아침 회의가 길었다.", meta = { CatalogMeta("오전 8:10") })
    DiaryMonthHeader("9월")
    DiaryEmptyState(DiaryIcons.BookOpen, "아직 쓴 일기가 없어요", description = "오늘 있었던 일 한 줄이면 충분해요", actionLabel = "오늘 일기 쓰기", onAction = {})
}

@Composable
private fun CatalogMeta(text: String) {
    BasicText(text, style = DiaryTheme.typography.captionStrong.copy(color = DiaryTheme.colors.textSecondary), maxLines = 1)
}

@Composable
internal fun CatalogKeypad() {
    Column(
        Modifier.padding(horizontal = LocalScreenGutter.current).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DiarySpacing.x6)
    ) {
        DiaryPinDots(filled = 2)
        DiaryPinDots(filled = 4, error = true)
        DiaryNumberPad(onDigit = {}, onDelete = {}, bottomStart = DiaryPadKey(DiaryIcons.Fingerprint, "생체 인증으로 열기") {})
        DiaryNumberPad(onDigit = {}, onDelete = {}, enabled = false)
    }
}

@Composable
private fun Caption(text: String, color: Color) {
    BasicText(
        text,
        style = DiaryTheme.typography.micro.copy(color = color, textAlign = TextAlign.Center),
        maxLines = 2,
        modifier = Modifier.padding(top = DiarySpacing.x1)
    )
}

/** Figma 변수와 견주기 쉽게 #AARRGGBB로. */
private fun Color.hex(): String = "#" + toArgb().toUInt().toString(16).uppercase().padStart(8, '0')

private val SwatchWidth: Dp = DiarySpacing.x14 + DiarySpacing.x6
