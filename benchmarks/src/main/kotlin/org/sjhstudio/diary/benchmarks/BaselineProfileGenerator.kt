package org.sjhstudio.diary.benchmarks

import android.os.SystemClock
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

    /**
     * 가장 자주 쓰는 일인데 처음 열 때 편집기 코드가 해석 실행되어 버벅였다(2026-11-18 실기기)
     * 쓰기 화면을 열고 글을 넣고 기분 시트를 연 뒤 글을 비우고 닫는다
     * 글이 남은 채 닫으면 임시 저장본이 회차마다 쌓여 다음 측정(WriteOpenBenchmark)이 쓰던 글 시트에 막힘
     */
    @Test
    fun writeEntry() = rule.collect(packageName = PACKAGE_NAME) {
        pressHome()
        startActivityAndWait()
        waitForTabs()
        checkNotNull(device.wait(Until.findObject(By.desc(WRITE_BUTTON)), TIMEOUT)) { "쓰기 버튼을 찾지 못했다" }.click()
        device.wait(Until.findObject(By.text(NEW_ENTRY)), SHEET_TIMEOUT)?.click()
        val editor = checkNotNull(device.wait(Until.findObject(By.clazz(EDIT_TEXT)), TIMEOUT)) { "편집기를 찾지 못했다" }
        editor.click()
        editor.text = SAMPLE_TEXT
        device.waitForIdle()
        open(MOOD_CHIP)
        // 시트가 뜨기 전에 뒤로 가기를 누르면 쓰기 화면이 닫힘
        check(device.wait(Until.hasObject(By.text(GOOD_MOOD)), TIMEOUT)) { "기분 시트가 뜨지 않았다" }
        // 키보드가 떠 있으면 첫 뒤로 가기가 키보드만 닫아 시트가 남음
        device.pressBack()
        if (!device.wait(Until.gone(By.text(GOOD_MOOD)), SHEET_TIMEOUT)) device.pressBack()
        check(device.wait(Until.gone(By.text(GOOD_MOOD)), TIMEOUT)) { "기분 시트가 닫히지 않았다" }
        checkNotNull(device.wait(Until.findObject(By.clazz(EDIT_TEXT)), TIMEOUT)) { "편집기를 찾지 못했다" }.text = ""
        device.waitForIdle()
        checkNotNull(device.wait(Until.findObject(By.desc(CLOSE)), TIMEOUT)) { "닫기를 찾지 못했다" }.click()
        waitForTabs()
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
        check(waitUntil { opened(row) }) { "$row 화면으로 넘어가지 않았다" }
        device.pressBack()
        check(waitUntil { !opened(row) && device.hasObject(By.text(SETTINGS_LIST_ANCHOR)) }) { "$row 에서 설정으로 돌아오지 않았다" }
    }

    // 폰은 목록이 사라지고, 넓은 화면은 목록 옆 칸에 그 줄 이름이 제목으로 하나 더 뜸
    private fun MacrobenchmarkScope.opened(row: Pattern): Boolean =
        !device.hasObject(By.text(SETTINGS_LIST_ANCHOR)) || device.findObjects(By.text(row)).size > 1

    private fun waitUntil(condition: () -> Boolean): Boolean {
        val end = SystemClock.uptimeMillis() + TIMEOUT
        while (SystemClock.uptimeMillis() < end) {
            if (condition()) return true
            SystemClock.sleep(POLL_MILLIS)
        }
        return condition()
    }

    private fun MacrobenchmarkScope.open(text: Pattern) {
        val node = checkNotNull(device.wait(Until.findObject(By.text(text)), TIMEOUT)) { "$text 를 찾지 못했다" }
        node.click()
        device.waitForIdle()
    }

    private companion object {
        const val PACKAGE_NAME = "org.sjhstudio.diary"
        const val TIMEOUT = 10_000L
        const val POLL_MILLIS = 100L
        const val FLINGS = 10
        const val GESTURE_MARGIN_DIVISOR = 5

        // 쓰던 글 시트는 없을 수도 있어 짧게 기다림
        const val SHEET_TIMEOUT = 2_000L
        const val EDIT_TEXT = "android.widget.EditText"
        const val SAMPLE_TEXT = "오늘은 한강까지 걸었다.\n바람이 차가워졌는데 그래서 더 좋았다."

        // 기기 언어가 무엇이든 찾도록 세 언어 이름을 다 둔다(shared/app·feature/settings 문자열 리소스와 같다).
        val HOME_TAB: Pattern = Pattern.compile("홈|Home|ホーム")
        val CALENDAR_TAB: Pattern = Pattern.compile("달력|Calendar|カレンダー")
        val SETTINGS_TAB: Pattern = Pattern.compile("설정|Settings|設定")
        val TEXT_SIZE_ROW: Pattern = Pattern.compile("본문 글자 크기|Diary text size|本文の文字サイズ")

        // 줄 화면들에는 없는 설정 목록 줄
        val SETTINGS_LIST_ANCHOR: Pattern = Pattern.compile("글꼴|Font|フォント")
        val THEME_ROW: Pattern = Pattern.compile("화면 모드|Appearance|表示モード")

        // shared/app write_new, feature/write write_new_entry·write_mood·write_close와 같다
        val WRITE_BUTTON: Pattern = Pattern.compile("일기 쓰기|New entry|日記を書く")
        val NEW_ENTRY: Pattern = Pattern.compile("새로 쓰기|New entry|新しく書く")
        val MOOD_CHIP: Pattern = Pattern.compile("기분|Mood|気分")
        val GOOD_MOOD: Pattern = Pattern.compile("좋음|Good|良い")
        val CLOSE: Pattern = Pattern.compile("닫기|Close|閉じる")
    }
}
