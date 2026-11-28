// 스토어 스크린샷(2.3.0 틀): 옅은 라벤더 바탕, 두 줄 제목(강조 낱말 하나는 [ ]로 감싸 브랜드 글자색), 흰 몸체에 옅은 테두리 틀
// 쓰는 법: swift scripts/store/compose_screens.swift <캡처 폴더> <결과 폴더> <ko|en|ja> <폭> <높이> <앞이름> <문구 표>
//   폰: 1206 2622(App Store 6.3"), 1320 2868(6.9"), 1620 2880(Play)
//   iPad 세로: TABLET=port … 2064 2752, Play 태블릿 가로: TABLET=land … 3840 2160
// 문구 표 줄: 종류|캡처|제목(\n 줄바꿈, [강조])|설명
//   phone|캡처   stack|캡처=이름표/캡처=이름표/…(달력 부분만 잘라 카드로)
// 문구 표와 만드는 순서는 docs/store/listing.md "스크린샷 머리말"
import AppKit
import CoreText

let args = CommandLine.arguments
_ = NSApplication.shared
let caps = args[1], out = args[2], lang = args[3]
let W = CGFloat(Double(args[4])!), H = CGFloat(Double(args[5])!), prefix = args[6]
let root = URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent().deletingLastPathComponent().path + "/shared/core/designsystem/src/commonMain/composeResources/font/"
for f in ["changwon_round_regular.otf", "pretendard_regular.otf", "pretendard_semibold.otf"] {
    CTFontManagerRegisterFontsForURL(URL(fileURLWithPath: root + f) as CFURL, .process, nil)
}
let tablet = ProcessInfo.processInfo.environment["TABLET"] ?? ""
let land = tablet == "land"
// 태블릿은 제목을 폰보다 덜 키움. 가로는 높이 기준
let k = land ? H / 1500 : (tablet == "port" ? W / 1650 : W / 1206)

func color(_ hex: UInt32, _ a: CGFloat = 1) -> NSColor {
    NSColor(srgbRed: CGFloat((hex >> 16) & 255) / 255, green: CGFloat((hex >> 8) & 255) / 255, blue: CGFloat(hex & 255) / 255, alpha: a)
}
let bg = color(0xF5F2FF), ink = color(0x1A1C20), sub = color(0x6E6A80), accent = color(0x6550B9)
let body = NSColor.white, rim = color(0xE4DEF7)
let bezelPt: CGFloat = 14

func R(_ x: CGFloat, _ top: CGFloat, _ w: CGFloat, _ h: CGFloat) -> NSRect { NSRect(x: x, y: H - top - h, width: w, height: h) }

func font(bold: Bool, size: CGFloat) -> NSFont {
    switch lang {
    case "ja": return NSFont(name: bold ? "HiraginoSans-W6" : "HiraginoSans-W3", size: size * 0.92)!
    default: return NSFont(name: "ChangwonDangamRound-Regular", size: size)!
    }
}

// [낱말]은 브랜드색. 창원단감둥근체는 굵기가 하나라 획을 덧그림
func text(_ raw: String, size: CGFloat, color c: NSColor, top: CGFloat, bold: Bool) -> CGFloat {
    let style = NSMutableParagraphStyle(); style.alignment = .center; style.lineHeightMultiple = 1.18
    let f = font(bold: bold, size: size)
    let stroke: CGFloat = lang == "ja" ? 0 : (bold ? (lang == "ko" ? -3.4 : -5.0) : -0.6)
    let s = NSMutableAttributedString()
    var hot = false
    for part in raw.replacingOccurrences(of: "\\n", with: "\n").split(omittingEmptySubsequences: false, whereSeparator: { $0 == "[" || $0 == "]" }).map(String.init) {
        let col = hot ? accent : c
        s.append(NSAttributedString(string: part, attributes: [.font: f, .foregroundColor: col, .strokeColor: col, .strokeWidth: stroke, .paragraphStyle: style]))
        hot.toggle()
    }
    let h = s.boundingRect(with: NSSize(width: W - 120 * k, height: 2000), options: [.usesLineFragmentOrigin]).height
    s.draw(with: R(60 * k, top, W - 120 * k, h + 10), options: [.usesLineFragmentOrigin])
    return top + h
}

