package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme

/**
 * 색 고르기(`docs/design.md` 테마 화면 직접 고르기)
 * 위에서부터 색 판(채도·밝기) 120, 색상 띠 28, 색 코드 입력칸
 * 고를 때마다 [onChange]로 0xRRGGBB를 알림. 색 코드를 덜 넣는 동안은 null
 * 판과 띠는 스크린 리더에서 값을 올리고 내림
 * 정확히 고르는 길은 색 코드
 * 색상은 따로 들고 있어 회색 쪽으로 끌어도 색상을 잃지 않음
 */
@Composable
fun DiaryColorPicker(
    initial: Int,
    onChange: (Int?) -> Unit,
    padLabel: String,
    hueLabel: String,
    hexLabel: String,
    modifier: Modifier = Modifier
) {
    val start = remember(initial) { rgbToHsv(initial) }
    var hue by rememberSaveable(initial) { mutableFloatStateOf(start[0]) }
    var saturation by rememberSaveable(initial) { mutableFloatStateOf(start[1]) }
    var value by rememberSaveable(initial) { mutableFloatStateOf(start[2]) }
    var hex by rememberSaveable(initial) { mutableStateOf(formatHex(initial)) }
    fun picked(h: Float, s: Float, v: Float) {
        hue = h
        saturation = s
        value = v
        val rgb = hsvToRgb(h, s, v)
        hex = formatHex(rgb)
        onChange(rgb)
    }
    val colors = DiaryTheme.colors
    Column(modifier.fillMaxWidth()) {
        SaturationPad(hue, saturation, value, padLabel, onPick = { s, v -> picked(hue, s, v) })
        // 누르는 곳은 48이고 그 남는 위아래만큼 줄여 띠와 판·칸 사이는 그대로 12
        Box(Modifier.padding(top = DiarySpacing.x3 - BAR_SLACK)) {
            HueBar(hue, hueLabel, onPick = { picked(it, saturation, value) })
        }
        BasicTextField(
            hex,
            onValueChange = { text ->
                // '#'은 칸 앞에 고정으로 두고 입력은 16진수 여섯 자만
                val cleaned = text.uppercase().filter { it in '0'..'9' || it in 'A'..'F' }.take(HEX_DIGITS)
                hex = cleaned
                val rgb = parseHex(cleaned)
                if (rgb != null) {
                    val (h, s, v) = rgbToHsv(rgb).let { Triple(it[0], it[1], it[2]) }
                    // 회색이면 색상이 정해지지 않아 지금 색상을 둠
                    hue = if (s == 0f) hue else h
                    saturation = s
                    value = v
                }
                // 덜 넣은 색 코드는 고른 색이 없음. 칸에 보이는 값과 다른 앞 색을 쓰지 않게
                onChange(rgb)
            },
            singleLine = true,
            textStyle = DiaryTheme.typography.reading.copy(color = colors.textPrimary),
            cursorBrush = SolidColor(colors.accentIcon),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, keyboardType = KeyboardType.Ascii),
            modifier =
                Modifier
                    .padding(top = DiarySpacing.x3 - BAR_SLACK)
                    .fillMaxWidth()
                    .clip(DiaryShapes.r12)
                    .background(colors.sunken)
                    .semantics { contentDescription = hexLabel },
            // 고른 색은 판을 덮지 않는 이 칸 앞에도 늘 보임
            decorationBox = { field ->
                Row(Modifier.height(FIELD_HEIGHT).padding(horizontal = DiarySpacing.x4), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(PREVIEW)
                            .background(Color(hsvToRgb(hue, saturation, value).toLong() or OPAQUE), DiaryShapes.full)
                    )
                    Spacer(Modifier.width(DiarySpacing.x3))
                    BasicText("#", style = DiaryTheme.typography.reading.copy(color = colors.textSecondary))
                    Spacer(Modifier.width(DiarySpacing.x0_5))
                    Box(Modifier.weight(1f)) { field() }
                }
            }
        )
    }
}

/**
 * 채도(가로)와 밝기(세로). 스크린 리더에서는 채도를 조절
 * 끄는 동안 손가락에 가리지 않게 손잡이 위에 고른 색을 크게 띄움(iOS 색 고르기 돋보기와 같은 방식)
 */
