// App Store 상품 페이지 헤더(21:9, 3840×1646) 생성
// 옅은 바탕에 앱 아이콘의 연필과 기분 얼굴 아홉만 둠. 무늬 없이 여백을 넉넉히
// 글자가 없어 언어마다 따로 만들지 않음
// 얼굴 모양과 색은 DiaryMoodFace.kt·DiaryColors.kt 라이트 값, 연필은 LaunchPencil.imageset 그대로
// 쓰는 법: swift scripts/store/make_header.swift <결과 PNG> <연필 SVG> [guide|-] [ring|arc]
// guide를 주면 Apple 안전 영역(1646×661, x 1097, y 493)을 빨간 선으로 겹쳐 그림(확인용)
import AppKit
import CoreText

let args = CommandLine.arguments
let output = args[1]
let pencilPath = args[2]
let guide = args.count > 3 && args[3] == "guide"
// 다섯째 인자: ring(기본), arc, pencil, wordmark-ko·en·ja(연필 심볼과 앱 이름). wordmark는 여섯째 인자로 글꼴 파일

let width = 3840, height = 1646
let safe = CGRect(x: 1097, y: 493, width: 1646, height: 661)

func color(_ hex: UInt32) -> CGColor {
    CGColor(srgbRed: CGFloat((hex >> 16) & 255) / 255, green: CGFloat((hex >> 8) & 255) / 255, blue: CGFloat(hex & 255) / 255, alpha: 1)
}

/** 바탕은 앱 아이콘처럼 흰빛에서 옅은 라벤더(`selected`)로 */
let top = color(0xF8F5FF), bottom = color(0xEBE4FF)
let ink = color(0x1A1C20)

struct Face {
    let fill: UInt32
    let lines: [String]
    let solids: [String]
}

let dotEyes = "M 17.97 18.7 L 17.97 19.8 M 26.04 18.7 L 26.04 19.8"
let angry = Face(fill: 0xFFAFB0, lines: ["M 14.8 15.8 L 20 18.6 M 29.2 15.8 L 24 18.6", "M 18.2 21.3 L 18.2 21.9 M 25.8 21.3 L 25.8 21.9", "M 16.5 30.5 C 19.5 26.8 24.5 26.8 27.5 30.5"], solids: [])
let cool = Face(
    fill: 0xAEE4FF, lines: ["M 21 18.2 L 23 18.2", "M 17 27.8 C 20 29.8 24 29.6 27.5 26.6"],
    solids: ["M 13 17 L 21 17 L 21 19 C 21 21.8 19.4 23 17 23 C 14.6 23 13 21.8 13 19 Z", "M 23 17 L 31 17 L 31 19 C 31 21.8 29.4 23 27 23 C 24.6 23 23 21.8 23 19 Z"]
)
let sad = Face(
    fill: 0xC4F4FE, lines: ["M 17.97 18 L 17.97 19 M 26.04 18 L 26.04 19", "M 17.6 31 C 20 28.2 24 28.2 26.4 31"],
    solids: ["M 27.6 21 C 26.3 23.1 25.4 24.4 25.4 25.6 C 25.4 26.9 26.4 27.8 27.6 27.8 C 28.8 27.8 29.8 26.9 29.8 25.6 C 29.8 24.4 28.9 23.1 27.6 21 Z"]
)
let sick = Face(fill: 0xBEE9B4, lines: ["M 15.4 17.4 L 19.6 19.2 L 15.4 21 M 28.6 17.4 L 24.4 19.2 L 28.6 21", "M 15 28.6 C 16.6 26.4 18.4 26.4 20 28.4 C 21 29.7 23 29.7 24 28.4 C 25.6 26.4 27.4 26.4 29 28.6"], solids: [])
let laugh = Face(fill: 0xFDFA87, lines: ["M 16.13 19.43 C 17.35 17.96 18.57 17.96 19.8 19.43 M 24.2 19.43 C 25.42 17.96 26.64 17.96 27.86 19.43"], solids: ["M 14.8 24.4 C 15.4 32 28.6 32 29.2 24.4 Z"])
let meh = Face(fill: 0xDFD4E4, lines: [dotEyes, "M 17.42 27.5 L 26.59 27.5"], solids: [])
let bad = Face(fill: 0x808080, lines: ["M 15.6 20 L 19.8 18 M 28.4 20 L 24.2 18", "M 16 30 C 19.4 26 24.6 26 28 30"], solids: [])
let good = Face(fill: 0xFFD4A8, lines: [dotEyes, "M 15.6 25.4 C 19.6 30 24.4 30 28.4 25.4"], solids: [])
let tired = Face(
    fill: 0xFCC6F7,
    lines: ["M 15.2 19.2 C 16.6 20.9 18.6 20.9 20 19.2 M 24 19.2 C 25.4 20.9 27.4 20.9 28.8 19.2", "M 22 26.4 C 23.4 26.4 24.3 27.7 24.3 29.2 C 24.3 30.7 23.4 31.8 22 31.8 C 20.6 31.8 19.7 30.7 19.7 29.2 C 19.7 27.7 20.6 26.4 22 26.4 Z", "M 29.2 8.8 L 33.2 8.8 L 29.2 13 L 33.2 13"],
    solids: []
)

