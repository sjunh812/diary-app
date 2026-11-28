import XCTest

/**
 * 스토어 스크린샷용 캡처. 견본 일기를 넣은 시뮬레이터에서 scripts/store/store_shots.sh로 돌림
 * 언어는 /tmp/diary-store-lang.txt, iPad 가로는 /tmp/diary-store-land 파일로 받음(시험 실행에 환경 변수가 넘어오지 않음)
 */
final class StoreShots: DeviceTestCase {
    lazy var lang = (try? String(contentsOfFile: "/tmp/diary-store-lang.txt", encoding: .utf8))?.trimmingCharacters(in: .whitespacesAndNewlines) ?? "ko"
    let words: [String: [String: String]] = [
        "ko": ["lock": "잠금", "lockOn": "잠금 사용", "bubble": "비눗방울", "listStyle": "목록 보기", "photoGrid": "사진 격자", "feed": "피드", "settings": "설정", "moodTheme": "기분 테마", "calendar": "달력", "home": "홈", "insight": "돌아보기", "backup": "백업과 복원", "write": "일기 쓰기", "mood": "기분", "excited": "설렘", "theme": "테마", "rose": "로즈", "lavender": "라벤더", "custom": "꾸미기", "close": "닫기", "back": "뒤로 가기", "skip": "건너뛰기", "later": "나중에", "font": "글꼴", "fontPick": "창원단감둥근체",
               "basic": "기본", "shape": "도형", "clay": "클레이 얼굴", "veggie": "채소·과일", "gouache": "과슈 붓", "doodle": "말랑 낙서 친구", "type": "퇴근길에 본 노을이 예뻤다. 오늘은 일찍 자야지."],
        "en": ["lock": "Lock", "lockOn": "Lock diary", "bubble": "Bubble", "listStyle": "List view", "photoGrid": "Photo grid", "feed": "Feed", "settings": "Settings", "moodTheme": "Mood theme", "calendar": "Calendar", "home": "Home", "insight": "Insights", "backup": "Backup and restore", "write": "New entry", "mood": "Mood", "excited": "Excited", "theme": "Theme", "rose": "Rose", "lavender": "Lavender", "custom": "Customize", "close": "Close", "back": "Back", "skip": "Skip", "later": "Later", "font": "Font", "fontPick": "Changwon Dangam Round",
               "basic": "Classic", "shape": "Shapes", "clay": "Clay Faces", "veggie": "Fruit & Veg", "gouache": "Gouache", "doodle": "Doodle Pals", "type": "The sunset on my way home was lovely. Early night tonight."],
        "ja": ["lock": "ロック", "lockOn": "日記をロック", "bubble": "シャボン玉", "listStyle": "一覧の表示", "photoGrid": "写真グリッド", "feed": "フィード", "settings": "設定", "moodTheme": "気分テーマ", "calendar": "カレンダー", "home": "ホーム", "insight": "ふりかえり", "backup": "バックアップと復元", "write": "日記を書く", "mood": "気分", "excited": "ワクワク", "theme": "テーマ", "rose": "ローズ", "lavender": "ラベンダー", "custom": "カスタマイズ", "close": "閉じる", "back": "戻る", "skip": "スキップ", "later": "あとで", "font": "フォント", "fontPick": "システムフォント",
               "basic": "ベーシック", "shape": "かたち", "clay": "ねんど", "veggie": "やさい・くだもの", "gouache": "ガッシュ", "doodle": "らくがき", "type": "帰り道の夕焼けがきれいだった。今日は早く寝よう。"],
    ]
    func w(_ key: String) -> String { words[lang]![key]! }

    func at(_ x: CGFloat, _ y: CGFloat) {
        app.coordinate(withNormalizedOffset: CGVector(dx: 0, dy: 0)).withOffset(CGVector(dx: x, dy: y)).tap()
    }

    func pickTheme(_ key: String) {
        tap(w("settings")); sleep(1)
        tap(w("moodTheme")); sleep(1)
        tap(w(key)); sleep(1)
    }

