// 스토어 폰 스크린샷: 연한 라벤더 바탕, 두 줄 제목과 설명 한 줄, 상태바를 자른 앱 화면
// 쓰는 법: swift scripts/store/make_store_screens.swift <캡처 폴더> <결과 폴더> <ko|en|ja|zh-Hant|es|pt|de|fr> <폭> <높이> <파일 앞이름> [tablet|landscape]
// 캡처 폴더에는 home, detail, calendar, insight, theme-rose, backup.png가 있어야 함
// 폰은 iPhone 17 Pro(1206×2622), tablet은 iPad 13" 세로(2064×2752), landscape는 iPad 13" 가로(2752×2064) 캡처
import AppKit
import CoreText

let args = CommandLine.arguments
_ = NSApplication.shared
let caps = args[1], out = args[2], lang = args[3]
let W = CGFloat(Double(args[4])!), H = CGFloat(Double(args[5])!), prefix = args[6]
let landscape = args.count > 7 && args[7] == "landscape"
let tablet = landscape || (args.count > 7 && args[7] == "tablet")
let root = URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent().deletingLastPathComponent()
let fontFile = root.appendingPathComponent("shared/core/designsystem/src/commonMain/composeResources/font/changwon_round_regular.otf")
CTFontManagerRegisterFontsForURL(fontFile as CFURL, .process, nil)

// 폭 1206 기준 값을 캔버스 폭에 맞춤. 태블릿은 제목을 폰보다 덜 키움
// 가로 캔버스는 높이가 짧아 높이를 기준으로 맞춤
let k = landscape ? H / 1500 : (tablet ? W / 1650 : W / 1206)

func color(_ hex: UInt32, _ a: CGFloat = 1) -> NSColor {
    NSColor(srgbRed: CGFloat((hex >> 16) & 255) / 255, green: CGFloat((hex >> 8) & 255) / 255, blue: CGFloat(hex & 255) / 255, alpha: a)
}
let bg = color(0xF5F2FF), ink = color(0x1A1C20), sub = color(0x6E6A80), line = color(0xE4DEF7)
// 상태바: iPhone 54pt × 3, iPad 24pt × 2에 여유
let statusCut: CGFloat = tablet ? 64 : 162

