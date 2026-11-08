package org.sjhstudio.diary.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.sjhstudio.diary.core.designsystem.resources.Res
import org.sjhstudio.diary.core.designsystem.resources.ds_am
import org.sjhstudio.diary.core.designsystem.resources.ds_next
import org.sjhstudio.diary.core.designsystem.resources.ds_pm
import org.sjhstudio.diary.core.designsystem.resources.ds_previous
import org.sjhstudio.diary.core.designsystem.resources.ds_wheel_ampm
import org.sjhstudio.diary.core.designsystem.resources.ds_wheel_hour
import org.sjhstudio.diary.core.designsystem.resources.ds_wheel_minute
import org.sjhstudio.diary.core.designsystem.resources.ds_wheel_month
import org.sjhstudio.diary.core.designsystem.resources.ds_wheel_year
import org.sjhstudio.diary.core.designsystem.theme.DiaryShapes
import org.sjhstudio.diary.core.designsystem.theme.DiarySpacing
import org.sjhstudio.diary.core.designsystem.theme.DiaryTheme
import org.sjhstudio.diary.core.designsystem.theme.diaryLanguage
import org.sjhstudio.diary.core.designsystem.theme.limitFontScale

/**
 * 휠 한 칸 높이.
 * 다섯 칸이 보이고 가운데가 고른 값이다(`docs/design.md` 시각 고르기).
 */
private val WheelRow: Dp = 40.dp
private const val VISIBLE_ROWS = 5
private val WheelColumnWidth: Dp = 72.dp

/** 달 이름이 긴 언어(September)도 줄지 않는 폭. */
private val MonthWheelColumnWidth: Dp = 128.dp
private const val MONTHS = 12

/**
 * 시·분은 끝에서 처음으로 이어 돈다.
 * 이만큼 되풀이해 두고 가운데에서 시작한다.
 */
private const val LOOP_REPEAT = 400

/**
 * 시각 고르기 휠(`docs/design.md` 시각 고르기, HIG 피커의 휠).
 * 바텀 시트 안에 둔다.
 * 오전·오후 / 시 / 분(1분 단위) 세 열이고, [is24Hour]면 오전·오후 열 없이 0~23시다.
 * 한국어·일본어는 오전·오후가 앞, 영어는 뒤에 온다(플랫폼 시각 형식과 같은 순서).
 * 돌리다 멈출 때마다 [onChange]로 하루 중 몇 분째인지(0~1439)를 알린다.
 * 글자는 칸이 정해져 있어 기기 글자 크기를 1.3배까지만 따른다.
 * [rows]는 보이는 칸 수(홀수)다.
 * 달력과 한 시트에 놓일 때는 3으로 줄여 시트가 스크롤되지 않게 한다.
 */
@Composable
fun DiaryTimeWheel(minuteOfDay: Int, onChange: (Int) -> Unit, is24Hour: Boolean, modifier: Modifier = Modifier, rows: Int = VISIBLE_ROWS) {
    var hour by remember { mutableIntStateOf(minuteOfDay / 60) }
    var minute by remember { mutableIntStateOf(minuteOfDay % 60) }
    val change by rememberUpdatedState(onChange)
    fun emit() = change(hour * 60 + minute)

    val ampmFirst = diaryLanguage() != "en"
    val ampm =
        @Composable {
            WheelColumn(
                labels = listOf(stringResource(Res.string.ds_am), stringResource(Res.string.ds_pm)),
                selected = if (hour >= 12) 1 else 0,
                loop = false,
                description = stringResource(Res.string.ds_wheel_ampm),
                rows = rows
            ) { index ->
                hour = hour % 12 + index * 12
                emit()
            }
        }
    val colors = DiaryTheme.colors
    Box(modifier.fillMaxWidth().height(WheelRow * rows), contentAlignment = Alignment.Center) {
        // 가운데 고른 칸 띠
        Box(Modifier.fillMaxWidth().height(WheelRow).clip(DiaryShapes.r12).background(colors.sunken))
        Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2)) {
            if (!is24Hour && ampmFirst) ampm()
            WheelColumn(
                labels = if (is24Hour) (0..23).map { it.toString() } else (listOf(12) + (1..11)).map { it.toString() },
                selected = if (is24Hour) hour else hour % 12,
                loop = true,
                description = stringResource(Res.string.ds_wheel_hour),
                rows = rows
            ) { index ->
                hour = if (is24Hour) index else index + if (hour >= 12) 12 else 0
                emit()
            }
            WheelColumn(
                labels = (0..59).map { it.toString().padStart(2, '0') },
                selected = minute,
                loop = true,
                description = stringResource(Res.string.ds_wheel_minute),
                rows = rows
            ) { index ->
                minute = index
                emit()
            }
            if (!is24Hour && !ampmFirst) ampm()
        }
    }
}

/**
 * 연·월 고르기 휠.
 * 달력의 연월 제목을 누르면 날짜 칸 자리에 나온다(HIG 피커의 휠, `docs/design.md` 시각 고르기와 같은 규격).
 * 한국어·일본어는 연이 앞, 영어는 달이 앞이다.
 * 달은 끝에서 처음으로 이어 돌고 연은 [years] 끝에서 멈춘다.
 */
