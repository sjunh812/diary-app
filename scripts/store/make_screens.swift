// 스토어 스크린샷과 피처 그래픽 생성
// 실제 앱 캡처 위에 머리말 배치
// 쓰는 법: swift scripts/store/make_screens.swift <캡처 폴더> <결과 폴더> <글꼴 폴더> [태블릿 캡처 폴더] [언어 ko·en·ja]
// 태블릿 폴더를 주면 2560×1600 가로 캡처(write, list, calendar, insight)로 태블릿 4장도 생성. "-"를 주면 건너뜀
// 영어·일본어는 백업 화면(드라이브 연결 필요)과 피처 그래픽 없이 4장씩 생성
// 여섯째 인자로 ios를 주면 App Store용 iPhone 1206×2622 세로와 iPad 2752×2064 가로를 4장씩 생성
// 여섯째 인자로 search를 주면 App Store 검색 결과 그림(3:2, 3840×2560) 한 장 생성
// 여섯째 인자로 mood·photo를 주면 맞춤형 제품 페이지 그림 생성. 캡처는 <캡처 폴더>/<mood|photo>
import AppKit
import CoreText

let args = CommandLine.arguments
_ = NSApplication.shared
let source = args[1], output = args[2], fonts = args[3]
let language = args.count > 5 ? args[5] : "ko"
let ios = args.count > 6 && args[6] == "ios"
let search = args.count > 6 && args[6] == "search"
/** Play 스크린샷·피처 그래픽·태블릿 */
/** 맞춤형 제품 페이지(기분 일기, 사진 일기) */
let page = args.count > 6 && ["mood", "photo"].contains(args[6]) ? args[6] : nil
let play = !ios && !search && page == nil
for name in ["changwon_round_regular.otf"] {
    CTFontManagerRegisterFontsForURL(URL(fileURLWithPath: "\(fonts)/\(name)") as CFURL, .process, nil)
}

func color(_ hex: UInt32) -> NSColor {
    NSColor(srgbRed: CGFloat((hex >> 16) & 255) / 255, green: CGFloat((hex >> 8) & 255) / 255, blue: CGFloat(hex & 255) / 255, alpha: 1)
}

let ink = color(0x1B1B23)
let weak = color(0x4A4A5C)
let accent = color(0x6C56CE)

/** 바탕은 앱 전용 색 하나(`selected`) */
let background = color(0xEBE4FF)

func canvas(_ width: Int, _ height: Int, _ draw: () -> Void) -> NSBitmapImageRep {
    // 스토어는 알파 없는 PNG만 받음
    let context = CGContext(
        data: nil, width: width, height: height, bitsPerComponent: 8, bytesPerRow: 0,
        space: CGColorSpace(name: CGColorSpace.sRGB)!, bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue
    )!
    NSGraphicsContext.current = NSGraphicsContext(cgContext: context, flipped: false)
    background.setFill()
    NSRect(x: 0, y: 0, width: width, height: height).fill()
    draw()
    NSGraphicsContext.current = nil
    return NSBitmapImageRep(cgImage: context.makeImage()!)
}

func styled(_ string: String, _ size: CGFloat, _ tint: NSColor, _ align: NSTextAlignment, accented: String? = nil) -> NSAttributedString {
    let style = NSMutableParagraphStyle()
    style.alignment = align
    style.lineHeightMultiple = 1.16
    // 앱 화면과 같은 글꼴 사용
    // 이 글꼴은 굵기가 하나뿐이라 윤곽선을 둘러 제목을 굵게 표시
    // 일본어는 이 글꼴에 가나와 문장부호가 없어 앱의 일본어 화면처럼 시스템 글꼴(히라기노) 사용(D13)
    let japanese = language == "ja"
    let font = (japanese ? NSFont(name: "HiraginoSans-W6", size: size) : NSFont(name: "ChangwonDangamRound-Regular", size: size))
        ?? NSFont.boldSystemFont(ofSize: size)
    let label = NSMutableAttributedString(
        string: string,
        attributes: [.font: font, .foregroundColor: tint, .strokeColor: tint, .strokeWidth: japanese ? 0 : (size < 40 ? -1.6 : -3.4), .paragraphStyle: style]
    )
    if let accented, let range = string.range(of: accented) {
        let span = NSRange(range, in: string)
        label.addAttributes([.foregroundColor: accent, .strokeColor: accent], range: span)
    }
    return label
}

func text(_ string: String, _ size: CGFloat, _ tint: NSColor, _ rect: NSRect, _ align: NSTextAlignment, accented: String? = nil) {
    styled(string, size, tint, align, accented: accented).draw(in: rect)
}