@Composable
private fun SaturationPad(hue: Float, saturation: Float, value: Float, label: String, onPick: (Float, Float) -> Unit) {
    // 손짓 감지는 한 번 걸어 두므로 늘 마지막 콜백을 부름
    val pickLatest by rememberUpdatedState(onPick)
    val pure = Color(hsvToRgb(hue, 1f, 1f).toLong() or OPAQUE)
    val current = Color(hsvToRgb(hue, saturation, value).toLong() or OPAQUE)
    var dragging by remember { mutableStateOf(false) }
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(PAD_HEIGHT)
            .semantics {
                contentDescription = label
                stateDescription = "${(saturation * PERCENT).roundToInt()}%, ${(value * PERCENT).roundToInt()}%"
                progressBarRangeInfo = ProgressBarRangeInfo(saturation, 0f..1f, steps = STEPS)
                setProgress { target ->
                    onPick(target.coerceIn(0f, 1f), value)
                    true
                }
            }.pointerInput(Unit) {
                fun pick(at: Offset) = pickLatest((at.x / size.width).coerceIn(0f, 1f), 1f - (at.y / size.height).coerceIn(0f, 1f))
                detectTapGestures { pick(it) }
            }.pointerInput(Unit) {
                fun pick(at: Offset) = pickLatest((at.x / size.width).coerceIn(0f, 1f), 1f - (at.y / size.height).coerceIn(0f, 1f))
                detectDragGestures(
                    onDragStart = { at ->
                        dragging = true
                        pick(at)
                    },
                    onDragEnd = { dragging = false },
                    onDragCancel = { dragging = false }
                ) { change, _ -> pick(change.position) }
            }
    ) {
        // 돋보기가 판 밖으로 나가야 해서 판만 둥글게 잘라 그림
        clipPath(Path().apply { addRoundRect(RoundRect(Rect(Offset.Zero, size), CornerRadius(PAD_RADIUS.toPx()))) }) {
            drawRect(pure)
            drawRect(Brush.horizontalGradient(listOf(Color.White, Color.White.copy(alpha = 0f))))
            drawRect(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0f), Color.Black)))
        }
        val at = Offset(saturation * size.width, (1f - value) * size.height)
        knob(at, current)
        if (dragging) loupe(at, current, sideWhenTop = true)
    }
}

/** 색상 0~360. 띠는 28, 누르는 곳은 위아래로 넓혀 48 */
@Composable
private fun HueBar(hue: Float, label: String, onPick: (Float) -> Unit) {
    val current = Color(hsvToRgb(hue, 1f, 1f).toLong() or OPAQUE)
    var dragging by remember { mutableStateOf(false) }
    val pickLatest by rememberUpdatedState(onPick)
    val inset = with(LocalDensity.current) { BAR_HEIGHT.toPx() / 2 }
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(BAR_HEIGHT + BAR_SLACK * 2)
            .semantics {
                contentDescription = label
                stateDescription = "${hue.roundToInt()}°"
                progressBarRangeInfo = ProgressBarRangeInfo(hue, 0f..FULL_TURN, steps = STEPS)
                setProgress { target ->
                    onPick(target.coerceIn(0f, FULL_TURN))
                    true
                }
            }.pointerInput(inset) {
                detectTapGestures { pickLatest(hueAt(it.x, size.width.toFloat(), inset)) }
            }.pointerInput(inset) {
                detectDragGestures(
                    onDragStart = {
                        dragging = true
                        pickLatest(hueAt(it.x, size.width.toFloat(), inset))
                    },
                    onDragEnd = { dragging = false },
                    onDragCancel = { dragging = false }
                ) { change, _ -> pickLatest(hueAt(change.position.x, size.width.toFloat(), inset)) }
            }
    ) {
        // 손잡이가 띠 밖으로 나가지 않게 양끝 반지름 안쪽이 0~360. 띠 색도 같은 범위에 맞춤
        val r = inset
        val middle = size.height / 2
        drawRoundRect(
            Brush.horizontalGradient(HUES.map { Color(hsvToRgb(it, 1f, 1f).toLong() or OPAQUE) }, startX = r, endX = size.width - r),
            topLeft = Offset(0f, middle - r),
            size = Size(size.width, r * 2),
            cornerRadius = CornerRadius(r)
        )
        val at = Offset(r + hue / FULL_TURN * (size.width - r * 2), middle)
        knob(at, current)
        if (dragging) loupe(at, current)
    }
}