/** M·L·C·Z만 쓰는 경로(Figma에서 뽑은 44 칸 기준) */
func path(_ data: String) -> CGPath {
    let tokens = data.split(separator: " ").map(String.init)
    let result = CGMutablePath()
    var i = 0
    func number() -> CGFloat {
        defer { i += 1 }
        return CGFloat(Double(tokens[i])!)
    }
    var command = ""
    while i < tokens.count {
        if Double(tokens[i]) == nil {
            command = tokens[i]
            i += 1
            if command == "Z" { result.closeSubpath(); continue }
        }
        switch command {
        case "M": result.move(to: CGPoint(x: number(), y: number()))
        case "L": result.addLine(to: CGPoint(x: number(), y: number()))
        case "C":
            let a = CGPoint(x: number(), y: number()), b = CGPoint(x: number(), y: number()), c = CGPoint(x: number(), y: number())
            result.addCurve(to: c, control1: a, control2: b)
        default: fatalError("모르는 명령 \(command)")
        }
    }
    return result
}

let context = CGContext(
    data: nil, width: width, height: height, bitsPerComponent: 8, bytesPerRow: 0,
    space: CGColorSpace(name: CGColorSpace.sRGB)!, bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue
)!
// 위에서 아래로 재는 좌표(Apple 안전 영역과 같은 기준)
context.translateBy(x: 0, y: CGFloat(height))
context.scaleBy(x: 1, y: -1)
let layoutArg = args.count > 4 ? args[4] : "ring"
if layoutArg.hasPrefix("wordmark") {
    // 가운데가 밝고 가장자리로 갈수록 라벤더(Structured·Goodnotes 헤더 방식)
    let radial = CGGradient(colorsSpace: CGColorSpace(name: CGColorSpace.sRGB), colors: [color(0xFBF9FF), color(0xE6DDFF)] as CFArray, locations: [0, 1])!
    let mid = CGPoint(x: safe.midX, y: safe.midY)
    context.drawRadialGradient(radial, startCenter: mid, startRadius: 0, endCenter: mid, endRadius: CGFloat(width) * 0.55, options: [.drawsAfterEndLocation])
} else {
    let gradient = CGGradient(colorsSpace: CGColorSpace(name: CGColorSpace.sRGB), colors: [top, bottom] as CFArray, locations: [0, 1])!
    context.drawLinearGradient(gradient, start: CGPoint(x: 0, y: 0), end: CGPoint(x: CGFloat(width), y: CGFloat(height)), options: [])
}

let layout = layoutArg
let layoutDiameter: CGFloat = layout == "arc" ? 150 : 170
// 무늬 없이 기분 얼굴 아홉을 모두 색으로. 가운데 연필 둘레에 둠
// iPhone은 헤더를 폭에 맞춰 줄여 보여 줘서 얼굴을 크게, 모두 안전 영역(1646×661) 안에
let diameter: CGFloat = layoutDiameter
let center = CGPoint(x: safe.midX, y: safe.midY)