/** 여러 줄 문구 중 가장 긴 줄의 너비 */
func textWidth(_ string: String, _ size: CGFloat) -> CGFloat {
    styled(string, size, ink, .left).boundingRect(with: NSSize(width: 10000, height: 10000), options: .usesLineFragmentOrigin).width.rounded(.up)
}

/** 캡처 위쪽 상태 표시줄 높이. 폰 56, 태블릿 48 */
var statusBar: CGFloat = 56
var shotFolder = source

func shot(_ name: String) -> NSImage { NSImage(contentsOfFile: "\(shotFolder)/\(name).png")! }

func shadowed(_ alpha: CGFloat, _ blur: CGFloat, _ body: () -> Void) {
    let shadow = NSShadow()
    shadow.shadowColor = NSColor(white: 0.1, alpha: alpha)
    shadow.shadowBlurRadius = blur
    shadow.shadowOffset = NSSize(width: 0, height: -blur / 3)
    NSGraphicsContext.saveGraphicsState()
    shadow.set()
    body()
    NSGraphicsContext.restoreGraphicsState()
}

/** 기기 틀 안의 실제 앱 화면. 아래쪽은 화면 밖으로 잘림 */
func phone(_ name: String, _ rect: NSRect, _ radius: CGFloat, bezel: Bool = true) {
    let frame = bezel ? rect.insetBy(dx: -14, dy: -14) : rect
    let outer = radius + (bezel ? 14 : 0)
    shadowed(0.08, 40) {
        (bezel ? ink : NSColor.white).setFill()
        NSBezierPath(roundedRect: frame, xRadius: outer, yRadius: outer).fill()
    }
    NSGraphicsContext.saveGraphicsState()
    NSBezierPath(roundedRect: rect, xRadius: radius, yRadius: radius).addClip()
    let image = shot(name)
    let scale = rect.width / image.size.width
    let height = image.size.height * scale
    // 상태 표시줄은 잘라 냄
    image.draw(in: NSRect(x: rect.minX, y: rect.maxY - height + statusBar * scale, width: rect.width, height: height))
    NSGraphicsContext.restoreGraphicsState()
}

func save(_ rep: NSBitmapImageRep, _ name: String) {
    try! rep.representation(using: .png, properties: [:])!.write(to: URL(fileURLWithPath: "\(output)/\(name).png"))
}

// 기준: 심플하고 미감 있고 잘 읽힐 것
// 앱 색 바탕, 큰 제목, 곧게 놓은 실제 화면만 배치
// 장식과 기울임 없이 강조는 낱말 하나만
let phoneSlides: [String: [(String, String, String)]] = [
    "ko": [
        ("write", "오늘 기분부터\n가볍게 골라 봐요", "기분"),
        ("list", "지난 일기가\n한눈에 보여요", "한눈에"),
        ("calendar", "한 달 기분을\n달력으로 봐요", "달력"),
        ("insight", "내 기분 흐름을\n돌아봐요", "돌아봐요"),
        ("backup", "사진까지\n드라이브에 백업해요", "백업")
    ],
    // 영어·일본어 머리말은 원어민 검수 전
    "en": [
        ("write", "Start with\ntoday's mood", "mood"),
        ("list", "Past entries\nat a glance", "at a glance"),
        ("calendar", "Your month's moods\non a calendar", "calendar"),
        ("insight", "Look back on\nyour moods", "Look back")
    ],
    "ja": [
        ("write", "今日の気分から\n気軽に選べます", "気分"),
        ("list", "過去の日記が\nひと目でわかる", "ひと目"),
        ("calendar", "1か月の気分を\nカレンダーで", "カレンダー"),
        ("insight", "気分の流れを\nふりかえる", "ふりかえる")
    ]
]
for (index, (name, words, word)) in phoneSlides[language]!.enumerated() where play {
    let rep = canvas(1080, 1920) {
        text(words, language == "ja" ? 80 : 88, ink, NSRect(x: 40, y: 1500, width: 1000, height: 300), .center, accented: word)
        // 첫 장은 아래쪽 기분 시트까지 보이게 화면 전체 사용
        if name == "write" {
            phone(name, NSRect(x: 215, y: 40, width: 650, height: 1400), 56)
        } else {
            phone(name, NSRect(x: 130, y: -560, width: 820, height: 1960), 64)
        }
    }
    save(rep, "phone-\(index + 1)")
}

if language == "ko", play {
let feature = canvas(1024, 500) {
    text("바쁜 날에도\n일기를 남겨 봐요", 60, ink, NSRect(x: 76, y: 176, width: 500, height: 170), .left, accented: "일기")
    text("회원가입 없이, 사진까지 내 드라이브에", 25, weak, NSRect(x: 76, y: 118, width: 500, height: 44), .left)
    // 피처 그래픽에는 기기 틀을 넣지 않음(Play 안내)
    phone("list", NSRect(x: 640, y: -640, width: 300, height: 1040), 26, bezel: false)
}
save(feature, "feature")
}

