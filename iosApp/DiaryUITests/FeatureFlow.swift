import XCTest

/** 실기기 기능 점검. 단계마다 화면을 남겨 사람이 봄 */
final class FeatureFlow: DeviceTestCase {
    /** 전면 광고가 뜨면 닫음(구글 시험 광고). 닫기 단추는 몇 초 뒤에야 눌림 */
    func closeAdIfShown() {
        let close = app.descendants(matching: .any).matching(NSPredicate(format: "label == 'Close Advertisement'")).firstMatch
        guard close.waitForExistence(timeout: 20) else { return }
        shot("interstitial")
        for _ in 0..<12 {
            sleep(2)
            guard close.exists else { break }
            close.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        }
        sleep(1)
        if close.exists { XCTFail("광고를 닫지 못함") }
    }

    func typeBody(_ text: String) {
        let field = app.textViews.firstMatch.exists ? app.textViews.firstMatch : app.textFields.firstMatch
        if field.waitForExistence(timeout: 5) {
            field.tap()
            field.typeText(text)
        } else {
            XCTFail("본문 입력 칸 없음")
        }
    }

    /** 1묶음: 처음 설치부터 첫 일기, 상세, 즐겨찾기, 긴 글, 수정 중 나가기 */
    func testFirstRunAndWriting() {
        app.launch()
        sleep(2)
        shot("launch")
        // 온보딩
        var pages = 0
        while element("다음").waitForExistence(timeout: 3), pages < 6 {
            shot("onboarding-\(pages + 1)")
            element("다음").tap()
            pages += 1
        }
        shot("onboarding-last")
        for label in ["시작하기", "시작", "건너뛰기"] { tapIfExists(label, timeout: 1) }
        sleep(1)
        shot("home-empty")

        // 첫 일기
        tap("일기 쓰기")
        sleep(1)
        shot("write-open")
        if element("기분은 어땠나요?").waitForExistence(timeout: 3) {
            shot("mood-sheet")
            tap("좋음")
        }
        typeBody("First entry on a real device. Today was calm.")
        shot("write-typed")
        tap("저장")
        // 저장해 작성 화면이 닫히면 상세 위에 첫 전면 광고 안내, 이어서 추적 허용 창과 광고(2026-11-16 결정)
        sleep(1)
        shot("notice-after-save")
        if element("전면 광고는 하루 한 번이에요").waitForExistence(timeout: 4) { tap("확인") }
        handleSystemAlert()
        closeAdIfShown()
        // 광고를 닫으면 그 일기 상세(FR-012)
        shot("detail-after-save")
        tapIfExists("즐겨찾기 추가")
        shot("detail-favorited")
        tap("뒤로 가기")
        sleep(1)
        shot("home-after-first-save")

        // 긴 글과 날씨
        tap("일기 쓰기")
        sleep(1)
        if element("기분은 어땠나요?").waitForExistence(timeout: 3) { tap("웃음") }
        tapIfExists("날씨")
        if element("날씨는 어땠나요?").waitForExistence(timeout: 3) {
            shot("weather-sheet")
            let first = app.buttons.matching(NSPredicate(format: "label == '맑음'")).firstMatch
            if first.exists { first.tap() } else { tapIfExists("선택 안 함") }
        }
        let long = String(repeating: "Long line to check wrapping and scrolling. ", count: 30)
        typeBody(long)
        shot("write-long")
        tap("저장")
        sleep(1)
        shot("detail-long")
        // 수정 중 나가기
        do {
            tap("수정하기")
            sleep(1)
            typeBody(" edited")
            tap("닫기")
            sleep(1)
            shot("discard-dialog")
            tapIfExists("수정한 내용 버리기")
            sleep(1)
            shot("after-discard")
            tap("뒤로 가기")
            // 오늘 광고는 이미 봐서 두 번째 저장에도 뜨지 않았어야 함
            sleep(3)
            shot("home-two-entries-no-second-ad")
        }
    }

