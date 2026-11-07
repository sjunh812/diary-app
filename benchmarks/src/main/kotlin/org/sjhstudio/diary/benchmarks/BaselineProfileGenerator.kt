package org.sjhstudio.diary.benchmarks

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import java.util.regex.Pattern
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 앱의 Baseline Profile을 만든다(스펙 001 T077, research R10).
 * 새로 깐 앱(옛 기록·잠금 없음)으로 돈다. 그래서 앱을 켜면 바로 앱 틀의 홈 탭이 뜬다.
 *
 * 시작 프로필(앱을 켤 때 읽는 클래스를 첫 dex에 모으는 것)에는 켜서 탭이 뜰 때까지만 넣는다.
 * 다른 화면까지 넣으면 첫 dex가 커져 오히려 시작이 느려진다(오발있과 같다).
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun startupToTabs() = rule.collect(packageName = PACKAGE_NAME, includeInStartupProfile = true) {
        pressHome()
        startActivityAndWait()
        waitForTabs()
    }

    @Test
    fun settingsScreens() = rule.collect(packageName = PACKAGE_NAME) {
        pressHome()
        startActivityAndWait()
        waitForTabs()
        open(SETTINGS_TAB)
        visit(TEXT_SIZE_ROW)
        visit(THEME_ROW)
        open(CALENDAR_TAB)
        open(HOME_TAB)
    }

    private fun MacrobenchmarkScope.waitForTabs() {
        check(device.wait(Until.hasObject(By.text(CALENDAR_TAB)), TIMEOUT)) { "탭이 뜨지 않았다" }
    }

    /**
     * 설정 줄을 눌러 들어갔다가 뒤로 나온다.
     * 하단 탭은 탭의 맨 위 화면에서만 보여서, 탭이 사라지고 다시 생기는 것으로 화면이 바뀐 것을 안다.
     * 바뀌기 전에 뒤로 가기를 누르면 앱이 닫힌다.
     */
    private fun MacrobenchmarkScope.visit(row: Pattern) {
        open(row)
        check(device.wait(Until.gone(By.text(CALENDAR_TAB)), TIMEOUT)) { "$row 화면으로 넘어가지 않았다" }
        device.pressBack()
        check(device.wait(Until.hasObject(By.text(CALENDAR_TAB)), TIMEOUT)) { "$row 에서 설정으로 돌아오지 않았다" }
    }

    private fun MacrobenchmarkScope.open(text: Pattern) {
        val node = checkNotNull(device.wait(Until.findObject(By.text(text)), TIMEOUT)) { "$text 를 찾지 못했다" }
        node.click()
        device.waitForIdle()
    }

    private companion object {
        const val PACKAGE_NAME = "org.sjhstudio.diary"
        const val TIMEOUT = 10_000L

        // 기기 언어가 무엇이든 찾도록 세 언어 이름을 다 둔다(shared/app·feature/settings 문자열 리소스와 같다).
        val HOME_TAB: Pattern = Pattern.compile("홈|Home|ホーム")
        val CALENDAR_TAB: Pattern = Pattern.compile("달력|Calendar|カレンダー")
        val SETTINGS_TAB: Pattern = Pattern.compile("설정|Settings|設定")
        val TEXT_SIZE_ROW: Pattern = Pattern.compile("본문 글자 크기|Diary text size|本文の文字サイズ")
        val THEME_ROW: Pattern = Pattern.compile("화면 모드|Appearance|表示モード")
    }
}