@Composable
fun DiaryMonthWheel(
    year: Int,
    month: Int,
    years: IntRange,
    yearLabel: (Int) -> String,
    monthLabel: (Int) -> String,
    onChange: (year: Int, month: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var shownYear by remember { mutableIntStateOf(year) }
    var shownMonth by remember { mutableIntStateOf(month) }
    val change by rememberUpdatedState(onChange)
    val yearColumn =
        @Composable {
            WheelColumn(
                labels = years.map(yearLabel),
                selected = year - years.first,
                loop = false,
                description = stringResource(Res.string.ds_wheel_year),
                width = MonthWheelColumnWidth
            ) { index ->
                shownYear = years.first + index
                change(shownYear, shownMonth)
            }
        }
    val monthColumn =
        @Composable {
            WheelColumn(
                labels = (1..MONTHS).map(monthLabel),
                selected = month - 1,
                loop = true,
                description = stringResource(Res.string.ds_wheel_month),
                width = MonthWheelColumnWidth
            ) { index ->
                shownMonth = index + 1
                change(shownYear, shownMonth)
            }
        }
    Box(modifier.fillMaxWidth().height(WheelRow * VISIBLE_ROWS), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxWidth().height(WheelRow).clip(DiaryShapes.r12).background(DiaryTheme.colors.sunken))
        Row(horizontalArrangement = Arrangement.spacedBy(DiarySpacing.x2)) {
            if (diaryLanguage() == "en") {
                monthColumn()
                yearColumn()
            } else {
                yearColumn()
                monthColumn()
            }
        }
    }
}

/** 휠 안에서 남은 끌기와 튕김을 다 써 버려서, 휠 끝에 닿아도 바텀 시트가 끌려 내려가지 않게 한다. */
private val ConsumeRemaining =
    object : NestedScrollConnection {
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset = available

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = available
    }

@Composable
private fun WheelColumn(
    labels: List<String>,
    selected: Int,
    loop: Boolean,
    description: String,
    width: Dp = WheelColumnWidth,
    rows: Int = VISIBLE_ROWS,
    onSelect: (Int) -> Unit
) {
    val size = labels.size
    val count = if (loop) size * LOOP_REPEAT else size
    val state = rememberLazyListState(initialFirstVisibleItemIndex = if (loop) size * (LOOP_REPEAT / 2) + selected else selected)
    val fling = rememberSnapFlingBehavior(state, SnapPosition.Center)
    val scope = rememberCoroutineScope()
    val select by rememberUpdatedState(onSelect)
    // 보이는 칸 가운데 줄에 가장 가까운 칸이 고른 칸이다.
    val center by remember {
        derivedStateOf {
            val info = state.layoutInfo
            val middle = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - middle) }?.index ?: state.firstVisibleItemIndex
        }
    }
    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress }.filter { !it }.collect { select(center % size) }
    }
    val typography = DiaryTheme.typography
    val previous = stringResource(Res.string.ds_previous)
    val next = stringResource(Res.string.ds_next)
    fun moveBy(step: Int): Boolean {
        val target = center + step
        if (target !in 0 until count) return false
        // 맨 위 칸 앞에 반 화면만큼 여백이 있어서, 그 칸으로 굴리면 가운데에 온다.
        scope.launch { state.animateScrollToItem(target) }
        return true
    }
    LazyColumn(
        state = state,
        flingBehavior = fling,
        contentPadding = PaddingValues(vertical = WheelRow * (rows / 2)),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier
                .width(width)
                .height(WheelRow * rows)
                .nestedScroll(ConsumeRemaining)
                // 화면 읽기 프로그램은 열 하나를 "시, 10"처럼 읽고, 이전·다음 동작으로 한 칸씩 옮긴다.
                .clearAndSetSemantics {
                    contentDescription = description
                    stateDescription = labels[center % size]
                    customActions =
                        listOf(
                            CustomAccessibilityAction(previous) { moveBy(-1) },
                            CustomAccessibilityAction(next) { moveBy(1) }
                        )
                }
    ) {
        items(count) { index ->
            val distance = abs(index - center)
            Box(Modifier.width(width).height(WheelRow), contentAlignment = Alignment.Center) {
                BasicText(
                    labels[index % size],
                    style = wheelStyle(distance, typography.numeral, typography.reading, typography.body),
                    maxLines = 1
                )
            }
        }
    }
}

/** 가운데는 `numeral` `textPrimary`, 한 칸 떨어지면 `reading` `textTertiary`, 그 밖은 `body` `placeholder`. */
@Composable
private fun wheelStyle(distance: Int, center: TextStyle, near: TextStyle, far: TextStyle): TextStyle {
    val colors = DiaryTheme.colors
    val style =
        when (distance) {
            0 -> center.copy(color = colors.textPrimary)
            1 -> near.copy(color = colors.textTertiary)
            else -> far.copy(color = colors.placeholder)
        }
    return style.limitFontScale().copy(textAlign = TextAlign.Center)
}