// 태블릿: 가로 2560×1600, 머리말은 위에 한 줄, 화면은 아래쪽이 잘림
// Play 7"·10" 태블릿 칸에 같은 이미지 사용. 백업 화면은 드라이브 연결이 필요해 제외
if play, args.count > 4, args[4] != "-" {
    shotFolder = args[4]
    statusBar = 48
    let tabletSlides: [String: [(String, String, String)]] = [
        "ko": [
            ("write", "오늘 기분부터 가볍게 골라 봐요", "기분"),
            ("list", "목록과 일기를 나란히 봐요", "나란히"),
            ("calendar", "한 달 기분을 달력으로 봐요", "달력"),
            ("insight", "내 기분 흐름을 돌아봐요", "돌아봐요")
        ],
        "en": [
            ("write", "Start with today's mood", "mood"),
            ("list", "Your list and entry, side by side", "side by side"),
            ("calendar", "Your month's moods on a calendar", "calendar"),
            ("insight", "Look back on your moods", "Look back")
        ],
        "ja": [
            ("write", "今日の気分から気軽に選べます", "気分"),
            ("list", "一覧と日記を並べて見られます", "並べて"),
            ("calendar", "1か月の気分をカレンダーで", "カレンダー"),
            ("insight", "気分の流れをふりかえる", "ふりかえる")
        ]
    ]
    for (index, (name, words, word)) in tabletSlides[language]!.enumerated() {
        let rep = canvas(2560, 1600) {
            text(words, language == "ja" ? 88 : 96, ink, NSRect(x: 80, y: 1270, width: 2400, height: 160), .center, accented: word)
            // 화면 높이는 캡처 비율에 맞추고 아래 제스처 막대가 잘리게 내림
            phone(name, NSRect(x: 280, y: -70, width: 2000, height: 1212), 48)
        }
        save(rep, "tablet-\(index + 1)")
    }
}

// App Store: iPhone 1206×2622(6.3", 필수 규격)과 iPad 13" 가로 2752×2064
// 백업 화면은 드라이브 연결이 필요해 세 언어 모두 4장
if ios {
    statusBar = 150
    for (index, (name, words, word)) in phoneSlides[language]!.prefix(4).enumerated() {
        let rep = canvas(1206, 2622) {
            text(words, language == "ja" ? 92 : 100, ink, NSRect(x: 40, y: 2080, width: 1126, height: 360), .center, accented: word)
            // 잘리면 탭 이름이 반만 보여 아래 탭 바까지 전체 사용
            phone(name, NSRect(x: 113, y: 24, width: 980, height: 2008), 80)
        }
        save(rep, "iphone-\(index + 1)")
    }
    if args.count > 4, args[4] != "-" {
        shotFolder = args[4]
        statusBar = 48
        let tabletSlides: [String: [(String, String, String)]] = [
            "ko": [
                ("write", "오늘 기분부터 가볍게 골라 봐요", "기분"),
                ("list", "목록과 일기를 나란히 봐요", "나란히"),
                ("calendar", "한 달 기분을 달력으로 봐요", "달력"),
                ("insight", "내 기분 흐름을 돌아봐요", "돌아봐요")
            ],
            "en": [
                ("write", "Start with today's mood", "mood"),
                ("list", "Your list and entry, side by side", "side by side"),
                ("calendar", "Your month's moods on a calendar", "calendar"),
                ("insight", "Look back on your moods", "Look back")
            ],
            "ja": [
                ("write", "今日の気分から気軽に選べます", "気分"),
                ("list", "一覧と日記を並べて見られます", "並べて"),
                ("calendar", "1か月の気分をカレンダーで", "カレンダー"),
                ("insight", "気分の流れをふりかえる", "ふりかえる")
            ]
        ]
        for (index, (name, words, word)) in tabletSlides[language]!.enumerated() {
            let rep = canvas(2752, 2064) {
                text(words, language == "ja" ? 96 : 104, ink, NSRect(x: 80, y: 1700, width: 2592, height: 180), .center, accented: word)
                // 아래 가장자리는 화면 밖으로 내려 잘리게 둠
                phone(name, NSRect(x: 276, y: -90, width: 2200, height: 1700), 56)
            }
            save(rep, "ipad-\(index + 1)")
        }
    }
}