/** 얼굴 하나 */
func draw(_ face: Face, at point: CGPoint) {
    let frame = CGRect(x: point.x - diameter / 2, y: point.y - diameter / 2, width: diameter, height: diameter)
    let scale = diameter / 44
    context.saveGState()
    context.setFillColor(color(face.fill))
    context.fillEllipse(in: frame)
    context.translateBy(x: frame.minX, y: frame.minY)
    context.scaleBy(x: scale, y: scale)
    context.setFillColor(ink)
    for solid in face.solids {
        context.addPath(path(solid))
        context.fillPath()
    }
    context.setStrokeColor(ink)
    // 44 칸에서 선 1.6(moodFaceLine)
    context.setLineWidth(1.6)
    context.setLineCap(.round)
    context.setLineJoin(.round)
    for line in face.lines {
        context.addPath(path(line))
        context.strokePath()
    }
    context.restoreGState()
}

// 앱의 기분 고르기 순서. 밝은 기분부터 한 바퀴 돌며 이어짐
let faces = [good, laugh, cool, meh, tired, sad, sick, angry, bad]
let points: [CGPoint]
switch layout {
case _ where layout.hasPrefix("wordmark"):
    points = []
case "pencil":
    // 기분 없이 연필 하나만
    points = []
case "arc":
    // 연필 위로 완만한 무지개 한 줄. 가운데가 가장 높음
    points = (0..<9).map { i in
        let t = CGFloat(i) / 8 * 2 - 1
        return CGPoint(x: center.x + t * 738, y: safe.minY + diameter / 2 + (t * t) * 240)
    }
default:
    // 연필 둘레 타원 한 바퀴. 둘레 길이로 고르게 나눠 간격을 맞춤
    let rx: CGFloat = 700, ry: CGFloat = 245
    var samples: [(CGFloat, CGFloat)] = [(-.pi / 2, 0)]
    let steps = 3600
    for k in 1...steps {
        // 맨 위(-90도)에서 시작해 재야 돌린 뒤에도 간격이 고름
        let a0 = CGFloat(k - 1) / CGFloat(steps) * 2 * .pi - .pi / 2, a1 = CGFloat(k) / CGFloat(steps) * 2 * .pi - .pi / 2
        let d = hypot(rx * (cos(a1) - cos(a0)), ry * (sin(a1) - sin(a0)))
        samples.append((a1, samples.last!.1 + d))
    }
    let total = samples.last!.1
    points = (0..<9).map { i in
        let target = CGFloat(i) / 9 * total
        let angle = samples.first { $0.1 >= target }!.0
        return CGPoint(x: center.x + cos(angle) * rx, y: center.y + sin(angle) * ry)
    }
}
for (face, point) in zip(faces, points) { draw(face, at: point) }

// 앱 아이콘의 연필
let pencil = NSImage(contentsOf: URL(fileURLWithPath: pencilPath))!
var rect = NSRect(x: 0, y: 0, width: 208, height: 208)
let cg = pencil.cgImage(forProposedRect: &rect, context: nil, hints: nil)!

func drawPencil(_ target: CGRect) {
    context.saveGState()
    // 그림 좌표는 아래에서 위로라 뒤집어 그림
    context.translateBy(x: 0, y: target.midY * 2)
    context.scaleBy(x: 1, y: -1)
    context.interpolationQuality = .high
    context.draw(cg, in: target)
    context.restoreGState()
}