func header(_ title: String, _ line: String) -> CGFloat {
    let oneLine = land ? title.replacingOccurrences(of: "\\n", with: lang == "ja" ? "" : " ") : title
    let t = text(oneLine, size: 92 * k, color: ink, top: (land ? 110 : 190) * k, bold: true)
    return text(line, size: 46 * k, color: sub, top: t + 34 * k, bold: false)
}

func shadow(_ blur: CGFloat, _ alpha: CGFloat, _ dy: CGFloat) -> NSShadow {
    let s = NSShadow(); s.shadowColor = color(0x2A1F66, alpha); s.shadowBlurRadius = blur; s.shadowOffset = NSSize(width: 0, height: -dy); return s
}

func image(_ name: String) -> NSImage { NSImage(contentsOfFile: "\(caps)/\(name).png")! }

// 플랫폼 표시 없는 흰 틀. 상태바를 잘라 내 iOS·Android 어느 스토어에도 씀. 아래는 캔버스 밖으로 잘림
let statusCut: CGFloat = tablet.isEmpty ? 150 : 56
// iPad는 상태바를 잘라 낸 자리에 흰 여백을 두어 위쪽 버튼이 모서리에 붙지 않게
let topPad: CGFloat = tablet.isEmpty ? 0 : 36
let bottomCut: CGFloat = tablet.isEmpty ? 0 : 60
// iPad 화면은 모서리가 덜 둥글어 위쪽 버튼이 잘리지 않게
let corner: CGFloat = tablet.isEmpty ? 120 : 64
func phone(_ name: String, top: CGFloat, width: CGFloat = 1000) {
    let img = image(name)
    let w = land ? W * 0.74 : (tablet == "port" ? W * 0.84 : width * k), bezel = bezelPt * k
    let screenW = w - bezel * 2, s = screenW / img.size.width
    let srcH = img.size.height - statusCut - bottomCut
    let screenH = srcH * s + topPad * k
    let ox = (W - w) / 2
    let frame = R(ox, top, w, screenH + bezel * 2)
    NSGraphicsContext.saveGraphicsState(); shadow(90 * k, 0.10, 30 * k).set(); body.setFill()
    NSBezierPath(roundedRect: frame, xRadius: corner * k, yRadius: corner * k).fill(); NSGraphicsContext.restoreGraphicsState()
    rim.setStroke()
    let edge = NSBezierPath(roundedRect: frame.insetBy(dx: 2 * k, dy: 2 * k), xRadius: (corner - 2) * k, yRadius: (corner - 2) * k); edge.lineWidth = 3 * k; edge.stroke()
    let screen = R(ox + bezel, top + bezel, screenW, screenH)
    NSGraphicsContext.saveGraphicsState()
    NSBezierPath(roundedRect: screen, xRadius: (corner - bezelPt) * k, yRadius: (corner - bezelPt) * k).addClip()
    NSColor.white.setFill(); screen.fill()
    img.draw(in: NSRect(x: screen.minX, y: screen.minY, width: screenW, height: srcH * s), from: NSRect(x: 0, y: bottomCut, width: img.size.width, height: srcH), operation: .sourceOver, fraction: 1)
    NSGraphicsContext.restoreGraphicsState()
}

// 달력 부분만 잘라 위아래로
func stackRow(_ items: [String], top: CGFloat) {
    let margin = 120 * k, gap = 56 * k
    let w = (W - margin * 2 - gap * CGFloat(items.count - 1)) / CGFloat(items.count)
    let padX = 30 * k, padY = 34 * k
    // 남은 높이 가운데에 둠
    let cardH = 640 * (w - padX * 2) / 1320 + padY * 2 + 84 * k
    let top = top + max(0, (H - top - cardH) / 2 - 40 * k)
    for (j, item) in items.enumerated() {
        let p = item.components(separatedBy: "="), img = image(p[0]), label = p.count > 1 ? p[1] : ""
        let x = margin + CGFloat(j) * (w + gap)
        let style = NSMutableParagraphStyle(); style.alignment = .left
        NSAttributedString(string: label, attributes: [.font: font(bold: false, size: 40 * k), .foregroundColor: sub, .strokeColor: sub, .strokeWidth: lang == "ja" ? 0 : -0.6, .paragraphStyle: style])
            .draw(with: R(x + 20 * k, top, w, 60 * k), options: [.usesLineFragmentOrigin])
        let y = top + 84 * k
        let cropTop: CGFloat = 330, cropH: CGFloat = 640
        let screenW = w - padX * 2, screenH = cropH * screenW / img.size.width
        let frame = R(x, y, w, screenH + padY * 2)
        NSGraphicsContext.saveGraphicsState(); shadow(60 * k, 0.08, 16 * k).set(); NSColor.white.setFill()
        NSBezierPath(roundedRect: frame, xRadius: 56 * k, yRadius: 56 * k).fill(); NSGraphicsContext.restoreGraphicsState()
        let screen = R(x + padX, y + padY, screenW, screenH)
        NSGraphicsContext.saveGraphicsState()
        NSBezierPath(rect: screen).addClip()
        img.draw(in: screen, from: NSRect(x: 0, y: img.size.height - cropTop - cropH, width: img.size.width, height: cropH), operation: .sourceOver, fraction: 1)
        NSGraphicsContext.restoreGraphicsState()
    }
}

