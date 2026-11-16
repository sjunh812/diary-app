import XCTest

/**
 * 실기기 화면 시험의 바탕. 한국어 기기에서 화면 문구로 찾아 누름
 * 단계마다 화면을 남겨 사람이 봄(`scripts/ios/device-test-shots.sh`)
 */
class DeviceTestCase: XCTestCase {
    let app = XCUIApplication()
    let springboard = XCUIApplication(bundleIdentifier: "com.apple.springboard")
    var step = 0

    override func setUp() {
        continueAfterFailure = true
    }

    func element(_ label: String) -> XCUIElement {
        app.descendants(matching: .any).matching(NSPredicate(format: "label == %@", label)).firstMatch
    }

    func contains(_ text: String) -> XCUIElement {
        app.descendants(matching: .any).matching(NSPredicate(format: "label CONTAINS %@", text)).firstMatch
    }

    @discardableResult
    func tap(_ label: String, timeout: TimeInterval = 10) -> Bool {
        press(element(label), label, timeout)
    }

    @discardableResult
    func tapContains(_ text: String, timeout: TimeInterval = 10) -> Bool {
        press(contains(text), text, timeout)
    }

    func tapIfExists(_ label: String, timeout: TimeInterval = 2) {
        let target = element(label)
        if target.waitForExistence(timeout: timeout) { target.tap() }
    }

    func shot(_ name: String) {
        step += 1
        let attachment = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        attachment.name = String(format: "%02d-%@", step, name)
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    /** 시스템 창. 로그인 허용은 계속, 추적·알림은 덜 쓰는 쪽 */
    func handleSystemAlert(timeout: TimeInterval = 4) {
        let alert = springboard.alerts.firstMatch
        guard alert.waitForExistence(timeout: timeout) else { return }
        shot("system-alert")
        let labels = ["계속", "Continue", "앱에 추적 금지 요청", "앱에 추적하지 않도록 요청", "Ask App Not to Track", "허용 안 함", "Don't Allow"]
        if let label = labels.first(where: { alert.buttons[$0].exists }) {
            alert.buttons[label].tap()
        } else {
            alert.buttons.element(boundBy: 0).tap()
        }
    }

    private func press(_ target: XCUIElement, _ name: String, _ timeout: TimeInterval) -> Bool {
        guard target.waitForExistence(timeout: timeout) else {
            XCTFail("못 찾음: \(name)")
            shot("missing-\(name)")
            return false
        }
        target.tap()
        return true
    }
}
