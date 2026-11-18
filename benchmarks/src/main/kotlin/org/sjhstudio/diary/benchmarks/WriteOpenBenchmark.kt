package org.sjhstudio.diary.benchmarks

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import java.util.regex.Pattern
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 앱을 막 켜고 쓰기 화면을 처음 열 때의 프레임 시간을 잰다
 * 미리 컴파일이 없는 경우(adb로 깐 앱)와 Baseline Profile만 쓴 경우(Play로 받은 앱)를 견준다
 * 쓰던 글이 없는 기기에서 돈다. 빈 쓰기 화면은 프로세스를 끝내도 임시 저장본을 남기지 않는다
 * Android 14 이상에서 돈다. 그 아래는 컴파일 초기화가 앱을 다시 깔아 일기가 지워짐
 */
@RunWith(AndroidJUnit4::class)
class WriteOpenBenchmark {
    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun noPrecompile() = open(CompilationMode.None())

    @Test
    fun baselineProfile() = open(CompilationMode.Partial(BaselineProfileMode.Require))

    private fun open(compilation: CompilationMode) = rule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = compilation,
        iterations = ITERATIONS,
        // 프로세스를 새로 띄워 홈까지는 준비로 두고, 쓰기 버튼을 누른 뒤의 프레임만 잰다
        // StartupMode.COLD는 준비 뒤에 프로세스를 끝내 직접 끝내고 띄움
        setupBlock = {
            killProcess()
            pressHome()
            startActivityAndWait()
            check(device.wait(Until.hasObject(By.text(CALENDAR_TAB)), TIMEOUT)) { "탭이 뜨지 않았다" }
        }
    ) {
        checkNotNull(device.wait(Until.findObject(By.desc(WRITE_BUTTON)), TIMEOUT)) { "쓰기 버튼을 찾지 못했다" }.click()
        check(device.wait(Until.hasObject(By.clazz(EDIT_TEXT)), TIMEOUT)) { "편집기가 뜨지 않았다" }
        device.waitForIdle()
    }

    private companion object {
        const val PACKAGE_NAME = "org.sjhstudio.diary"
        const val TIMEOUT = 10_000L
        const val ITERATIONS = 10
        const val EDIT_TEXT = "android.widget.EditText"

        val CALENDAR_TAB: Pattern = Pattern.compile("달력|Calendar|カレンダー")
        val WRITE_BUTTON: Pattern = Pattern.compile("일기 쓰기|New entry|日記を書く")
    }
}