    func testCapture() {
        app.launchArguments = ["-diaryPlan", "PLUS"]
        app.launch()
        sleep(3)
        for _ in 0..<3 {
            let alert = springboard.alerts.firstMatch
            if alert.waitForExistence(timeout: 2) { alert.buttons.element(boundBy: 0).tap() }
        }
        tapIfExists(w("skip"), timeout: 3); sleep(2)
        tapIfExists(w("later"), timeout: 2)
        // 글꼴
        tap(w("settings")); sleep(1)
        tap(w("font")); sleep(1)
        tapIfExists(w("fontPick"), timeout: 3); sleep(2)
        tapIfExists(w("back"), timeout: 2); sleep(1)
        pickTheme("basic"); tapIfExists(w("back"), timeout: 2); sleep(1)
        tap(w("home")); sleep(1)
        tapIfExists(w("close"), timeout: 2); sleep(1)
        shot("home")
        tap(w("calendar")); sleep(2)
        at(48, 284); sleep(2)
        shot("calendar")
        at(200, 640); sleep(2)
        shot("detail")
        tapIfExists(w("back"), timeout: 2); sleep(1)
        tap(w("insight")); sleep(2)
        shot("insight")
        tap(w("settings")); sleep(1)
        tap(w("backup")); sleep(2)
        shot("backup")
        tapIfExists(w("back"), timeout: 2); sleep(1)
        tap(w("theme")); sleep(1)
        tapIfExists(w("rose"), timeout: 2); sleep(2)
        shot("theme-rose")
        tapIfExists(w("lavender"), timeout: 2); sleep(1)
        tapIfExists(w("back"), timeout: 2); sleep(1)
        for key in ["shape", "clay", "veggie", "gouache", "doodle"] {
            pickTheme(key)
            if key == "clay" {
                shot("theme-list")
                tapContains(w("custom")); sleep(2)
                shot("clay-custom")
                tapIfExists(w("back"), timeout: 2); sleep(1)
            }
            tapIfExists(w("back"), timeout: 2); sleep(1)
            tap(w("calendar")); sleep(2)
            at(48, 284); sleep(2)
            shot("cal-\(key)")
        }
        pickTheme("basic"); tapIfExists(w("back"), timeout: 2); sleep(1)
        tap(w("home")); sleep(1)
        tap(w("write")); sleep(2)
        app.typeText(w("type")); sleep(1)
        tap(w("mood")); sleep(2)
        shot("mood-sheet")
    }

    func testHome() {
        app.launchArguments = ["-diaryPlan", "PLUS"]
        app.launch()
        sleep(3)
        tapIfExists(w("later"), timeout: 2)
        tap(w("settings")); sleep(1)
        tap(w("font")); sleep(1)
        tapIfExists(w("fontPick"), timeout: 3); sleep(2)
        tapIfExists(w("back"), timeout: 2); sleep(1)
        tap(w("home")); sleep(1)
        tapIfExists(w("close"), timeout: 2); sleep(1)
        shot("home")
    }

    func keys(_ digits: String) {
        for d in digits { tap(String(d)); usleep(400_000) }
    }

    func testExtra() {
        app.launchArguments = ["-diaryPlan", "PLUS"]
        app.launch()
        sleep(3)
        tapIfExists(w("later"), timeout: 2)
        tap(w("settings")); sleep(1)
        tap(w("font")); sleep(1)
        tapIfExists(w("fontPick"), timeout: 3); sleep(2)
        tapIfExists(w("back"), timeout: 2); sleep(1)
        pickTheme("bubble"); tapIfExists(w("back"), timeout: 2); sleep(1)
        tap(w("calendar")); sleep(2)
        at(48, 284); sleep(2)
        shot("cal-bubble")
        pickTheme("basic"); tapIfExists(w("back"), timeout: 2); sleep(1)
        tap(w("listStyle")); sleep(1)
        tap(w("photoGrid")); sleep(1)
        tapIfExists(w("back"), timeout: 2); sleep(1)
        tap(w("home")); sleep(2)
        tapIfExists(w("close"), timeout: 2); sleep(1)
        shot("home-grid")
        tap(w("settings")); sleep(1)
        tap(w("listStyle")); sleep(1)
        tap(w("feed")); sleep(1)
        tapIfExists(w("back"), timeout: 2); sleep(1)
        app.terminate()
        testLock()
    }

    func testLock() {
        app.launchArguments = ["-diaryPlan", "PLUS"]
        app.launch()
        sleep(3)
        tapIfExists(w("later"), timeout: 2)
        tap(w("settings")); sleep(1)
        // 잠금 켜기
        tap(w("lock")); sleep(1)
        tap(w("lockOn")); sleep(2)
        keys("1234"); sleep(2)
        keys("1234"); sleep(2)
        shot("lock-on-settings")
        tapIfExists(w("back"), timeout: 2); sleep(1)
        shot("settings-lock")
        app.terminate(); sleep(1)
        app.launch(); sleep(3)
        keys("12"); sleep(1)
        shot("lock-screen")
        keys("34"); sleep(2)
        // 잠금 끄기
        tap(w("settings")); sleep(1)
        tap(w("lock")); sleep(1)
        tap(w("lockOn")); sleep(2)
        keys("1234"); sleep(2)
        shot("lock-off")
    }

    lazy var landscape = FileManager.default.fileExists(atPath: "/tmp/diary-store-land")
    let sea = ["ko": "오랜만에 바다를", "en": "Drove out to the sea", "ja": "久しぶりに海を見に"]

    /** 화면 트리에서 라벨이 [text]를 품은 버튼 칸 가운데를 누름 */
    let day28 = ["ko": "11월 28일", "en": "November 28", "ja": "11月28日"]
    let place = ["ko": "한강공원", "en": "Riverside park", "ja": "川沿いの公園"]