if layout.hasPrefix("wordmark") {
    // 연필 심볼과 앱 이름을 한 덩어리로 가운데에. 언어는 wordmark-ko·en·ja
    let language = layout.split(separator: "-").last.map(String.init) ?? "ko"
    let name = ["ko": "1일 1일기", "en": "1Day1Diary", "ja": "1日1日記"][language] ?? "1일 1일기"
    let fontSize: CGFloat = 230
    // 스토어 스크린샷과 같은 글꼴. 일본어는 이 글꼴에 한자가 없어 히라기노
    let fontPath = args.count > 5 ? args[5] : ""
    if !fontPath.isEmpty { CTFontManagerRegisterFontsForURL(URL(fileURLWithPath: fontPath) as CFURL, .process, nil) }
    let font = (language == "ja" ? NSFont(name: "HiraginoSans-W6", size: fontSize) : NSFont(name: "ChangwonDangamRound-Regular", size: fontSize))
        ?? NSFont.boldSystemFont(ofSize: fontSize)
    let label = NSAttributedString(string: name, attributes: [
        .font: font, .foregroundColor: NSColor(cgColor: ink)!,
        .strokeColor: NSColor(cgColor: ink)!, .strokeWidth: language == "ja" ? 0 : -3.4
    ])
    let line = CTLineCreateWithAttributedString(label)
    // 눈에 보이는 글자 테두리(기준선 위가 +)
    let glyphs = CTLineGetBoundsWithOptions(line, .useGlyphPathBounds)
    // 영어는 y의 꼬리가 아래로 늘어져 대문자 높이로 세로 가운데를 잡음
    let visualTop = language == "en" ? font.capHeight : glyphs.maxY
    let visualBottom = language == "en" ? 0 : glyphs.minY

    // 연필 그림 상자 안에서 실제로 칠해진 곳(launch-pencil.svg 208 칸 기준)
    let mark: CGFloat = 440
    let pencilVisual = CGRect(x: 56.0 / 208, y: 48.2 / 208, width: 104.7 / 208, height: 109.4 / 208)
    // 기울어진 연필은 지우개 끝이 글자에 가장 가까워 보여 글자 높이의 절반쯤 띄움
    let gap: CGFloat = 100
    let groupWidth = pencilVisual.width * mark + gap + glyphs.width
    let visualLeft = center.x - groupWidth / 2

    // 연필: 칠해진 곳의 가운데를 묶음 세로 가운데에
    let pencilBox = CGRect(
        x: visualLeft - pencilVisual.minX * mark,
        y: center.y - (pencilVisual.midY) * mark,
        width: mark, height: mark
    )
    drawPencil(pencilBox)

    // 글자: 보이는 테두리 왼쪽을 연필 오른쪽 + 간격에, 보이는 높이의 가운데를 세로 가운데에
    let textLeft = visualLeft + pencilVisual.width * mark + gap - glyphs.minX
    let baseline = center.y + (visualTop + visualBottom) / 2
    context.saveGState()
    context.textMatrix = CGAffineTransform(scaleX: 1, y: -1)
    context.textPosition = CGPoint(x: textLeft, y: baseline)
    CTLineDraw(line, context)
    context.restoreGState()
} else {
    let size: CGFloat = layout == "pencil" ? 560 : 400
    // pencil은 연필이 막 쓴 한 줄과 함께 둠. 연필을 오른쪽에, 줄은 연필 끝에서 왼쪽으로
    let pencilCenter = layout == "pencil" ? CGPoint(x: center.x + 300, y: center.y - 40) : center
    let target = CGRect(x: pencilCenter.x - size / 2, y: pencilCenter.y - size / 2, width: size, height: size)
    if layout == "pencil" {
        // 연필 끝(그림 안 0.27, 0.73)에서 끝나는 손글씨 같은 물결 한 줄
        let tip = CGPoint(x: target.minX + 0.27 * size, y: target.minY + 0.73 * size)
        let line = CGMutablePath()
        let start = CGPoint(x: tip.x - 760, y: tip.y + 6)
        line.move(to: start)
        let waves = 4
        let step = (tip.x - start.x) / CGFloat(waves)
        for i in 0..<waves {
            let x0 = start.x + CGFloat(i) * step
            line.addCurve(
                to: CGPoint(x: x0 + step, y: tip.y + (i == waves - 1 ? 0 : 6)),
                control1: CGPoint(x: x0 + step * 0.33, y: tip.y - 34),
                control2: CGPoint(x: x0 + step * 0.66, y: tip.y + 40)
            )
        }
        context.saveGState()
        context.addPath(line)
        context.setStrokeColor(color(0xB892F5))
        context.setLineWidth(20)
        context.setLineCap(.round)
        context.setLineJoin(.round)
        context.strokePath()
        context.restoreGState()
    }
    drawPencil(target)
}

if guide {
    context.setStrokeColor(color(0xE5484D))
    context.setLineWidth(6)
    context.stroke(safe)
}

let image = NSBitmapImageRep(cgImage: context.makeImage()!)
try! image.representation(using: .png, properties: [:])!.write(to: URL(fileURLWithPath: output))
print("저장 \(output)")
