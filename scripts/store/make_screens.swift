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

/** 장마다 바뀌는 바탕. 앱의 기분 색에서 가져온다. */
nonisolated(unsafe) var background = color(0xE7E0FF)

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

func text(_ string: String, _ size: CGFloat, _ weight: String, _ tint: NSColor, _ rect: NSRect, _ align: NSTextAlignment) {
    let style = NSMutableParagraphStyle()
    style.alignment = align
    style.lineHeightMultiple = 1.18
    let font = NSFont(name: "Pretendard-\(weight)", size: size) ?? NSFont.boldSystemFont(ofSize: size)
    NSAttributedString(string: string, attributes: [.font: font, .foregroundColor: tint, .paragraphStyle: style]).draw(in: rect)
}

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
func phone(_ name: String, _ rect: NSRect, _ radius: CGFloat) {
    let frame = rect.insetBy(dx: -14, dy: -14)
    let bezel = NSBezierPath(roundedRect: frame, xRadius: radius + 14, yRadius: radius + 14)
    shadowed(0.22, 48) {
        ink.setFill()
        bezel.fill()
    }
    NSGraphicsContext.saveGraphicsState()
    NSBezierPath(roundedRect: rect, xRadius: radius, yRadius: radius).addClip()
    let image = shot(name)
    let height = image.size.height * rect.width / image.size.width
    image.draw(in: NSRect(x: rect.minX, y: rect.maxY - height, width: rect.width, height: height))
    NSGraphicsContext.restoreGraphicsState()
}

/** 화면의 한 조각을 카드로 크게 꺼낸다. [area]는 캡처 좌표(위가 0)다. */
func lifted(_ name: String, _ area: NSRect, _ rect: NSRect, _ radius: CGFloat, _ angle: CGFloat = 0) {
    NSGraphicsContext.saveGraphicsState()
    let turn = NSAffineTransform()
    turn.translateX(by: rect.midX, yBy: rect.midY)
    turn.rotate(byDegrees: angle)
    turn.translateX(by: -rect.midX, yBy: -rect.midY)
    turn.concat()
    let path = NSBezierPath(roundedRect: rect, xRadius: radius, yRadius: radius)
    shadowed(0.2, 40) {
        NSColor.white.setFill()
        path.fill()
    }
    path.addClip()
    let image = shot(name)
    let from = NSRect(x: area.minX, y: image.size.height - area.maxY, width: area.width, height: area.height)
    image.draw(in: rect, from: from, operation: .sourceOver, fraction: 1)
    NSGraphicsContext.restoreGraphicsState()
}

/** 바탕에 크게 놓는 둥근 덩어리. */
func blob(_ tint: NSColor, _ x: CGFloat, _ y: CGFloat, _ width: CGFloat, _ height: CGFloat) {
    tint.setFill()
    NSBezierPath(ovalIn: NSRect(x: x, y: y, width: width, height: height)).fill()
}

enum Mood { case smile, laugh, wink }

/** 앱의 기분 얼굴을 닮은 스티커. 흰 테두리로 바탕에서 띄운다. */
func face(_ tint: NSColor, _ x: CGFloat, _ y: CGFloat, _ r: CGFloat, _ mood: Mood = .smile) {
    shadowed(0.18, r * 0.35) {
        NSColor.white.setFill()
        NSBezierPath(ovalIn: NSRect(x: x - r - r * 0.09, y: y - r - r * 0.09, width: (r * 1.09) * 2, height: (r * 1.09) * 2)).fill()
    }
    tint.setFill()
    NSBezierPath(ovalIn: NSRect(x: x - r, y: y - r, width: r * 2, height: r * 2)).fill()
    ink.setFill()
    ink.setStroke()
    let eye = r * 0.11
    let line = NSBezierPath()
    line.lineWidth = r * 0.1
    line.lineCapStyle = .round
    switch mood {
    case .laugh:
        for side: CGFloat in [-1, 1] {
            line.appendArc(withCenter: NSPoint(x: x + side * r * 0.36, y: y + r * 0.08), radius: r * 0.17, startAngle: 20, endAngle: 160)
            line.move(to: NSPoint(x: x, y: y))
        }
    default:
        NSBezierPath(ovalIn: NSRect(x: x - r * 0.36 - eye, y: y + r * 0.14 - eye, width: eye * 2, height: eye * 2)).fill()
        NSBezierPath(ovalIn: NSRect(x: x + r * 0.36 - eye, y: y + r * 0.14 - eye, width: eye * 2, height: eye * 2)).fill()
    }
    line.removeAllPoints()
    if mood == .laugh {
        for side: CGFloat in [-1, 1] {
            let arc = NSBezierPath()
            arc.lineWidth = r * 0.1
            arc.lineCapStyle = .round
            arc.appendArc(withCenter: NSPoint(x: x + side * r * 0.36, y: y + r * 0.06), radius: r * 0.17, startAngle: 20, endAngle: 160)
            arc.stroke()
        }
    }
    line.appendArc(withCenter: NSPoint(x: x, y: y - r * 0.1), radius: r * 0.42, startAngle: 200, endAngle: 340)
    line.stroke()
}