    func backgroundAndReturn(seconds: UInt32 = 2) {
        XCUIDevice.shared.press(.home)
        sleep(seconds)
        app.activate()
        sleep(2)
    }

    /** 2묶음: 검색·달력·돌아보기·휴지통·설정·잠금·임시 저장·회전·뒤로 갔다 오기. 1묶음 뒤 상태(일기 둘)에서 */
    func testSecondPass() {
        app.launch()
        sleep(2)
        tapIfExists("건너뛰기", timeout: 2)
        shot("home")

        // 검색
        tap("검색")
        sleep(1)
        shot("search-idle")
        let field = app.textFields.firstMatch
        if field.waitForExistence(timeout: 3) {
            field.tap()
            field.typeText("Long")
            sleep(1)
            shot("search-result")
            field.typeText("zzzz")
            sleep(1)
            shot("search-empty")
        } else { XCTFail("검색 칸 없음") }
        tap("뒤로 가기")

        // 달력과 돌아보기
        tap("달력")
        sleep(1)
        shot("calendar")
        tapIfExists("이전 달")
        sleep(1)
        shot("calendar-previous")
        tapIfExists("오늘")
        tap("돌아보기")
        sleep(1)
        shot("insight-week")
        for tab in ["월", "년", "전체"] {
            tapIfExists(tab)
            sleep(1)
            shot("insight-\(tab)")
        }

        // 휴지통으로 옮기고 복원
        tap("홈")
        let first = contains("First entry on a real device")
        if first.waitForExistence(timeout: 5) {
            first.tap()
            sleep(1)
            tap("더보기")
            sleep(1)
            shot("detail-more")
            tap("휴지통으로 옮기기")
            sleep(1)
            shot("after-trash")
            tapIfExists("뒤로 가기", timeout: 2)
        } else { XCTFail("첫 일기 없음") }
        tap("설정")
        tap("휴지통")
        sleep(1)
        shot("trash")
        let trashed = contains("First entry on a real device")
        if trashed.waitForExistence(timeout: 3) {
            trashed.tap()
            sleep(1)
            shot("trash-item")
            tapIfExists("복원하기")
            sleep(1)
            shot("trash-restored")
        } else { XCTFail("휴지통에 일기 없음") }
        tapIfExists("뒤로 가기", timeout: 2)

        // 화면 설정
        tap("화면 모드")
        sleep(1)
        shot("theme-sheet")
        tapIfExists("라이트")
        sleep(1)
        shot("theme-light")
        tapIfExists("뒤로 가기", timeout: 2)
        tap("본문 글자 크기")
        sleep(1)
        tapIfExists("가장 크게")
        sleep(1)
        shot("text-size-largest")
        tapIfExists("뒤로 가기", timeout: 2)
        tap("목록 보기")
        sleep(1)
        tapIfExists("카드")
        sleep(1)
        shot("list-style-card")
        tapIfExists("뒤로 가기", timeout: 2)
        tap("홈")
        sleep(1)
        shot("home-card-light-largest")
        // 되돌리기
        tap("설정")
        tap("목록 보기"); tapIfExists("피드"); tapIfExists("뒤로 가기", timeout: 2)
        tap("본문 글자 크기"); tapIfExists("보통"); tapIfExists("뒤로 가기", timeout: 2)
        tap("화면 모드"); tapIfExists("시스템 설정"); tapIfExists("뒤로 가기", timeout: 2)

        // 잠금: 4자리 두 번, 뒤로 갔다 오면 잠김, 풀기, 끄기
        tap("잠금")
        sleep(1)
        shot("lock-settings")
        tapIfExists("잠금 사용")
        sleep(1)
        shot("pin-setup")
        for _ in 0..<2 {
            for digit in ["1", "2", "3", "4"] { tapIfExists(digit, timeout: 2) }
            sleep(1)
        }
        sleep(1)
        shot("lock-on")
        handleSystemAlert()
        tapIfExists("바로", timeout: 2)
        shot("lock-relock-immediately")
        backgroundAndReturn()
        shot("locked")
        for digit in ["1", "2", "3", "4"] { tapIfExists(digit, timeout: 2) }
        sleep(1)
        shot("unlocked")
        // 틀린 비밀번호
        backgroundAndReturn()
        for digit in ["9", "9", "9", "9"] { tapIfExists(digit, timeout: 2) }
        sleep(1)
        shot("wrong-pin")
        for digit in ["1", "2", "3", "4"] { tapIfExists(digit, timeout: 2) }
        sleep(1)
        // 잠금 끄기
        if element("잠금 사용").waitForExistence(timeout: 3) {
            element("잠금 사용").tap()
            sleep(1)
            for digit in ["1", "2", "3", "4"] { tapIfExists(digit, timeout: 2) }
            sleep(1)
        }
        shot("lock-off")
        tapIfExists("뒤로 가기", timeout: 2)

        // 쓰는 중 앱을 끄면 임시 저장이 남는지
        tap("홈")
        tap("일기 쓰기")
        sleep(1)
        typeBody("Draft that must survive a kill.")
        shot("draft-typed")
        app.terminate()
        app.launch()
        sleep(3)
        shot("after-kill")
        tapIfExists("일기 쓰기", timeout: 3)
        sleep(1)
        shot("draft-reopen")
        if contains("Draft that must survive").exists == false { XCTFail("임시 저장이 남지 않음") }
        tapIfExists("닫기", timeout: 2)
        sleep(1)

        // 회전과 뒤로 갔다 오기
        XCUIDevice.shared.orientation = .landscapeLeft
        sleep(2)
        shot("landscape-home")
        XCUIDevice.shared.orientation = .portrait
        sleep(2)
        backgroundAndReturn(seconds: 5)
        shot("after-background")
    }