    func frameInTree(_ text: String) -> [Double]? {
        for line in app.debugDescription.precomposedStringWithCanonicalMapping.replacingOccurrences(of: "\u{00A0}", with: " ").split(separator: "\n") where line.contains("Button") && line.contains(text.precomposedStringWithCanonicalMapping) {
            let l = String(line)
            guard let r = l.range(of: #"\{\{(-?[\d.]+), (-?[\d.]+)\}, \{([\d.]+), ([\d.]+)\}\}"#, options: .regularExpression) else { continue }
            let n = l[r].replacingOccurrences(of: "{", with: "").replacingOccurrences(of: "}", with: "").split(separator: ",").map { Double($0.trimmingCharacters(in: .whitespaces))! }
            if n[0] >= 0 { return n }
        }
        return nil
    }

    /** 11월 28일(토) 칸에서 왼쪽 6칸, 위 2줄 = 11월 8일(일) */
    func tapDay8() {
        if !landscape { at(156, 251); return }
        guard let f = frameInTree(day28[lang]!) else { XCTFail("no 28"); return }
        at(CGFloat(f[0] + f[2] / 2 - 6 * f[2]), CGFloat(f[1] + f[3] / 2 - 2 * (f[3] + 4)))
    }

    func tapByTree(_ text: String) {
        let tree = app.debugDescription.precomposedStringWithCanonicalMapping.replacingOccurrences(of: "\u{00A0}", with: " ")
        for line in tree.split(separator: "\n") where line.contains("Button") && line.contains("label: '") && line.contains(text.precomposedStringWithCanonicalMapping) {
            let l = String(line)
            guard let r = l.range(of: #"\{\{(-?[\d.]+), (-?[\d.]+)\}, \{([\d.]+), ([\d.]+)\}\}"#, options: .regularExpression) else { continue }
            let nums = l[r].replacingOccurrences(of: "{", with: "").replacingOccurrences(of: "}", with: "").split(separator: ",").map { Double($0.trimmingCharacters(in: .whitespaces))! }
            let x = nums[0] + nums[2] / 2, y = nums[1] + nums[3] / 2
            if x < 0 || y < 0 { continue }
            at(CGFloat(x), CGFloat(y)); return
        }
        XCTFail("tree miss \(text)")
    }

    func testIPad() {
        if landscape { XCUIDevice.shared.orientation = .landscapeLeft }
        app.launchArguments = ["-diaryPlan", "PLUS"]
        app.launch()
        sleep(3)
        for _ in 0..<3 {
            let alert = springboard.alerts.firstMatch
            if alert.waitForExistence(timeout: 2) { alert.buttons.element(boundBy: 0).tap() }
        }
        tapIfExists(w("skip"), timeout: 3); sleep(2)
        tapIfExists(w("later"), timeout: 2)
        tap(w("settings")); sleep(1)
        tap(w("font")); sleep(1)
        tapIfExists(w("fontPick"), timeout: 3); sleep(2)
        tapIfExists(w("back"), timeout: 2); sleep(1)
        pickTheme("basic"); tapIfExists(w("back"), timeout: 2); sleep(1)
        tap(w("home")); sleep(1)
        tapIfExists(w("close"), timeout: 2); sleep(1)
        tapByTree(place[lang]!); sleep(2)
        shot("home")
        tap(w("calendar")); sleep(2)
        tapDay8(); sleep(2)
                if landscape { tapByTree(sea[lang]!) } else { at(850, 150) }
        sleep(2)
        shot("detail")
        tap(w("calendar")); sleep(2)
        tapDay8(); sleep(2)
        shot("calendar")
        tap(w("insight")); sleep(2)
        shot("insight")
        tap(w("settings")); sleep(1)
        tap(w("backup")); sleep(2)
        shot("backup")
        tap(w("theme")); sleep(1)
        tapIfExists(w("rose"), timeout: 2); sleep(2)
        shot("theme-rose")
        tapIfExists(w("lavender"), timeout: 2); sleep(1)
        tap(w("moodTheme")); sleep(1)
        tap(w("clay")); sleep(1)
        shot("theme-list")
        tapContains(w("custom")); sleep(2)
        shot("clay-custom")
        tapIfExists(w("back"), timeout: 2); sleep(1)
        tap(w("basic")); sleep(1)
        tap(w("listStyle")); sleep(1)
        tap(w("photoGrid")); sleep(1)
        tap(w("home")); sleep(2)
        shot("home-grid")
        tap(w("settings")); sleep(1)
        tap(w("listStyle")); sleep(1)
        tap(w("feed")); sleep(1)
        tap(w("home")); sleep(1)
        tap(w("write")); sleep(2)
        app.typeText(w("type")); sleep(1)
        tap(w("mood")); sleep(2)
        shot("mood-sheet")
        app.terminate()
        testLock()
    }
}