func save(_ rep: NSBitmapImageRep, _ name: String) {
    try! rep.representation(using: .png, properties: [:])!.write(to: URL(fileURLWithPath: "\(output)/\(name).png"))
}

let sky = color(0xB8E8FF), yellow = color(0xFFF68A), peach = color(0xFFD4A8)
let pink = color(0xFCC9F5), mint = color(0xB8EDC0), lavender = color(0xD9CFFF)

func title(_ words: String) {
    text(words, 84, "Bold", ink, NSRect(x: 40, y: 1480, width: 1000, height: 320), .center)
}

func slide(_ tint: NSColor, _ name: String, _ words: String, _ extra: () -> Void) -> NSBitmapImageRep {
    background = tint
    return canvas(1080, 1920) {
        blob(NSColor(white: 1, alpha: 0.42), -220, 960, 720, 720)
        blob(NSColor(white: 1, alpha: 0.42), 640, 200, 680, 680)
        title(words)
        phone(name, NSRect(x: 150, y: -520, width: 780, height: 1860), 64)
        extra()
    }
}

save(slide(lavender, "w1", "바쁜 날에도\n일기를 남겨요") {
    lifted("w1", NSRect(x: 30, y: 345, width: 660, height: 210), NSRect(x: 130, y: 560, width: 820, height: 261), 44, -2)
    face(yellow, 120, 1380, 88, .laugh)
    face(peach, 960, 1420, 74)
    face(pink, 985, 1040, 54)
}, "phone-1")

save(slide(sky, "b1", "사진까지\n내 드라이브에") {
    lifted("b1", NSRect(x: 50, y: 252, width: 980, height: 230), NSRect(x: 40, y: 760, width: 1000, height: 235), 40, 1.5)
    face(mint, 112, 1395, 70)
}, "phone-2")

save(slide(yellow.blended(withFraction: 0.45, of: .white)!, "c1", "한 달 기분을\n달력으로") {
    lifted("c1", NSRect(x: 30, y: 418, width: 1020, height: 140), NSRect(x: 30, y: 700, width: 1020, height: 140), 36, -1.5)
    face(peach, 980, 1390, 80, .laugh)
    face(sky, 104, 1400, 62)
}, "phone-3")

save(slide(pink.blended(withFraction: 0.35, of: .white)!, "l1", "내 일기는\n나만 보게") {
    face(lavender, 985, 1400, 72)
    face(mint, 98, 1040, 56)
}, "phone-4")

save(slide(mint.blended(withFraction: 0.35, of: .white)!, "f1", "글꼴 10종에서\n골라 써요") {
    lifted("f1", NSRect(x: 50, y: 254, width: 980, height: 237), NSRect(x: 30, y: 880, width: 1020, height: 247), 44, -2)
    face(yellow, 975, 1395, 76, .laugh)
}, "phone-5")

save(slide(peach.blended(withFraction: 0.35, of: .white)!, "h1", "지난 일기를\n사진과 함께") {
    lifted("h1", NSRect(x: 45, y: 430, width: 990, height: 535), NSRect(x: 30, y: 300, width: 1020, height: 551), 48, 2)
    face(pink, 110, 1395, 68)
}, "phone-6")

background = lavender
let feature = canvas(1024, 500) {
    blob(sky, 560, 230, 420, 420)
    blob(yellow, 800, -120, 340, 340)
    blob(pink, 470, -170, 300, 300)
    text("바쁜 날에도\n일기를 남겨요", 60, "Bold", ink, NSRect(x: 64, y: 180, width: 500, height: 180), .left)
    text("회원가입 없이, 사진까지 내 드라이브에", 25, "Medium", weak, NSRect(x: 64, y: 122, width: 500, height: 44), .left)
    lifted("h1", NSRect(x: 45, y: 430, width: 990, height: 535), NSRect(x: 560, y: 110, width: 400, height: 216), 28, -3)
    face(yellow, 560, 390, 56, .laugh)
    face(mint, 900, 410, 44)
    face(peach, 980, 110, 36)
}
save(feature, "feature")
