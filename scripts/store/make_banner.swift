// 가로 그림: Play 그래픽 이미지(1024×500), App Store 헤더(3840×1646), 검색 결과 그림(3840×2560)
// 스크린샷과 같은 연한 라벤더 바탕, 두 줄 제목과 설명 한 줄, 상태바를 자른 앱 화면
// 쓰는 법: swift scripts/store/make_banner.swift <캡처 폴더> <결과 PNG> <ko|en|ja> <feature|header|search> [guide]
// 캡처 폴더에는 home.png, detail.png(iPhone 17 Pro 1206×2622)가 있어야 함
import AppKit
import CoreText

let args = CommandLine.arguments
_ = NSApplication.shared
let caps = args[1], output = args[2], lang = args[3], mode = args[4]
let guide = args.count > 5 && args[5] == "guide"
let root = URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent().deletingLastPathComponent()
CTFontManagerRegisterFontsForURL(root.appendingPathComponent("shared/core/designsystem/src/commonMain/composeResources/font/changwon_round_regular.otf") as CFURL, .process, nil)

let size: CGSize = ["feature": CGSize(width: 1024, height: 500), "header": CGSize(width: 3840, height: 1646), "search": CGSize(width: 3840, height: 2560)][mode]!
let W = size.width, H = size.height

func color(_ hex: UInt32, _ a: CGFloat = 1) -> NSColor {
    NSColor(srgbRed: CGFloat((hex >> 16) & 255) / 255, green: CGFloat((hex >> 8) & 255) / 255, blue: CGFloat(hex & 255) / 255, alpha: a)
}
let bg = color(0xF5F2FF), ink = color(0x1A1C20), sub = color(0x6E6A80), line = color(0xE4DEF7)
// 강조는 낱말 하나만. 라벤더 브랜드색 중 글자 대비가 되는 진한 단계
let accent = color(0x6550B9)
let accentWord = ["ko": "한 줄", "en": "One line", "ja": "1行"]
let statusCut: CGFloat = 162

let copy: [String: (String, String)] = [
    "ko": ("한 줄이면\n오늘 일기 끝", "바쁜 날에도 쓰기 쉬운 일기장"),
    "en": ("One line,\nand today is done", "A diary that's easy even on busy days"),
    "ja": ("1行で\n今日の日記は完了", "忙しい日でも書きやすい日記帳"),
]

func R(_ x: CGFloat, _ top: CGFloat, _ w: CGFloat, _ h: CGFloat) -> NSRect { NSRect(x: x, y: H - top - h, width: w, height: h) }

func attributed(_ s: String, _ px: CGFloat, _ c: NSColor, bold: Bool, align: NSTextAlignment) -> NSAttributedString {
    let style = NSMutableParagraphStyle(); style.alignment = align; style.lineHeightMultiple = 1.16
    let japanese = lang == "ja"
    let f = japanese ? NSFont(name: bold ? "HiraginoSans-W6" : "HiraginoSans-W3", size: px * 0.92)! : NSFont(name: "ChangwonDangamRound-Regular", size: px)!
    let stroke: CGFloat = japanese ? 0 : (bold ? (lang == "en" ? -5.0 : -3.4) : -0.6)
    return NSAttributedString(string: s, attributes: [.font: f, .foregroundColor: c, .strokeColor: c, .strokeWidth: stroke, .paragraphStyle: style])
}

// 제목과 설명을 한 덩어리로 x, 가운데 y에 그림
func block(x: CGFloat, centerY: CGFloat, width: CGFloat, title: CGFloat, align: NSTextAlignment) {
    let t = NSMutableAttributedString(attributedString: attributed(copy[lang]!.0, title, ink, bold: true, align: align))
    if let r = copy[lang]!.0.range(of: accentWord[lang]!) {
        let range = NSRange(r, in: copy[lang]!.0)
        t.addAttributes([.foregroundColor: accent, .strokeColor: accent], range: range)
    }
    let s = attributed(copy[lang]!.1, title * 0.46, sub, bold: false, align: align)
    let th = t.boundingRect(with: NSSize(width: width, height: 4000), options: [.usesLineFragmentOrigin]).height
    let sh = s.boundingRect(with: NSSize(width: width, height: 4000), options: [.usesLineFragmentOrigin]).height
    let gap = title * 0.36
    let top = centerY - (th + gap + sh) / 2
    t.draw(with: R(x, top, width, th + 4), options: [.usesLineFragmentOrigin])
    s.draw(with: R(x, top + th + gap, width, sh + 4), options: [.usesLineFragmentOrigin])
}