    /** 화면 밖이면 보일 때까지 올려서 누름 */
    func scrollTap(_ label: String) {
        let target = element(label)
        guard target.waitForExistence(timeout: 5) else { XCTFail("못 찾음: \(label)"); return }
        var tries = 0
        while !target.isHittable, tries < 6 {
            app.swipeUp()
            tries += 1
        }
        target.tap()
    }

    func pin(_ digits: String) {
        for digit in digits { tapIfExists(String(digit), timeout: 2) }
        sleep(1)
    }

    /** 3묶음: 검색 입력, 임시 저장(쓰다가 꺼짐·나갔다 꺼짐), 잠금 */
    func testThirdPass() {
        app.launch()
        sleep(2)

        // 검색
        tap("검색")
        sleep(1)
        app.typeText("Long")
        sleep(2)
        shot("search-result")
        app.typeText("zzzz")
        sleep(2)
        shot("search-empty")
        tap("뒤로 가기")

        // 쓰고 3초 지나 갑자기 꺼짐
        tap("일기 쓰기")
        sleep(1)
        typeBody("Draft A survives a crash.")
        sleep(3)
        app.terminate()
        app.launch()
        sleep(3)
        tap("일기 쓰기")
        sleep(2)
        shot("draft-a-reopen")
        if !contains("Draft A").waitForExistence(timeout: 3) { XCTFail("3초 뒤 꺼진 임시 저장이 안 보임") }
        // 이어 쓰기 안내가 있으면 이어 씀
        for label in ["이어서 쓰기", "이어 쓰기", "계속 쓰기"] { tapIfExists(label, timeout: 1) }
        sleep(1)
        shot("draft-a-continued")
        tapIfExists("닫기", timeout: 2)
        sleep(1)
        shot("draft-a-closed")

        // 쓰다가 바로 홈으로 나간 뒤 꺼짐(앱 전환기에서 닫기와 같음)
        tapIfExists("일기 쓰기", timeout: 3)
        sleep(1)
        for label in ["새로 쓰기", "새 글 쓰기"] { tapIfExists(label, timeout: 1) }
        typeBody(" Draft B")
        XCUIDevice.shared.press(.home)
        sleep(2)
        app.terminate()
        app.launch()
        sleep(3)
        tap("일기 쓰기")
        sleep(2)
        shot("draft-b-reopen")
        if !contains("Draft B").waitForExistence(timeout: 3) { XCTFail("나갔다 꺼진 임시 저장이 안 보임") }
        for label in ["이어서 쓰기", "이어 쓰기", "계속 쓰기"] { tapIfExists(label, timeout: 1) }
        sleep(1)
        shot("draft-b-continued")
        tapIfExists("닫기", timeout: 2)
        sleep(1)
        for label in ["임시 저장하기", "임시 저장", "버리기"] where element(label).exists { shot("draft-close-dialog"); break }
        tapIfExists("임시 저장하기", timeout: 1)

        // 잠금
        tap("설정")
        sleep(1)
        scrollTap("잠금")
        sleep(1)
        shot("lock-settings")
        scrollTap("잠금 사용")
        sleep(1)
        shot("pin-setup")
        pin("1234")
        shot("pin-again")
        pin("1234")
        handleSystemAlert()
        shot("lock-on")
        tapIfExists("자동 잠금", timeout: 2)
        sleep(1)
        shot("relock-options")
        tapIfExists("바로", timeout: 2)
        sleep(1)
        shot("relock-immediately")
        backgroundAndReturn()
        shot("locked")
        pin("9999")
        shot("wrong-pin")
        pin("1234")
        shot("unlocked")
        if element("잠금 사용").waitForExistence(timeout: 3) {
            element("잠금 사용").tap()
            sleep(1)
            shot("lock-off-confirm")
            pin("1234")
        }
        shot("lock-off")
        backgroundAndReturn()
        shot("not-locked-after-off")
    }

