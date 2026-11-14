// 스토어 견본 일기에 붙일 풍경 그림 여섯 장을 그린다.
// 실제 사진 대신 직접 그린 그림이라 저작권 걱정이 없다.
// 쓰는 법: swift scripts/store/make_photos.swift <결과 폴더>
import AppKit

_ = NSApplication.shared
let output = CommandLine.arguments[1]
let width = 1600, height = 1200

func color(_ hex: UInt32) -> NSColor {
    NSColor(srgbRed: CGFloat((hex >> 16) & 255) / 255, green: CGFloat((hex >> 8) & 255) / 255, blue: CGFloat(hex & 255) / 255, alpha: 1)
}

func sky(_ top: UInt32, _ bottom: UInt32) {
    NSGradient(starting: color(bottom), ending: color(top))!.draw(in: NSRect(x: 0, y: 0, width: width, height: height), angle: 90)
}

func circle(_ x: CGFloat, _ y: CGFloat, _ r: CGFloat, _ hex: UInt32) {
    color(hex).setFill()
    NSBezierPath(ovalIn: NSRect(x: x - r, y: y - r, width: r * 2, height: r * 2)).fill()
}

/** 부드러운 언덕. 점들을 곡선으로 잇고 아래를 채운다. */
func hill(_ points: [(CGFloat, CGFloat)], _ hex: UInt32) {
    let path = NSBezierPath()
    path.move(to: NSPoint(x: 0, y: 0))
    path.line(to: NSPoint(x: 0, y: points[0].1))
    for i in 1..<points.count {
        let (px, py) = points[i - 1], (x, y) = points[i]
        let mid = (px + x) / 2
        path.curve(to: NSPoint(x: x, y: y), controlPoint1: NSPoint(x: mid, y: py), controlPoint2: NSPoint(x: mid, y: y))
    }
    path.line(to: NSPoint(x: CGFloat(width), y: 0))
    path.close()
    color(hex).setFill()
    path.fill()
}

func band(_ y: CGFloat, _ h: CGFloat, _ hex: UInt32) {
    color(hex).setFill()
    NSRect(x: 0, y: y, width: CGFloat(width), height: h).fill()
}

func cloud(_ x: CGFloat, _ y: CGFloat, _ s: CGFloat, _ hex: UInt32) {
    circle(x, y, 60 * s, hex)
    circle(x + 70 * s, y + 25 * s, 75 * s, hex)
    circle(x + 150 * s, y, 60 * s, hex)
    color(hex).setFill()
    NSRect(x: x, y: y - 60 * s, width: 150 * s, height: 60 * s).fill()
}

func save(_ name: String, _ draw: () -> Void) {
    let context = CGContext(
        data: nil, width: width, height: height, bitsPerComponent: 8, bytesPerRow: 0,
        space: CGColorSpace(name: CGColorSpace.sRGB)!, bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue
    )!
    NSGraphicsContext.current = NSGraphicsContext(cgContext: context, flipped: false)
    draw()
    NSGraphicsContext.current = nil
    let rep = NSBitmapImageRep(cgImage: context.makeImage()!)
    try! rep.representation(using: .jpeg, properties: [.compressionFactor: 0.88])!
        .write(to: URL(fileURLWithPath: "\(output)/\(name).jpg"))
}

// 해 지는 바다
save("sea") {
    sky(0xF6C9B8, 0xFDE8C8)
    circle(800, 470, 170, 0xFFB38A)
    band(0, 460, 0x8CC7D9)
    band(300, 30, 0xA6D6E4)
    band(180, 22, 0xA6D6E4)
}

// 보랏빛 언덕
save("hills") {
    sky(0xE7DDFB, 0xFBEFF6)
    circle(1220, 880, 110, 0xFFFFFF)
    hill([(0, 620), (500, 760), (1000, 600), (1600, 720)], 0xC9B6F2)
    hill([(0, 420), (600, 520), (1100, 380), (1600, 470)], 0xA992E6)
    hill([(0, 220), (700, 300), (1600, 180)], 0x8E74D8)
}

// 밤 산과 달
save("night") {
    sky(0x2E2A5C, 0x6B5FA8)
    circle(1180, 920, 90, 0xFFF3C4)
    for (x, y) in [(200, 1000), (420, 1080), (700, 960), (900, 1110), (1450, 1050), (560, 840)] {
        circle(CGFloat(x), CGFloat(y), 6, 0xFFFFFF)
    }
    hill([(0, 500), (400, 720), (800, 480), (1200, 650), (1600, 520)], 0x4C4590)
    hill([(0, 260), (600, 380), (1600, 240)], 0x3A3478)
}

// 맑은 하늘과 구름
save("clouds") {
    sky(0xA9DBF5, 0xE6F6FD)
    cloud(220, 820, 1.2, 0xFFFFFF)
    cloud(980, 960, 0.9, 0xFFFFFF)
    cloud(1150, 560, 1.1, 0xF3FAFE)
    hill([(0, 220), (800, 320), (1600, 200)], 0xBFE5C8)
}

// 노을 사막
save("dunes") {
    sky(0xFCE3B0, 0xFFF4DC)
    circle(420, 760, 120, 0xFFC86B)
    hill([(0, 520), (700, 640), (1600, 480)], 0xF4C98E)
    hill([(0, 300), (900, 420), (1600, 280)], 0xE9AE6F)
}

// 라벤더 밭
save("lavender") {
    sky(0xF3E9FF, 0xFFF8F2)
    circle(1250, 900, 100, 0xFFE2B8)
    hill([(0, 560), (800, 620), (1600, 540)], 0xD9C8F5)
    band(0, 470, 0xB89AE8)
    for row in 0..<6 {
        for col in 0..<16 {
            circle(CGFloat(col) * 105 + CGFloat(row % 2) * 52, CGFloat(row) * 75 + 40, 22, 0x9C7BDD)
        }
    }
}
print("그림 6장: \(output)")