/**
 * 손가락 위에 띄우는 고른 색 원. 흰 테두리 밖 가는 선으로 판과 떨어져 보이게(그림자 원은 딤처럼 보여 쓰지 않음)
 * 위에 자리가 없으면(판 맨 위) 손가락 옆, 화면 가운데 쪽에 띄움
 */
private fun DrawScope.loupe(at: Offset, fill: Color, sideWhenTop: Boolean = false) {
    val r = LOUPE.toPx() / 2
    val lift = LOUPE_LIFT.toPx()
    val center =
        if (sideWhenTop && at.y < lift) {
            Offset(if (at.x > size.width / 2) at.x - lift else at.x + lift, at.y.coerceAtLeast(r))
        } else {
            Offset(at.x.coerceIn(r, size.width - r), at.y - lift)
        }
    drawCircle(Color.Black.copy(alpha = LOUPE_OUTLINE), radius = r + LOUPE_OUTLINE_WIDTH.toPx(), center = center)
    drawCircle(Color.White, radius = r, center = center)
    drawCircle(fill, radius = r - KNOB_BORDER.toPx(), center = center)
}

/** 띠에서 [x] 자리의 색상. 양끝 [inset] 안쪽이 0~360 */
private fun hueAt(x: Float, width: Float, inset: Float): Float = ((x - inset) / (width - inset * 2)).coerceIn(0f, 1f) * FULL_TURN

/** 흰 테두리 3의 손잡이 28 */
private fun DrawScope.knob(center: Offset, fill: Color) {
    val r = KNOB.toPx() / 2
    drawCircle(fill, radius = r, center = center)
    drawCircle(Color.White, radius = r - KNOB_BORDER.toPx() / 2, center = center, style = Stroke(KNOB_BORDER.toPx()))
}

internal fun hsvToRgb(h: Float, s: Float, v: Float): Int = Color.hsv(h.coerceIn(0f, FULL_TURN), s, v).toArgb() and RGB

internal fun rgbToHsv(rgb: Int): FloatArray {
    val color = Color(rgb.toLong() or OPAQUE)
    val (r, g, b) = Triple(color.red, color.green, color.blue)
    val max = maxOf(r, g, b)
    val delta = max - minOf(r, g, b)
    // 가장 큰 채널이 있는 육분면에서 시작해 색상을 셈
    val sector =
        when {
            delta == 0f -> 0f
            max == r -> ((g - b) / delta).mod(SECTORS)
            max == g -> (b - r) / delta + GREEN_SECTOR
            else -> (r - g) / delta + BLUE_SECTOR
        }
    return floatArrayOf(sector * SIXTH, if (max == 0f) 0f else delta / max, max)
}

/** 칸에 넣는 여섯 자리('#'은 칸 앞에 고정) */
private fun formatHex(rgb: Int) = (rgb and RGB).toString(HEX).padStart(HEX_DIGITS, '0').uppercase()

private fun parseHex(text: String): Int? = text.takeIf { it.length == HEX_DIGITS }?.toIntOrNull(HEX)

private val PAD_HEIGHT = 120.dp
private val BAR_HEIGHT = 28.dp

// 띠 위아래로 넓혀 누르는 곳 48
private val BAR_SLACK = 10.dp
private val FIELD_HEIGHT = 48.dp
private val KNOB = 28.dp
private val KNOB_BORDER = 3.dp
private val PAD_RADIUS = 12.dp
private val PREVIEW = 24.dp

// 손가락(약 48) 위로 비켜 보이는 크기와 거리
private val LOUPE = 48.dp
private val LOUPE_LIFT = 56.dp
private val LOUPE_OUTLINE_WIDTH = 1.dp
private const val LOUPE_OUTLINE = 0.12f
private const val FULL_TURN = 360f
private const val SIXTH = 60f
private const val SECTORS = 6f
private const val GREEN_SECTOR = 2f
private const val BLUE_SECTOR = 4f
private const val HEX = 16
private const val HEX_DIGITS = 6
private const val RGB = 0xFFFFFF
private const val OPAQUE = 0xFF000000
private const val PERCENT = 100
private const val STEPS = 0
private val HUES = listOf(0f, 60f, 120f, 180f, 240f, 300f, 360f)