func shadow(_ blur: CGFloat, _ dy: CGFloat) -> NSShadow {
    let s = NSShadow(); s.shadowColor = color(0x2A1F66, 0.10); s.shadowBlurRadius = blur; s.shadowOffset = NSSize(width: 0, height: -dy); return s
}

// 얇고 밝은 기기 틀. 캔버스 밖은 잘림
func phone(_ name: String, x: CGFloat, top: CGFloat, width: CGFloat) {
    let img = NSImage(contentsOfFile: "\(caps)/\(name).png")!
    let k = width / 1010
    let pad = 16 * k, lip = 40 * k
    let screenW = width - pad * 2
    let srcH = img.size.height - statusCut
    let screenH = srcH * screenW / img.size.width
    let frame = R(x, top, width, screenH + pad * 2 + lip)
    NSGraphicsContext.saveGraphicsState(); shadow(80 * k, 24 * k).set(); NSColor.white.setFill()
    NSBezierPath(roundedRect: frame, xRadius: 96 * k, yRadius: 96 * k).fill(); NSGraphicsContext.restoreGraphicsState()
    line.setStroke()
    let border = NSBezierPath(roundedRect: frame.insetBy(dx: 1.5 * k, dy: 1.5 * k), xRadius: 95 * k, yRadius: 95 * k); border.lineWidth = max(1, 3 * k); border.stroke()
    let screen = R(x + pad, top + pad + lip, screenW, screenH)
    NSGraphicsContext.saveGraphicsState()
    NSBezierPath(roundedRect: screen, xRadius: 80 * k, yRadius: 80 * k).addClip()
    NSColor.white.setFill(); screen.fill()
    img.draw(in: screen, from: NSRect(x: 0, y: 0, width: img.size.width, height: srcH), operation: .sourceOver, fraction: 1)
    NSGraphicsContext.restoreGraphicsState()
}

let ctx = CGContext(data: nil, width: Int(W), height: Int(H), bitsPerComponent: 8, bytesPerRow: 0,
                    space: CGColorSpace(name: CGColorSpace.sRGB)!, bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue)!
NSGraphicsContext.current = NSGraphicsContext(cgContext: ctx, flipped: false)
bg.setFill(); NSRect(x: 0, y: 0, width: W, height: H).fill()

switch mode {
case "feature":
    // 왼쪽 글, 오른쪽에 아래가 잘린 화면 한 장
    block(x: 80, centerY: H / 2, width: 520, title: 50, align: .left)
    phone("home", x: 640, top: 56, width: 320)
case "header":
    // 안전 영역(1646×661, 가운데)에 글. 넓은 화면에서만 보이는 양옆에 화면
    block(x: 1097, centerY: 493 + 661 / 2, width: 1646, title: 124, align: .center)
    phone("home", x: 3000, top: 330, width: 720)
    phone("detail", x: 120, top: 330, width: 720)
case "search":
    // 왼쪽 글, 오른쪽 화면(안전 영역 836~3004 안)
    // 영어는 줄이 길어 두 줄에 들어가게 조금 줄임
    block(x: 900, centerY: H / 2, width: 1160, title: lang == "en" ? 124 : 150, align: .left)
    phone("home", x: 2170, top: 380, width: 830)
default:
    fatalError(mode)
}

if guide && mode != "feature" {
    NSColor.red.setStroke()
    let g = mode == "header" ? R(1097, 493, 1646, 661) : R(836, 0, 3004 - 836, H)
    let p = NSBezierPath(rect: g); p.lineWidth = 6; p.stroke()
}
NSGraphicsContext.current = nil
let rep = NSBitmapImageRep(cgImage: ctx.makeImage()!)
try! rep.representation(using: .png, properties: [:])!.write(to: URL(fileURLWithPath: output))
print("\(lang) \(mode)")
