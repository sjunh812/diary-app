import XCTest

/**
 * 실기기 구글 드라이브 백업·복원 왕복. 구글 로그인은 사람이 함
 * 차례: testBackup → 앱 삭제 → testRestoreAfterReinstall
 * 복원은 켤 때 뜨는 가장 최근 백업(방금 이 기기에서 올린 것)만 합침. 다른 기기의 일기는 받지 않음
 */
final class DriveFlow: DeviceTestCase {
    private let foundTitle = "구글 드라이브에 백업이 있어요"

    func testBackup() {
        app.launch()
        sleep(2)
        handleSystemAlert()
        openBackup()
        connectIfNeeded()
        // 연결하면 드라이브에 있던 다른 기기 백업을 알려 줌. 받지 않음
        if element(foundTitle).waitForExistence(timeout: 8) {
            shot("found-sheet")
            tap("나중에")
        }
        // 연결 직후 첫 백업이 저절로 돌 수 있어 단추가 있으면만 누름
        let now = element("지금 백업")
        if now.waitForExistence(timeout: 5), now.isEnabled { now.tap() }
        shot("backup-started")
        let done = app.descendants(matching: .any)
            .matching(NSPredicate(format: "label CONTAINS %@ OR label CONTAINS %@", "모두 백업했어요", "사진까지 모두")).firstMatch
        let finished = done.waitForExistence(timeout: 300)
        shot("backup-finished")
        XCTAssertTrue(finished, "백업이 끝나지 않음")
    }

    func testRestoreAfterReinstall() {
        app.launch()
        sleep(2)
        skipOnboarding()
        handleSystemAlert()
        openBackup()
        connectIfNeeded()
        // 앱을 켜 두면 홈에서도 같은 안내가 뜸
        let found = element(foundTitle)
        if !found.waitForExistence(timeout: 20) {
            app.terminate()
            app.launch()
            XCTAssertTrue(found.waitForExistence(timeout: 30), "드라이브 백업 안내가 뜨지 않음")
        }
        shot("found-sheet")
        tap("복원하기")
        if element("합치기").waitForExistence(timeout: 30) {
            shot("restore-preview")
            tap("합치기")
        }
        let done = app.descendants(matching: .any)
            .matching(NSPredicate(format: "label CONTAINS %@ OR label CONTAINS %@", "추가했어요", "합쳤어요")).firstMatch
        let finished = done.waitForExistence(timeout: 300)
        shot("restore-done")
        XCTAssertTrue(finished, "복원이 끝나지 않음")
        app.terminate()
        app.launch()
        sleep(4)
        shot("home-after-restore")
    }

    private func skipOnboarding() {
        var pages = 0
        while element("다음").waitForExistence(timeout: 3), pages < 6 {
            element("다음").tap()
            pages += 1
        }
        for label in ["시작하기", "시작", "건너뛰기"] { tapIfExists(label, timeout: 1) }
    }

    private func openBackup() {
        tap("설정")
        sleep(1)
        tapContains("백업과 복원")
        sleep(2)
        shot("backup-screen")
    }

    /** 연결 안 됐으면 연결. 사람이 로그인할 때까지 7분 기다림 */
    private func connectIfNeeded() {
        let connect = element("구글 드라이브 연결하기")
        guard connect.waitForExistence(timeout: 5) else { return }
        connect.tap()
        handleSystemAlert(timeout: 6)
        sleep(3)
        shot("google-sign-in")
        let connected = element("지금 백업")
        let found = element(foundTitle)
        let deadline = Date().addingTimeInterval(420)
        while Date() < deadline, !connected.exists, !found.exists { sleep(2) }
        shot("connected")
        XCTAssertTrue(connected.exists || found.exists, "로그인이 끝나지 않음")
    }
}
