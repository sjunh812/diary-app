// 스토어 스크린샷과 피처 그래픽을 만든다.
// 실제 앱 캡처 위에 머리말을 얹는다.
// 쓰는 법: swift scripts/store/make_screens.swift <캡처 폴더> <결과 폴더> <글꼴 폴더>
import AppKit
import CoreText

let args = CommandLine.arguments
_ = NSApplication.shared
let source = args[1], output = args[2], fonts = args[3]
for name in ["pretendard_bold.otf", "pretendard_medium.otf"] {
    CTFontManagerRegisterFontsForURL(URL(fileURLWithPath: "\(fonts)/\(name)") as CFURL, .process, nil)
}

func color(_ hex: UInt32) -> NSColor {
    NSColor(srgbRed: CGFloat((hex >> 16) & 255) / 255, green: CGFloat((hex >> 8) & 255) / 255, blue: CGFloat(hex & 255) / 255, alpha: 1)
}

let ink = color(0x1B1B23)
let weak = color(0x4A4A5C)
let accent = color(0x6C56CE)

/** 바탕은 앱 전용 색 하나다(`selected`). */
let background = color(0xEBE4FF)

func canvas(_ width: Int, _ height: Int, _ draw: () -> Void) -> NSBitmapImageRep {
    // 스토어는 알파 없는 PNG를 받는다.
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

func text(_ string: String, _ size: CGFloat, _ weight: String, _ tint: NSColor, _ rect: NSRect, _ align: NSTextAlignment, accented: String? = nil) {
    let style = NSMutableParagraphStyle()
    style.alignment = align
    style.lineHeightMultiple = 1.16
    let font = NSFont(name: "Pretendard-\(weight)", size: size) ?? NSFont.boldSystemFont(ofSize: size)
    let label = NSMutableAttributedString(string: string, attributes: [.font: font, .foregroundColor: tint, .paragraphStyle: style])
    if let accented, let range = string.range(of: accented) {
        label.addAttribute(.foregroundColor, value: accent, range: NSRange(range, in: string))
    }
    label.draw(in: rect)
}

/** 캡처 위쪽 상태 표시줄 높이. */
let statusBar: CGFloat = 56

func shot(_ name: String) -> NSImage { NSImage(contentsOfFile: "\(source)/\(name).png")! }

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

/** 기기 틀 안의 실제 앱 화면. 아래는 화면 밖으로 잘린다. */
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
    // 상태 표시줄은 잘라 낸다.
    image.draw(in: NSRect(x: rect.minX, y: rect.maxY - height + statusBar * scale, width: rect.width, height: height))
    NSGraphicsContext.restoreGraphicsState()
}

func save(_ rep: NSBitmapImageRep, _ name: String) {
    try! rep.representation(using: .png, properties: [:])!.write(to: URL(fileURLWithPath: "\(output)/\(name).png"))
}

// 기준: 심플하고 미감 있고 잘 읽혀야 한다.
// 앱 색 바탕, 큰 제목, 곧게 놓은 실제 화면만 둔다.
// 장식과 기울임을 넣지 않고 강조는 낱말 하나만 쓴다(2026-11-07 사용자 지적).
let slides: [(String, String, String)] = [
    ("write", "오늘 기분부터\n가볍게 골라 봐요", "기분"),
    ("list", "지난 일기가\n한눈에 보여요", "한눈에"),
    ("calendar", "한 달 기분을\n달력으로 봐요", "달력"),
    ("insight", "내 기분 흐름을\n돌아봐요", "돌아봐요"),
    ("backup", "사진까지\n드라이브에 백업해요", "백업")
]
for (index, (name, words, word)) in slides.enumerated() {
    let rep = canvas(1080, 1920) {
        text(words, 88, "Bold", ink, NSRect(x: 40, y: 1500, width: 1000, height: 300), .center, accented: word)
        // 첫 장은 아래쪽 기분 시트까지 보이게 화면 전체를 넣는다.
        if name == "write" {
            phone(name, NSRect(x: 215, y: 40, width: 650, height: 1400), 56)
        } else {
            phone(name, NSRect(x: 130, y: -560, width: 820, height: 1960), 64)
        }
    }
    save(rep, "phone-\(index + 1)")
}

let feature = canvas(1024, 500) {
    text("바쁜 날에도\n일기를 남겨 봐요", 60, "Bold", ink, NSRect(x: 76, y: 176, width: 500, height: 170), .left, accented: "일기")
    text("회원가입 없이, 사진까지 내 드라이브에", 25, "Medium", weak, NSRect(x: 76, y: 118, width: 500, height: 44), .left)
    // 피처 그래픽에는 기기 틀을 넣지 않는다(Play 안내).
    phone("list", NSRect(x: 640, y: -640, width: 300, height: 1040), 26, bezel: false)
}
save(feature, "feature")