let copy: [String: [(String, String, String)]] = [
    "ko": [
        ("home", "한 줄이면\n오늘 일기 끝", "바쁜 날에도 쓰기 쉬운 일기장"),
        ("detail", "사진과 함께\n남겨요", "한 편에 사진 10장까지 넣을 수 있어요"),
        ("calendar", "그날의 기분이\n달력에 남아요", "한 달 기분을 한눈에 봐요"),
        ("insight", "어떤 기분이 많았는지\n돌아봐요", "기간별로 볼 수 있어요"),
        ("theme-rose", "좋아하는 색으로\n바꿔 보세요", "테마 색 7가지 중에서 골라요"),
        ("backup", "사진까지\n내 드라이브에 백업해요", "회원가입 없이 하루 한 번 자동으로 올려요"),
    ],
    "en": [
        ("home", "One line,\nand today is done", "A diary that's easy even on busy days"),
        ("detail", "Keep your day\nwith photos", "Up to 10 photos in each entry"),
        ("calendar", "Your moods\non a calendar", "See your whole month at a glance"),
        ("insight", "Look back on\nyour moods", "See which moods came up most"),
        ("theme-rose", "Make it\nyour color", "Pick from 7 theme colors"),
        ("backup", "Back up photos\nto your own Drive", "No sign-up. Automatic, once a day."),
    ],
    "ja": [
        ("home", "1行で\n今日の日記は完了", "忙しい日でも書きやすい日記帳"),
        ("detail", "写真と一緒に\n残せます", "1つの日記に写真10枚まで"),
        ("calendar", "その日の気分が\nカレンダーに", "1か月の気分がひと目でわかります"),
        ("insight", "どんな気分が多かったか\nふり返れます", "期間ごとに見られます"),
        ("theme-rose", "好きな色に\n変えられます", "7つのテーマカラーから選べます"),
        ("backup", "写真まで\n自分のドライブに", "会員登録なしで、1日1回自動でバックアップ"),
    ],
    "zh-Hant": [
        ("home", "一行\n就寫完今天的日記", "忙碌的日子也能輕鬆寫"),
        ("detail", "和照片一起\n留下今天", "每篇最多10張照片"),
        ("calendar", "每天的心情\n留在月曆上", "一眼看完整個月的心情"),
        ("insight", "回顧哪種心情\n最多", "可以依期間查看"),
        ("theme-rose", "換成\n喜歡的顏色", "從7種主題色中挑選"),
        ("backup", "連照片都備份到\n自己的雲端硬碟", "免註冊，每天自動備份一次"),
    ],
    "es": [
        ("home", "Una línea\ny listo por hoy", "Un diario fácil incluso en días ocupados"),
        ("detail", "Guarda tu día\ncon fotos", "Hasta 10 fotos en cada entrada"),
        ("calendar", "Tu ánimo\nen el calendario", "Todo el mes de un vistazo"),
        ("insight", "Repasa\ncómo te sentiste", "Descubre tus ánimos más frecuentes"),
        ("theme-rose", "Ponle\ntu color", "Elige entre 7 colores de tema"),
        ("backup", "Respalda tus fotos\nen tu propio Drive", "Sin registro. Automático, una vez al día."),
    ],
    "pt": [
        ("home", "Uma linha\ne pronto por hoje", "Um diário fácil até nos dias corridos"),
        ("detail", "Guarde seu dia\ncom fotos", "Até 10 fotos em cada registro"),
        ("calendar", "Seu humor\nno calendário", "O mês inteiro de relance"),
        ("insight", "Reveja\nseu humor", "Veja qual humor apareceu mais"),
        ("theme-rose", "Deixe com\na sua cor", "Escolha entre 7 cores de tema"),
        ("backup", "Backup das fotos\nno seu próprio Drive", "Sem cadastro. Automático, uma vez por dia."),
    ],
    "de": [
        ("home", "Eine Zeile,\nund der Tag ist notiert", "Leicht zu schreiben, auch an vollen Tagen"),
        ("detail", "Halte deinen Tag\nmit Fotos fest", "Bis zu 10 Fotos pro Eintrag"),
        ("calendar", "Deine Stimmung\nim Kalender", "Den ganzen Monat auf einen Blick"),
        ("insight", "Blick zurück\nauf deine Stimmung", "Sieh, welche Stimmung am häufigsten war"),
        ("theme-rose", "Mach es\nin deiner Farbe", "Wähle aus 7 Designfarben"),
        ("backup", "Fotos sichern\nin deinem Drive", "Ohne Konto. Automatisch, einmal am Tag."),
    ],
    "fr": [
        ("home", "Une ligne,\net la journée est écrite", "Un journal simple, même les jours chargés"),
        ("detail", "Gardez votre journée\nen photos", "Jusqu’à 10 photos par entrée"),
        ("calendar", "Votre humeur\nsur le calendrier", "Tout le mois en un coup d’œil"),
        ("insight", "Revoyez\nvos humeurs", "Voyez celles qui reviennent le plus"),
        ("theme-rose", "À vos\ncouleurs", "Choisissez parmi 7 couleurs de thème"),
        ("backup", "Sauvegardez vos photos\ndans votre Drive", "Sans inscription. Automatique, une fois par jour."),
    ],
]

func R(_ x: CGFloat, _ top: CGFloat, _ w: CGFloat, _ h: CGFloat) -> NSRect { NSRect(x: x, y: H - top - h, width: w, height: h) }

// 창원단감둥근체에 가나·한자·악센트 글자가 없어 일본어·중국어는 시스템 글꼴, 유럽 언어는 앱 기본 글꼴(Pretendard)
let usesSystemFont = ["ja", "zh-Hant", "es", "pt", "de", "fr"].contains(lang)
for file in ["pretendard_regular.otf", "pretendard_semibold.otf"] {
    CTFontManagerRegisterFontsForURL(root.appendingPathComponent("shared/core/designsystem/src/commonMain/composeResources/font/\(file)") as CFURL, .process, nil)
}
func systemFont(bold: Bool, size: CGFloat) -> NSFont {
    switch lang {
    case "ja": return NSFont(name: bold ? "HiraginoSans-W6" : "HiraginoSans-W3", size: size * 0.92)!
    case "zh-Hant": return NSFont(name: bold ? "PingFangTC-Semibold" : "PingFangTC-Light", size: size * 0.92)!
    case "es", "pt", "de", "fr": return NSFont(name: bold ? "Pretendard-SemiBold" : "Pretendard-Regular", size: size * 0.92)!
    default: return NSFont(name: "ChangwonDangamRound-Regular", size: size)!
    }
}

