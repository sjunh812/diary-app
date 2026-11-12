package org.sjhstudio.diary.benchmarks

import android.os.Build
import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMacrobenchmarkApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import java.util.regex.Pattern
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 홈 목록을 빠르게 넘기며 프레임 시간을 잰다(스펙 001 SC-010, 스펙 002 SC-010).
 * 일기가 쌓인 기기에서 돈다. 1.7.4에 견본을 넣고 이전한 뒤 이 빌드를 덮어 깐다(test-report "스크롤 성능").
 * Android 14부터는 컴파일을 지우고 Baseline Profile만 쓴 상태로 잰다(새로 깐 사용자와 같음).
 * Android 13 이하는 컴파일 초기화가 앱을 지우고 다시 깔아 일기가 사라짐.
 * 그래서 컴파일은 건드리지 않고, 재기 전에 `cmd package compile -f -m speed-profile`로 맞춘다.
 */
@OptIn(ExperimentalMacrobenchmarkApi::class)
@RunWith(AndroidJUnit4::class)
class ScrollBenchmark {
    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun homeList() = rule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = compilation(),
        iterations = ITERATIONS,
        startupMode = StartupMode.WARM,
        setupBlock = {
            pressHome()
            startActivityAndWait()
            check(device.wait(Until.hasObject(By.text(CALENDAR_TAB)), TIMEOUT)) { "탭이 뜨지 않았다" }
        }
    ) {
        flingList()
    }

    private fun compilation(): CompilationMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        CompilationMode.Partial(BaselineProfileMode.Require)
    } else {
        CompilationMode.Ignore()
    }

    private fun MacrobenchmarkScope.flingList() {
        // 태블릿은 오른쪽 상세 칸도 스크롤되어 가장 긴 칸을 목록으로 봄
        val list = checkNotNull(device.wait(Until.findObjects(By.scrollable(true)), TIMEOUT)?.maxByOrNull { it.visibleBounds.height() }) {
            "목록을 찾지 못했다"
        }
        // 가장자리에서 시작하면 시스템 뒤로 가기 몸짓이 됨
        list.setGestureMargin(device.displayWidth / GESTURE_MARGIN_DIVISOR)
        repeat(FLINGS) { list.fling(Direction.DOWN) }
        repeat(FLINGS) { list.fling(Direction.UP) }
    }

    private companion object {
        const val PACKAGE_NAME = "org.sjhstudio.diary"
        const val TIMEOUT = 10_000L
        const val ITERATIONS = 5
        const val FLINGS = 8
        const val GESTURE_MARGIN_DIVISOR = 5

        val CALENDAR_TAB: Pattern = Pattern.compile("달력|Calendar|カレンダー")
    }
}