    /** 이름으로 시작하는 버튼(값이 붙은 줄·스위치) */
    func row(_ prefix: String) -> XCUIElement {
        app.descendants(matching: .any).matching(NSPredicate(format: "label BEGINSWITH %@ AND (elementType == %d OR elementType == %d)", prefix,
            XCUIElement.ElementType.button.rawValue, XCUIElement.ElementType.switch.rawValue)).firstMatch
    }

    func tapRow(_ prefix: String) {
        let target = row(prefix)
        guard target.waitForExistence(timeout: 5) else { XCTFail("못 찾음: \(prefix)"); shot("missing-\(prefix)"); return }
        var tries = 0
        while !target.isHittable, tries < 6 { app.swipeUp(); tries += 1 }
        target.tap()
    }

    /** 잠금 켜기·자동 잠금·틀린 비밀번호·풀기·끄기 */
    func testLock() {
        app.launch()
        sleep(2)
        tap("설정")
        sleep(1)
        tapRow("잠금,")
        sleep(1)
        shot("lock-settings")
        tapRow("잠금 사용")
        sleep(1)
        shot("pin-setup")
        pin("1234")
        shot("pin-again")
        pin("1234")
        sleep(1)
        shot("lock-on")
        tapRow("자동 잠금")
        sleep(1)
        shot("relock-options")
        tapIfExists("바로", timeout: 2)
        sleep(1)
        shot("relock-immediately")
        backgroundAndReturn()
        shot("locked")
        pin("9999")
        shot("wrong-pin")
        pin("1234")
        shot("unlocked")
        // 앱을 끄고 다시 켜도 잠김
        app.terminate()
        app.launch()
        sleep(2)
        shot("locked-after-relaunch")
        pin("1234")
        shot("unlocked-after-relaunch")
        tap("설정")
        tapRow("잠금,")
        sleep(1)
        tapRow("잠금 사용")
        sleep(1)
        shot("lock-off-confirm")
        pin("1234")
        shot("lock-off")
        backgroundAndReturn()
        shot("not-locked-after-off")
    }