// 맞춤형 제품 페이지: 검색어에 맞는 화면만 골라 머리말을 새로 씀
// 검색 결과에 처음 세 장이 보여 가장 잘 보여 주는 화면을 앞에 둠
let pageSlides: [String: [String: [(String, String, String)]]] = [
    "mood": [
        "ko": [
            ("year", "한 해 기분이\n한눈에 보여요", "한 해"),
            ("calendar", "그날 기분이\n달력에 남아요", "달력"),
            ("month", "자주 고른 기분을\n돌아봐요", "돌아봐요"),
            ("write", "아홉 가지 기분 중\n하나를 골라요", "아홉 가지")
        ],
        "en": [
            ("year", "A year of moods\nat a glance", "A year"),
            ("calendar", "Every day's mood\non your calendar", "calendar"),
            ("month", "See which moods\ncame up most", "most"),
            ("write", "Pick one of\nnine moods", "nine moods")
        ],
        "ja": [
            ("year", "1年の気分が\nひと目でわかる", "1年"),
            ("calendar", "その日の気分が\nカレンダーに残る", "カレンダー"),
            ("month", "よく選んだ気分を\nふりかえる", "ふりかえる"),
            ("write", "9つの気分から\nひとつ選ぶ", "9つ")
        ]
    ],
    "photo": [
        "ko": [
            ("grid", "사진으로\n지난 일기를 봐요", "사진"),
            ("detail", "한 편에 사진을\n여러 장 담아요", "여러 장"),
            ("backup", "사진까지\n드라이브에 백업해요", "백업")
        ],
        "en": [
            ("grid", "Look back\nthrough photos", "photos"),
            ("detail", "Add several photos\nto one entry", "several photos"),
            ("backup", "Back up photos\nto your Drive", "Back up")
        ],
        "ja": [
            ("grid", "写真で\n日記をふりかえる", "写真"),
            ("detail", "1つの日記に\n写真を何枚も", "何枚も"),
            ("backup", "写真まで\nドライブにバックアップ", "バックアップ")
        ]
    ]
]
if let page {
    let slides = pageSlides[page]![language]!
    shotFolder = "\(source)/\(page)"
    statusBar = 150
    for (index, (name, words, word)) in slides.enumerated() {
        let rep = canvas(1206, 2622) {
            text(words, language == "ja" ? 92 : 100, ink, NSRect(x: 40, y: 2080, width: 1126, height: 360), .center, accented: word)
            phone(name, NSRect(x: 113, y: 24, width: 980, height: 2008), 80)
        }
        save(rep, "iphone-\(index + 1)")
    }
    // 태블릿은 머리말을 한 줄로
    // 태블릿 쓰기 화면은 기분이 옆 칸에 작게 놓여 빈 화면처럼 보여 뺌
    if args.count > 4, args[4] != "-" {
        shotFolder = "\(args[4])/\(page)"
        statusBar = 48
        for (index, (name, words, word)) in slides.filter({ $0.0 != "write" }).enumerated() {
            let line = words.replacingOccurrences(of: "\n", with: language == "ja" ? "" : " ")
            let rep = canvas(2752, 2064) {
                text(line, language == "ja" ? 96 : 104, ink, NSRect(x: 80, y: 1700, width: 2592, height: 180), .center, accented: word)
                phone(name, NSRect(x: 276, y: -90, width: 2200, height: 1700), 56)
            }
            save(rep, "ipad-\(index + 1)")
        }
    }
}

// App Store 검색 결과: 3:2 3840×2560, 짧은 문구와 실제 목록 화면(Apple 예시와 같은 배치)
// 문구와 화면 위쪽은 안전 영역(2168×1030, x 836, y 765) 안, 기기 아래쪽은 그림 밖으로 잘림
// 문구는 홍보 문구 첫 줄(docs/store/listing.md)
if search {
    statusBar = 150
    let lines: [String: (String, String)] = [
        "ko": ("하루 한 줄이면\n충분해요", "한 줄"),
        "en": ("One line a day\nis enough", "One line"),
        "ja": ("1日1行で\n十分です", "1行")
    ]
    let (words, word) = lines[language]!
    let rep = canvas(3840, 2560) {
        // 검색 카드는 작게 보여 문구를 크게
        // 문구와 화면 사이를 언어와 상관없이 같게 두고, 둘을 한 덩어리로 가운데에
        let gap: CGFloat = 200, phoneWidth: CGFloat = 840
        // 안전 영역 너비(2168)를 넘는 언어만 문구를 줄임
        let base: CGFloat = language == "ja" ? 176 : 184
        let room = 2168 - gap - phoneWidth
        let size = min(base, (base * room / textWidth(words, base)).rounded(.down))
        let wordsWidth = textWidth(words, size)
        let start = ((3840 - (wordsWidth + gap + phoneWidth)) / 2).rounded()
        text(words, size, ink, NSRect(x: start, y: 1090, width: wordsWidth + 10, height: 520), .left, accented: word)
        phone("list", NSRect(x: start + wordsWidth + gap, y: -20, width: phoneWidth, height: 1740), 88)
    }
    save(rep, "search")
}
