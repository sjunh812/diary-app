import XCTest

/** 실기기 샌드박스 구매. Apple 로그인·결제 확인은 사람이 누름 */
final class PurchaseFlow: DeviceTestCase {
    func openSettings() {
        app.launch()
        if element("건너뛰기").waitForExistence(timeout: 8) { element("건너뛰기").tap() }
        shot("home")
        tap("설정")
        shot("settings")
    }

    /** 광고 제거를 삼. Apple 로그인·결제 확인은 사람이 누를 때까지 기다림 */
    func testBuyAdFree() {
        openSettings()
        tap("Plus 알아보기")
        tap("광고만 없애기")
        tap("광고 없애기")
        let bought = element("Plus로 더 편하게")
        XCTAssertTrue(bought.waitForExistence(timeout: 240), "구매가 끝나지 않음")
        shot("after-buy")
        tap("홈")
        sleep(3)
        shot("home-after-buy")
    }

    /** 재설치 뒤 구매 복원 */
    func testRestore() {
        openSettings()
        tap("Plus 알아보기")
        tap("구매 복원")
        let restored = element("구매를 복원했어요")
        let ok = restored.waitForExistence(timeout: 240)
        shot("after-restore")
        XCTAssertTrue(ok, "복원 안내가 안 뜸")
    }

    /** 재설치 뒤 아무것도 누르지 않아도 구매가 돌아오는지 */
    func testPlanAfterReinstall() {
        openSettings()
        sleep(5)
        shot("settings-after-reinstall")
        XCTAssertTrue(element("Plus로 더 편하게").exists, "재설치 뒤 광고 제거가 돌아오지 않음")
    }
}