    /** 사진 고르기 창은 다른 프로세스라 앱 안과 시스템 둘 다에서 찾음 */
    func any(_ predicate: NSPredicate) -> XCUIElement {
        let inApp = app.descendants(matching: .any).matching(predicate).firstMatch
        if inApp.exists { return inApp }
        return springboard.descendants(matching: .any).matching(predicate).firstMatch
    }

    /** 사진 고르기·카메라·사진 삭제와 되돌리기·사진 있는 일기 저장 */
    func testPhotos() {
        app.launch()
        sleep(2)
        tap("일기 쓰기")
        sleep(1)
        for label in ["새로 쓰기"] { tapIfExists(label, timeout: 2) }
        typeBody("Photo test entry.")
        // 사진 고르기
        tap("사진")
        sleep(3)
        handleSystemAlert()
        shot("picker")
        let images = app.descendants(matching: .image).allElementsBoundByIndex.filter { $0.isHittable }
        let picked = images.prefix(2)
        if picked.isEmpty {
            // 고르기 창 안 첫 칸을 화면 위치로 누름
            app.coordinate(withNormalizedOffset: CGVector(dx: 0.17, dy: 0.32)).tap()
            sleep(1)
            app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.32)).tap()
        } else {
            picked.forEach { $0.tap(); sleep(1) }
        }
        sleep(1)
        shot("picker-selected")
        for label in ["추가", "Add", "완료", "Done"] {
            let done = any(NSPredicate(format: "label == %@", label))
            if done.exists { done.tap(); break }
        }
        sleep(3)
        shot("photos-added")
        // 사진 삭제와 되돌리기
        if tapIfFound("사진 삭제") {
            sleep(1)
            shot("photo-removed")
            tapIfExists("되돌리기", timeout: 3)
            sleep(1)
            shot("photo-undo")
        }
        // 카메라
        tap("카메라")
        sleep(2)
        handleCameraAlert()
        sleep(2)
        shot("camera")
        let shutter = any(NSPredicate(format: "label IN %@", ["Take Picture", "사진 찍기", "촬영", "셔터", "PhotoCapture"]))
        if shutter.waitForExistence(timeout: 5) {
            shutter.tap()
            sleep(3)
            shot("camera-taken")
            let use = any(NSPredicate(format: "label IN %@", ["Use Photo", "사진 사용", "사진 사용하기"]))
            if use.waitForExistence(timeout: 5) { use.tap() }
            sleep(3)
        } else {
            XCTFail("셔터 없음")
            let cancel = any(NSPredicate(format: "label IN %@", ["Cancel", "취소"]))
            if cancel.exists { cancel.tap() }
        }
        shot("after-camera")
        tap("저장")
        sleep(2)
        shot("detail-with-photos")
        // 상세에서 사진 크게 보기
        let photo = app.images.allElementsBoundByIndex.first { $0.isHittable && $0.frame.height > 80 }
        if let photo {
            photo.tap()
            sleep(2)
            shot("photo-viewer")
            tapIfExists("닫기", timeout: 3)
        }
        tap("뒤로 가기")
        sleep(2)
        shot("home-with-photo")
    }

    func tapIfFound(_ label: String) -> Bool {
        let target = element(label)
        guard target.waitForExistence(timeout: 3) else { return false }
        target.tap()
        return true
    }

    /** 카메라 권한 창은 허용(앱 기능 시험) */
    func handleCameraAlert() {
        let alert = springboard.alerts.firstMatch
        guard alert.waitForExistence(timeout: 3) else { return }
        shot("camera-permission")
        for label in ["허용", "Allow", "확인", "OK"] where alert.buttons[label].exists {
            alert.buttons[label].tap()
            return
        }
    }
}

