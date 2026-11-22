import XCTest

/**
 * 테마 색·Plus 칩·결제 화면 그림·홈 백업 줄을 보는 실기기 시험
 * 설정을 바꾸지 않고 보기만 함. 색 고르기는 [취소]로 닫음
 */
final class ThemeFlow: DeviceTestCase {
    func testThemeAndPlusScreens() {
        app.launch()
        sleep(2)
        shot("home")

        tap("설정")
        sleep(1)
        shot("settings")

        tap("테마")
        sleep(1)
        app.swipeUp()
        shot("theme")
        // 무료는 이름 뒤에 ", Plus"가 붙음
        tapContains("직접 고르기")
        sleep(1)
        shot("custom-color")
        if element("이 색으로 하기").exists {
            // 다 지워도 '#'은 남음
            tapContains("색 코드")
            app.typeText(String(repeating: XCUIKeyboardKey.delete.rawValue, count: 8))
            shot("custom-color-cleared")
            tap("취소")
        } else {
            tapIfExists("괜찮아요")
            tapIfExists("닫기")
        }
        sleep(1)
        tapIfExists("뒤로 가기")

        tap("목록 보기")
        sleep(1)
        app.swipeUp()
        sleep(1)
        shot("list-style")
        tapIfExists("뒤로 가기")

        if element("Plus 알아보기").waitForExistence(timeout: 2) {
            tap("Plus 알아보기")
            usleep(2_500_000)
            shot("paywall-mid")
            sleep(6)
            shot("paywall-end")
            tapIfExists("닫기")
        }

        // 홈을 떠나 6초 넘게 있다 돌아와도 백업 줄이 사라졌다 나오지 않음
        sleep(6)
        tap("홈")
        shot("home-back-0s")
        usleep(300_000)
        shot("home-back-0.3s")
    }
}