// 창원단감둥근체는 굵기가 하나라 획을 덧그려 굵게. 일본어는 시스템 글꼴(히라기노)
func text(_ s: String, size: CGFloat, color c: NSColor, top: CGFloat, bold: Bool) -> CGFloat {
    let style = NSMutableParagraphStyle(); style.alignment = .center; style.lineHeightMultiple = 1.18
    let f = systemFont(bold: bold, size: size)
    // 라틴 문자는 같은 획에도 가늘어 보여 더 굵게
    let stroke: CGFloat = usesSystemFont ? 0 : (bold ? (lang == "ko" ? -3.4 : -5.0) : -0.6)
    let a = NSAttributedString(string: s, attributes: [.font: f, .foregroundColor: c, .strokeColor: c, .strokeWidth: stroke, .paragraphStyle: style])
    let h = a.boundingRect(with: NSSize(width: W - 120 * k, height: 2000), options: [.usesLineFragmentOrigin]).height
    a.draw(with: R(60 * k, top, W - 120 * k, h + 10), options: [.usesLineFragmentOrigin])
    return top + h
}

func header(_ title: String, _ subline: String) -> CGFloat {
    // 가로는 제목을 한 줄로 두어 화면 자리를 넓힘. 일본어는 띄어 쓰지 않아 붙임
    let t = text(landscape ? title.replacingOccurrences(of: "\n", with: ["ja", "zh-Hant"].contains(lang) ? "" : " ") : title, size: 92 * k, color: ink, top: (landscape ? 110 : 190) * k, bold: true)
    return text(subline, size: 46 * k, color: sub, top: t + 34 * k, bold: false)
}

func shadow(_ blur: CGFloat, _ alpha: CGFloat, _ dy: CGFloat) -> NSShadow {
    let s = NSShadow(); s.shadowColor = color(0x2A1F66, alpha); s.shadowBlurRadius = blur; s.shadowOffset = NSSize(width: 0, height: -dy); return s
}

// 얇고 밝은 기기 틀. 아래는 캔버스 밖으로 잘림
func phone(_ name: String, top: CGFloat) {
    let img = NSImage(contentsOfFile: "\(caps)/\(name).png")!
    // 세로가 긴 Play 태블릿 칸은 기기를 더 크게 두어 아래가 잘리게
    let width = landscape ? W * 0.74 : (tablet ? W * (H / W > 1.6 ? 0.94 : 0.84) : 1010 * k), pad = 16 * k, lip = 40 * k
    let screenW = width - pad * 2
    // iPad는 오른쪽 아래 창 크기 손잡이를 잘라 냄
    let srcH = img.size.height - statusCut - (tablet ? 60 : 0)
    let screenH = srcH * screenW / img.size.width
    let ox = (W - width) / 2
    let frame = R(ox, top, width, screenH + pad * 2 + lip)
    NSGraphicsContext.saveGraphicsState(); shadow(80 * k, 0.10, 24 * k).set(); NSColor.white.setFill()
    NSBezierPath(roundedRect: frame, xRadius: (tablet ? 60 : 96) * k, yRadius: (tablet ? 60 : 96) * k).fill(); NSGraphicsContext.restoreGraphicsState()
    line.setStroke()
    let border = NSBezierPath(roundedRect: frame.insetBy(dx: 1.5 * k, dy: 1.5 * k), xRadius: (tablet ? 59 : 95) * k, yRadius: (tablet ? 59 : 95) * k); border.lineWidth = 3 * k; border.stroke()
    let screen = R(ox + pad, top + pad + lip, screenW, screenH)
    NSGraphicsContext.saveGraphicsState()
    NSBezierPath(roundedRect: screen, xRadius: (tablet ? 44 : 80) * k, yRadius: (tablet ? 44 : 80) * k).addClip()
    NSColor.white.setFill(); screen.fill()
    img.draw(in: screen, from: NSRect(x: 0, y: tablet ? 60 : 0, width: img.size.width, height: srcH), operation: .sourceOver, fraction: 1)
    NSGraphicsContext.restoreGraphicsState()
}

for (i, s) in copy[lang]!.enumerated() {
    let ctx = CGContext(data: nil, width: Int(W), height: Int(H), bitsPerComponent: 8, bytesPerRow: 0,
                        space: CGColorSpace(name: CGColorSpace.sRGB)!, bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue)!
    NSGraphicsContext.current = NSGraphicsContext(cgContext: ctx, flipped: false)
    bg.setFill(); NSRect(x: 0, y: 0, width: W, height: H).fill()
    let bottom = header(s.1, s.2)
    phone(s.0, top: bottom + (landscape ? 80 : 110) * k)
    NSGraphicsContext.current = nil
    let rep = NSBitmapImageRep(cgImage: ctx.makeImage()!)
    try! rep.representation(using: .png, properties: [:])!.write(to: URL(fileURLWithPath: "\(out)/\(prefix)-\(i + 1).png"))
}
print("\(lang) \(prefix): \(copy[lang]!.count)장")
