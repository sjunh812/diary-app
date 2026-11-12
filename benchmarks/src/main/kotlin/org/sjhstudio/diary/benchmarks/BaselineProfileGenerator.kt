package org.sjhstudio.diary.benchmarks

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import java.util.regex.Pattern
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 앱의 Baseline Profile을 만든다(스펙 001 T077, research R10).
 * 잠금이 없고 편지·온보딩을 넘긴 앱으로 돈다. 그래서 앱을 켜면 바로 앱 틀의 홈 탭이 뜬다.
 * 홈 목록 넘기기는 일기가 쌓여 있어야 한다. 1.7.4 견본을 이전한 Android 14 이상 기기에서 돈다(release-prep 스킬).
 * Android 13 이하는 컴파일 초기화가 앱을 다시 깔아 일기가 지워짐
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

    /** 처음 보는 일기 줄을 그리는 코드가 해석 실행되지 않게 홈 목록을 끝까지 넘긴다 */
    @Test
    fun homeListScroll() = rule.collect(packageName = PACKAGE_NAME) {
        pressHome()
        startActivityAndWait()
        waitForTabs()
        // 태블릿은 오른쪽 상세 칸도 스크롤되어 가장 긴 칸을 목록으로 봄
        val list = checkNotNull(device.wait(Until.findObjects(By.scrollable(true)), TIMEOUT)?.maxByOrNull { it.visibleBounds.height() }) {
            "목록을 찾지 못했다"
        }
        // 가장자리에서 시작하면 시스템 뒤로 가기 몸짓이 됨
        list.setGestureMargin(device.displayWidth / GESTURE_MARGIN_DIVISOR)
        repeat(FLINGS) { list.fling(Direction.DOWN) }
        repeat(FLINGS) { list.fling(Direction.UP) }
    }

    private fun MacrobenchmarkScope.waitForTabs() {
        check(device.wait(Until.hasObject(By.text(CALENDAR_TAB)), TIMEOUT)) { "탭이 뜨지 않았다" }
    }

    /**
     * 설정 줄을 눌러 들어갔다가 뒤로 나온다.
     * "글꼴" 줄이 보이면 설정 목록이다. 넓은 화면은 탭이 남아 탭으로는 알 수 없다.
     * 화면이 바뀌기 전에 뒤로 가기를 누르면 앱이 닫힌다.
     */
    private fun MacrobenchmarkScope.visit(row: Pattern) {
        open(row)
        check(device.wait(Until.gone(By.text(SETTINGS_LIST_ANCHOR)), TIMEOUT)) { "$row 화면으로 넘어가지 않았다" }
        device.pressBack()
        check(device.wait(Until.hasObject(By.text(SETTINGS_LIST_ANCHOR)), TIMEOUT)) { "$row 에서 설정으로 돌아오지 않았다" }
    }

    private fun MacrobenchmarkScope.open(text: Pattern) {
        val node = checkNotNull(device.wait(Until.findObject(By.text(text)), TIMEOUT)) { "$text 를 찾지 못했다" }
        node.click()
        device.waitForIdle()
    }

    private companion object {
        const val PACKAGE_NAME = "org.sjhstudio.diary"
        const val TIMEOUT = 10_000L
        const val FLINGS = 10
        const val GESTURE_MARGIN_DIVISOR = 5

        // 기기 언어가 무엇이든 찾도록 세 언어 이름을 다 둔다(shared/app·feature/settings 문자열 리소스와 같다).
        val HOME_TAB: Pattern = Pattern.compile("홈|Home|ホーム")
        val CALENDAR_TAB: Pattern = Pattern.compile("달력|Calendar|カレンダー")
        val SETTINGS_TAB: Pattern = Pattern.compile("설정|Settings|設定")
        val TEXT_SIZE_ROW: Pattern = Pattern.compile("본문 글자 크기|Diary text size|本文の文字サイズ")

        // 줄 화면들에는 없는 설정 목록 줄
        val SETTINGS_LIST_ANCHOR: Pattern = Pattern.compile("글꼴|Font|フォント")
        val THEME_ROW: Pattern = Pattern.compile("화면 모드|Appearance|表示モード")
    }
}
