package org.sjhstudio.diary.benchmarks

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 콜드 스타트 시간을 잰다(SC-007, research R10).
 * Baseline Profile 없이 잰 것과 프로필을 쓴 것을 나란히 두어 프로필이 시작을 줄이는지 본다.
 * 목록 넘기기는 스펙 002 홈 목록이 생기면 더한다.
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun coldStartupWithoutProfile() = startup(CompilationMode.None())

    @Test
    fun coldStartupWithProfile() = startup(CompilationMode.Partial(BaselineProfileMode.Require))

    private fun startup(compilation: CompilationMode) = rule.measureRepeated(
        packageName = "org.sjhstudio.diary",
        metrics = listOf(StartupTimingMetric()),
        compilationMode = compilation,
        iterations = 10,
        startupMode = StartupMode.COLD
    ) {
        pressHome()
        startActivityAndWait()
    }
}