func stack(_ items: [String], top: CGFloat) {
    // 캔버스가 낮으면(Play 9:16) 세 장이 들어가게 함께 줄임
    let unit = 84 + 80 + 64 + 640 * (1010 - 72) / 1320.0
    let f = min(1, (H - top - 70 * k) / (CGFloat(items.count) * unit * k))
    var y = top
    for item in items {
        let p = item.components(separatedBy: "="), img = image(p[0]), label = p.count > 1 ? p[1] : ""
        let w = 1010 * k * f, padX = 36 * k * f, padY = 40 * k * f
        let style = NSMutableParagraphStyle(); style.alignment = .left
        NSAttributedString(string: label, attributes: [.font: font(bold: false, size: 40 * k), .foregroundColor: sub, .strokeColor: sub, .strokeWidth: lang == "ja" ? 0 : -0.6, .paragraphStyle: style])
            .draw(with: R((W - w) / 2 + 24 * k, y, w, 60 * k), options: [.usesLineFragmentOrigin])
        y += 84 * k * f
        let cropTop: CGFloat = 330, cropH: CGFloat = 640
        let screenW = w - padX * 2, screenH = cropH * screenW / img.size.width
        let frame = R((W - w) / 2, y, w, screenH + padY * 2)
        NSGraphicsContext.saveGraphicsState(); shadow(60 * k, 0.08, 16 * k).set(); NSColor.white.setFill()
        NSBezierPath(roundedRect: frame, xRadius: 60 * k, yRadius: 60 * k).fill(); NSGraphicsContext.restoreGraphicsState()
        let screen = R((W - w) / 2 + padX, y + padY, screenW, screenH)
        NSGraphicsContext.saveGraphicsState()
        NSBezierPath(rect: screen).addClip()
        img.draw(in: screen, from: NSRect(x: 0, y: img.size.height - cropTop - cropH, width: img.size.width, height: cropH), operation: .sourceOver, fraction: 1)
        NSGraphicsContext.restoreGraphicsState()
        y += screenH + padY * 2 + 64 * k * f
    }
}

let rows = try! String(contentsOfFile: args[7], encoding: .utf8).split(separator: "\n").map(String.init).filter { !$0.isEmpty && !$0.hasPrefix("#") }
for (i, row) in rows.enumerated() {
    let p = row.components(separatedBy: "|")
    let ctx = CGContext(data: nil, width: Int(W), height: Int(H), bitsPerComponent: 8, bytesPerRow: 0,
                        space: CGColorSpace(name: CGColorSpace.sRGB)!, bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue)!
    NSGraphicsContext.current = NSGraphicsContext(cgContext: ctx, flipped: false)
    bg.setFill(); NSRect(x: 0, y: 0, width: W, height: H).fill()
    let bottom = header(p[2], p[3])
    switch p[0] {
    case "stack": if land { stackRow(p[1].components(separatedBy: "/"), top: bottom + 160 * k) } else { stack(p[1].components(separatedBy: "/"), top: bottom + 100 * k) }
    default: phone(p[1], top: bottom + (land ? 80 : 110) * k)
    }
    NSGraphicsContext.current = nil
    try! NSBitmapImageRep(cgImage: ctx.makeImage()!).representation(using: .png, properties: [:])!
        .write(to: URL(fileURLWithPath: "\(out)/\(prefix)-\(i + 1).png"))
}
print("\(lang) \(prefix): \(rows.count)장")
