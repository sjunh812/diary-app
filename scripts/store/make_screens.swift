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

let background = color(0xEDE9FE)
let ink = color(0x1B1B23)
let weak = color(0x5B5B6B)

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

func phone(_ name: String, _ rect: NSRect, _ radius: CGFloat) {
    let image = NSImage(contentsOfFile: "\(source)/\(name).png")!
    let path = NSBezierPath(roundedRect: rect, xRadius: radius, yRadius: radius)
    let shadow = NSShadow()
    shadow.shadowColor = NSColor(white: 0, alpha: 0.14)
    shadow.shadowBlurRadius = 36
    shadow.shadowOffset = NSSize(width: 0, height: -10)
    NSGraphicsContext.saveGraphicsState()
    shadow.set()
    NSColor.white.setFill()
    path.fill()
    NSGraphicsContext.restoreGraphicsState()
    NSGraphicsContext.saveGraphicsState()
    path.addClip()
    let scale = rect.width / image.size.width
    let height = image.size.height * scale
    image.draw(in: NSRect(x: rect.minX, y: rect.maxY - height, width: rect.width, height: height))
    NSGraphicsContext.restoreGraphicsState()
}

func save(_ rep: NSBitmapImageRep, _ name: String) {
    try! rep.representation(using: .png, properties: [:])!.write(to: URL(fileURLWithPath: "\(output)/\(name).png"))
}

let screens = [
    ("w1", "바쁜 날에도\n일기를 남겨요"),
    ("b1", "사진까지\n내 드라이브에"),
    ("c1", "한 달 기분을\n달력으로"),
    ("l1", "내 일기는\n나만 보게"),
    ("f1", "글꼴 10종에서\n골라 써요"),
    ("h1", "지난 일기를\n사진과 함께")
]
for (index, (name, title)) in screens.enumerated() {
    let rep = canvas(1080, 1920) {
        text(title, 76, "Bold", ink, NSRect(x: 60, y: 1500, width: 960, height: 300), .center)
        phone(name, NSRect(x: 110, y: -470, width: 860, height: 1911), 56)
    }
    save(rep, "phone-\(index + 1)")
}

let feature = canvas(1024, 500) {
    text("바쁜 날에도\n일기를 남겨요", 58, "Bold", ink, NSRect(x: 72, y: 170, width: 520, height: 170), .left)
    text("회원가입 없이, 사진까지 내 드라이브에", 26, "Medium", weak, NSRect(x: 72, y: 118, width: 520, height: 44), .left)
    phone("h1", NSRect(x: 620, y: -420, width: 330, height: 733 + 140), 28)
}
save(feature, "feature")
