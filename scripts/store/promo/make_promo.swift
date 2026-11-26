import AVFoundation
import AppKit
import CoreText

// 앱 부품을 꺼내 움직이는 세로 소개 영상(1080×1920, 60fps)
// 쓰는 법(저장소 맨 위에서):
//   swiftc -O scripts/store/promo/make_promo.swift -o /tmp/make_promo
//   /tmp/make_promo docs/store/promo/ko.json
// PREVIEW_ONLY=1이면 영상 없이 previewAt 시각의 JPG만 만든다
struct Copy: Decodable {
  let line: String, lineHl: String
  let mood: String, moodHl: String
  let top: String, topHl: String
  let photo: String, photoHl: String
  let chips: [String]
  let typed: String, placeholder: String
  let cardDate: String, cardText: [String]
  let name: String, stores: String
  let hero: [String], heroHl: String
}
struct Config: Decodable { let out: String; let assets: String; let font: String; let icon: String; let home: String; let copy: Copy; let writeEmpty: String; let writeDone: String; let previewAt: [Double]?; let capScale: Double? }
let cfg = try! JSONDecoder().decode(Config.self, from: Data(contentsOf: URL(fileURLWithPath: CommandLine.arguments[1])))
let C = cfg.copy
let W = 1080, H = 1920, FPS = 60
/// 첫 제목 카드 길이. 기능 장면은 그 뒤에 시작
let INTRO = 2.3
/// (장면 시각, 머무는 길이): 다 친 글, 기분을 다 띄운 뒤, 웃음을 고른 뒤, 통계, 사진, 약속 칩. 자막마다 1.5초 넘게 읽히게
let holds: [(Double, Double)] = [(2.04, 0.35), (2.79, 0.8), (3.3, 0.55), (5.25, 0.4), (7.15, 0.5), (8.7, 1.1)]
/// 끝 화면(장면 9.27)을 2초 보여 줌
let TOTAL = INTRO + 11.3 + holds.map(\.1).reduce(0, +)

// 글꼴과 색
CTFontManagerRegisterFontsForURL(URL(fileURLWithPath: cfg.font) as CFURL, .process, nil)
let fontName: String = {
  let d = CTFontManagerCreateFontDescriptorsFromURL(URL(fileURLWithPath: cfg.font) as CFURL) as! [CTFontDescriptor]
  return CTFontDescriptorCopyAttribute(d[0], kCTFontNameAttribute) as! String
}()
func font(_ s: CGFloat) -> CTFont { CTFontCreateWithName(fontName as CFString, s, nil) }
func rgb(_ h: UInt32, _ a: CGFloat = 1) -> CGColor { CGColor(srgbRed: CGFloat((h >> 16) & 255) / 255, green: CGFloat((h >> 8) & 255) / 255, blue: CGFloat(h & 255) / 255, alpha: a) }
let ink: UInt32 = 0x1B1D22, sub: UInt32 = 0x6B7080, brand: UInt32 = 0x6550B9
// 기분 색과 얼굴 경로는 앱과 같게(DiaryColors.mood, DiaryMoodFace, 44 칸 기준)
let moodColor: [String: UInt32] = ["good": 0xFFD4A8, "laugh": 0xFDFA87, "cool": 0xAEE4FF, "okay": 0xDFD4E4, "tired": 0xFCC6F7, "sad": 0xC4F4FE, "sick": 0xBEE9B4, "angry": 0xFFAFB0, "bad": 0x808080]
let dotEyes = "M 17.97 18.7 L 17.97 19.8 M 26.04 18.7 L 26.04 19.8"
let moodPaths: [String: (lines: [String], solids: [String])] = [
  "angry": (["M 14.8 15.8 L 20 18.6 M 29.2 15.8 L 24 18.6", "M 18.2 21.3 L 18.2 21.9 M 25.8 21.3 L 25.8 21.9", "M 16.5 30.5 C 19.5 26.8 24.5 26.8 27.5 30.5"], []),
  "cool": (["M 21 18.2 L 23 18.2", "M 17 27.8 C 20 29.8 24 29.6 27.5 26.6"],
           ["M 13 17 L 21 17 L 21 19 C 21 21.8 19.4 23 17 23 C 14.6 23 13 21.8 13 19 Z", "M 23 17 L 31 17 L 31 19 C 31 21.8 29.4 23 27 23 C 24.6 23 23 21.8 23 19 Z"]),
  "sad": (["M 17.97 18 L 17.97 19 M 26.04 18 L 26.04 19", "M 17.6 31 C 20 28.2 24 28.2 26.4 31"],
          ["M 27.6 21 C 26.3 23.1 25.4 24.4 25.4 25.6 C 25.4 26.9 26.4 27.8 27.6 27.8 C 28.8 27.8 29.8 26.9 29.8 25.6 C 29.8 24.4 28.9 23.1 27.6 21 Z"]),
  "sick": (["M 15.4 17.4 L 19.6 19.2 L 15.4 21 M 28.6 17.4 L 24.4 19.2 L 28.6 21", "M 15 28.6 C 16.6 26.4 18.4 26.4 20 28.4 C 21 29.7 23 29.7 24 28.4 C 25.6 26.4 27.4 26.4 29 28.6"], []),
  "laugh": (["M 16.13 19.43 C 17.35 17.96 18.57 17.96 19.8 19.43 M 24.2 19.43 C 25.42 17.96 26.64 17.96 27.86 19.43"], ["M 14.8 24.4 C 15.4 32 28.6 32 29.2 24.4 Z"]),
  "okay": ([dotEyes, "M 17.42 27.5 L 26.59 27.5"], []),
  "bad": (["M 15.6 20 L 19.8 18 M 28.4 20 L 24.2 18", "M 16 30 C 19.4 26 24.6 26 28 30"], []),
  "good": ([dotEyes, "M 15.6 25.4 C 19.6 30 24.4 30 28.4 25.4"], []),
  "tired": (["M 15.2 19.2 C 16.6 20.9 18.6 20.9 20 19.2 M 24 19.2 C 25.4 20.9 27.4 20.9 28.8 19.2",
             "M 22 26.4 C 23.4 26.4 24.3 27.7 24.3 29.2 C 24.3 30.7 23.4 31.8 22 31.8 C 20.6 31.8 19.7 30.7 19.7 29.2 C 19.7 27.7 20.6 26.4 22 26.4 Z",
             "M 29.2 8.8 L 33.2 8.8 L 29.2 13 L 33.2 13"], []),
]
/// 앱 벡터 경로(M, L, C, Z만 씀)를 CGPath로
func svgPath(_ d: String) -> CGPath {
  let path = CGMutablePath()
  let tokens = d.split(separator: " ").map(String.init)
  var i = 0
  func num() -> CGFloat { defer { i += 1 }; return CGFloat(Double(tokens[i])!) }
  while i < tokens.count {
    let op = tokens[i]; i += 1
    switch op {
    case "M": let x = num(), y = num(); path.move(to: CGPoint(x: x, y: y))
    case "L": let x = num(), y = num(); path.addLine(to: CGPoint(x: x, y: y))
    case "C":
      let x1 = num(), y1 = num(), x2 = num(), y2 = num(), x = num(), y = num()
      path.addCurve(to: CGPoint(x: x, y: y), control1: CGPoint(x: x1, y: y1), control2: CGPoint(x: x2, y: y2))
    case "Z": path.closeSubpath()
    default: break
    }
  }
  return path
}
let moodCGPaths = moodPaths.mapValues { (lines: $0.lines.map(svgPath), solids: $0.solids.map(svgPath)) }
func load(_ p: String) -> CGImage { NSImage(contentsOfFile: p)!.cgImage(forProposedRect: nil, context: nil, hints: nil)! }
let icon = load(cfg.icon), home = load(cfg.home)
let writeCrop = CGRect(x: 0, y: 170, width: 1320, height: 470)
let writeEmpty = load(cfg.writeEmpty).cropping(to: writeCrop)!, writeDone = load(cfg.writeDone).cropping(to: writeCrop)!
let photos = ["sea", "dunes", "clouds"].map { load("\(cfg.assets)/photo-\($0).jpg") }

// 곡선
func clamp(_ x: Double) -> Double { min(1, max(0, x)) }
func easeOut(_ x: Double) -> Double { 1 - pow(1 - clamp(x), 3) }
func easeIn(_ x: Double) -> Double { pow(clamp(x), 3) }
func easeInOut(_ x: Double) -> Double { let t = clamp(x); return t < 0.5 ? 4 * t * t * t : 1 - pow(-2 * t + 2, 3) / 2 }
func back(_ x: Double) -> Double { let t = clamp(x), c1 = 1.9, c3 = c1 + 1; return 1 + c3 * pow(t - 1, 3) + c1 * pow(t - 1, 2) }
func lerp(_ a: Double, _ b: Double, _ t: Double) -> Double { a + (b - a) * t }
func span(_ t: Double, _ a: Double, _ d: Double) -> Double { clamp((t - a) / d) }

// 그리기(위가 0)
var ctx: CGContext!
/// 묶음 투명도. 글자·그림이 setAlpha로 덮어쓰지 않게 곱함
var groupAlpha: CGFloat = 1
func image(_ img: CGImage, _ r: CGRect, alpha: CGFloat = 1, radius: CGFloat = 0) {
  ctx.saveGState(); ctx.setAlpha(alpha * groupAlpha)
  if radius > 0 { ctx.addPath(CGPath(roundedRect: r, cornerWidth: radius, cornerHeight: radius, transform: nil)); ctx.clip() }
  ctx.translateBy(x: r.minX, y: r.maxY); ctx.scaleBy(x: 1, y: -1); ctx.interpolationQuality = .high
  ctx.draw(img, in: CGRect(origin: .zero, size: r.size)); ctx.restoreGState()
}
func face(_ m: String, cx: Double, cy: Double, size: Double, alpha: Double = 1) {
  guard size > 0.5 else { return }
  // 벡터로 그려 어떤 크기에서도 선명하게
  ctx.saveGState(); ctx.setAlpha(CGFloat(alpha) * groupAlpha)
  ctx.setFillColor(rgb(moodColor[m]!)); ctx.fillEllipse(in: CGRect(x: cx - size / 2, y: cy - size / 2, width: size, height: size))
  ctx.translateBy(x: CGFloat(cx - size / 2), y: CGFloat(cy - size / 2)); ctx.scaleBy(x: CGFloat(size / 44), y: CGFloat(size / 44))
  let faceInk = rgb(0x1B1D22)
  for p in moodCGPaths[m]!.solids { ctx.addPath(p); ctx.setFillColor(faceInk); ctx.fillPath() }
  ctx.setStrokeColor(faceInk); ctx.setLineWidth(1.6); ctx.setLineCap(.round); ctx.setLineJoin(.round)
  for p in moodCGPaths[m]!.lines { ctx.addPath(p); ctx.strokePath() }
  ctx.restoreGState()
}
func rrect(_ r: CGRect, _ radius: CGFloat, fill: CGColor, shadow: Bool = false, stroke: CGColor? = nil) {
  ctx.saveGState()
  let p = CGPath(roundedRect: r, cornerWidth: radius, cornerHeight: radius, transform: nil)
  if shadow { ctx.setShadow(offset: CGSize(width: 0, height: 22), blur: 60, color: rgb(0x3A2A8A, 0.14)) }
  ctx.addPath(p); ctx.setFillColor(fill); ctx.fillPath()
  ctx.setShadow(offset: .zero, blur: 0, color: nil)
  if let stroke { ctx.addPath(p); ctx.setStrokeColor(stroke); ctx.setLineWidth(2); ctx.strokePath() }
  ctx.restoreGState()
}
func attr(_ s: String, _ size: CGFloat, _ color: UInt32, hl: String = "", hlColor: UInt32 = brand, bold: Bool = true) -> NSAttributedString {
  let a = NSMutableAttributedString(string: s, attributes: [.font: font(size), .foregroundColor: rgb(color), .strokeColor: rgb(color), .strokeWidth: bold ? -2.6 : 0])
  if !hl.isEmpty, let r = s.range(of: hl) { a.addAttributes([.foregroundColor: rgb(hlColor), .strokeColor: rgb(hlColor)], range: NSRange(r, in: s)) }
  return a
}
func width(_ a: NSAttributedString) -> CGFloat { CGFloat(CTLineGetTypographicBounds(CTLineCreateWithAttributedString(a), nil, nil, nil)) }
/// x는 왼쪽 시작, baseline 기준
func text(_ a: NSAttributedString, x: CGFloat, baseline: CGFloat, alpha: CGFloat = 1) {
  ctx.saveGState(); ctx.setAlpha(alpha * groupAlpha); ctx.translateBy(x: x, y: baseline); ctx.scaleBy(x: 1, y: -1)
  ctx.textPosition = .zero; CTLineDraw(CTLineCreateWithAttributedString(a), ctx); ctx.restoreGState()
}
func centered(_ a: NSAttributedString, cx: CGFloat, baseline: CGFloat, alpha: CGFloat = 1) { text(a, x: cx - width(a) / 2, baseline: baseline, alpha: alpha) }

/// 낱말 하나씩 떠오르는 자막. inAt 등장, outAt 퇴장
let capScale = CGFloat(cfg.capScale ?? 1)
/// 낱말 하나씩 떠오르는 자막. 줄바꿈(\n)이면 두 줄, baseline은 묶음의 가운데 기준
func caption(_ s: String, hl: String, t: Double, inAt: Double, outAt: Double, baseline: CGFloat = 470, color: UInt32 = ink, hlColor: UInt32 = brand, size: CGFloat = 92) {
  let sz = size * capScale
  guard t >= inAt, t < outAt + 0.3 else { return }
  let out = easeIn((t - outAt) / 0.25)
  let lines = s.components(separatedBy: "\n")
  let lh = sz * 1.32
  var order = 0
  for (li, line) in lines.enumerated() {
    let lineBase = baseline + (CGFloat(li) - CGFloat(lines.count - 1) / 2) * lh
    var x = 540 - width(attr(line, sz, color)) / 2
    for w in line.components(separatedBy: " ") {
      let isHl = !hl.isEmpty && hl.components(separatedBy: " ").contains(w)
      let p = easeOut((t - inAt - Double(order) * 0.07) / 0.35)
      let y = lineBase + CGFloat(1 - p) * 36 - CGFloat(out) * 30
      text(attr(w, sz, isHl ? hlColor : color), x: x, baseline: y, alpha: CGFloat(p * (1 - out)))
      x += width(attr(w + " ", sz, color))
      order += 1
    }
  }
}

// 배경: 종이색 위 천천히 흐르는 빛
func paper(_ t: Double) {
  ctx.setFillColor(rgb(0xFBFAFF)); ctx.fill(CGRect(x: 0, y: 0, width: W, height: H))
  let blobs: [(UInt32, CGFloat, Double, Double, Double)] = [(0xDCD2FF, 0.7, 300, 520, 0.4), (0xFFE4D2, 0.55, 820, 1500, 0.33), (0xE8E2FF, 0.6, 760, 900, 0.27)]
  for (c, a, x, y, sp) in blobs {
    let cx = x + sin(t * sp) * 90, cy = y + cos(t * sp * 1.3) * 110
    let g = CGGradient(colorsSpace: CGColorSpace(name: CGColorSpace.sRGB), colors: [rgb(c, a), rgb(c, 0)] as CFArray, locations: [0, 1])!
    ctx.drawRadialGradient(g, startCenter: CGPoint(x: cx, y: cy), startRadius: 0, endCenter: CGPoint(x: cx, y: cy), endRadius: 620, options: [])
  }
}

// 한 달 기분(통계 장면 숫자)
let month: [String?] = ["okay", "laugh", "laugh", "cool", "sad", nil, "laugh", "laugh", "okay", "good", "tired", "cool", "good",
                        "good", nil, "laugh", "cool", "good", "sick", "laugh", nil, "okay", "laugh", "good", "laugh"]
let counts = Dictionary(grouping: month.compactMap { $0 }, by: { $0 }).mapValues { $0.count }.sorted { $0.value > $1.value || ($0.value == $1.value && $0.key < $1.key) }
let total = Double(month.compactMap { $0 }.count)
/// 통계 머리: 가장 많은 기분 얼굴과 비율을 한 줄 가운데로
let topPct = attr("\(counts[0].value * 100 / Int(total))%", 230, brand)
let topFace = 190.0, topGap = 36.0
let topLeft = 540 - (topFace + topGap + Double(width(topPct))) / 2
let topFaceX = topLeft + topFace / 2, topFaceY = 745.0

// 기분 고르기 원 위치
let ring = ["good", "laugh", "cool", "okay", "tired", "sad", "sick", "angry", "bad"]
/// 앱 기분 시트처럼 5개, 4개 두 줄
func ringPos(_ i: Int) -> (Double, Double) {
  i < 5 ? (540 + Double(i - 2) * 172, 1110) : (540 + (Double(i - 5) - 1.5) * 172, 1290)
}

func scenes(_ real: Double) {
  // 머무는 구간에서는 장면 시각을 멈춤. 배경과 떠 있는 얼굴은 실제 시간으로 움직임
  var t = real, drift = 0.0, pulse = 0.0, acc = 0.0
  for (at, len) in holds {
    if real < at + acc { t = real - acc; break }
    if real < at + acc + len {
      t = at
      let k = sin((real - at - acc) / len * .pi)
      if at == 2.79 { drift = k } else if at == 3.3 { pulse = k }
      break
    }
    acc += len; t = real - acc
  }
  ctx.saveGState(); ctx.translateBy(x: 0, y: CGFloat(H)); ctx.scaleBy(x: 1, y: -1)
  paper(real)
  ctx.saveGState(); ctx.translateBy(x: 0, y: 110)

  // 1. 실제 쓰기 화면 윗부분을 카드로(0~3.7)
  if t < 3.7 {
    let pin = back(span(t, 0.05, 0.45))
    let away = easeInOut(span(t, 3.4, 0.25))
    let lift = easeInOut(span(t, 2.05, 0.45))
    let k = 990.0 / 1320.0 * (0.9 + 0.1 * pin)
    let cw = 1320 * k, ch = 470 * k
    let py = 900 - (1 - pin) * 60 - lift * 110 - away * 50
    let r = CGRect(x: 540 - cw / 2, y: py - ch / 2, width: cw, height: ch)
    ctx.saveGState(); groupAlpha = CGFloat(clamp(pin * 1.4) * (1 - away)); ctx.setAlpha(groupAlpha)
    rrect(r, 48, fill: rgb(0xFFFFFF), shadow: true, stroke: rgb(0xE9E4F8))
    image(writeEmpty, r, radius: 48)
    // 기분을 고르면 머리와 칩 줄만 실제 화면(웃음 칩, 저장 켜짐)으로. 본문은 친 글 그대로
    let done = easeInOut(span(t, 3.05, 0.22))
    if done > 0 {
      ctx.saveGState(); ctx.clip(to: CGRect(x: r.minX, y: r.minY, width: r.width, height: 340 * k))
      image(writeDone, r, alpha: CGFloat(done), radius: 48); ctx.restoreGState()
    }
    let perChar = min(0.075, 1.3 / Double(C.typed.count))
    let typed = String(C.typed.prefix(Int(max(0, (t - 0.75) / perChar))))
    let tx = r.minX + 62 * k, base = r.minY + 424 * k
    if !typed.isEmpty {
      ctx.setFillColor(rgb(0xFFFFFF)); ctx.fill(CGRect(x: r.minX + 40 * k, y: r.minY + 352 * k, width: 1240 * k, height: 100 * k))
      text(attr(typed, 50 * k, ink, bold: false), x: tx, baseline: base)
    }
    let caretX = tx + (typed.isEmpty ? 0 : width(attr(typed, 50 * k, ink, bold: false))) + 3
    if t < 3.0 && (Int(t * 2.2) % 2 == 0 || (t > 0.75 && t < 0.75 + Double(C.typed.count) * perChar)) {
      ctx.setFillColor(rgb(brand)); ctx.fill(CGRect(x: caretX, y: base - 44 * k, width: 4, height: 58 * k))
    }
    ctx.restoreGState(); groupAlpha = 1
  }
  caption(C.line, hl: C.lineHl, t: t, inAt: 0.35, outAt: 2.05)

  // 2. 기분 고르기(2.15~4.1)
  caption(C.mood, hl: C.moodHl, t: t, inAt: 2.25, outAt: 3.35)
  if t > 2.15 && t < 4.1 {
    for (i, m) in ring.enumerated() {
      let (x, y) = ringPos(i)
      let pop = back(span(t, 2.2 + Double(i) * 0.03, 0.35))
      if m == "laugh" {
        // 고른 얼굴: 커졌다가 통계 머리 자리로 날아감
        let pick = easeOut(span(t, 3.0, 0.28))
        let fly = easeInOut(span(t, 3.45, 0.65))
        let s0 = 136 * pop * (1 + 0.5 * pick) * (1 + 0.06 * drift + 0.07 * pulse)
        let size = lerp(s0, topFace, fly)
        let cx = lerp(lerp(x, 540, pick), topFaceX, fly), cy = lerp(lerp(y, 1180, pick), topFaceY, fly) - sin(fly * .pi) * 70
        if pick > 0 && fly < 1 {
          ctx.saveGState(); groupAlpha = CGFloat(0.35 * pick * (1 - fly)); ctx.setAlpha(groupAlpha)
          ctx.setFillColor(rgb(moodColor[m]!)); ctx.fillEllipse(in: CGRect(x: cx - size * 0.7, y: cy - size * 0.7, width: size * 1.4, height: size * 1.4)); ctx.restoreGState(); groupAlpha = 1
        }
        face(m, cx: cx, cy: cy, size: size)
      } else {
        let gone = easeIn(span(t, 2.82 + Double(i) * 0.015, 0.18))
        face(m, cx: x, cy: y + sin(real * 2.6 + Double(i)) * 9 * drift, size: 136 * pop * (1 - gone), alpha: 1 - gone)
      }
    }
  }

  // 3. 가장 많은 기분(4.1~6.1). 날아온 얼굴이 그대로 머리가 됨
  caption(C.top, hl: C.topHl, t: t, inAt: 3.75, outAt: 5.7)
  if t >= 4.1 && t < 6.1 {
    let out = easeInOut(span(t, 5.75, 0.3))
    let n = easeOut(span(t, 4.2, 0.9))
    let pct = Int((Double(counts[0].value) / total * 100 * n).rounded())
    ctx.saveGState(); groupAlpha = CGFloat(1 - out); ctx.setAlpha(groupAlpha)
    let rise = CGFloat(1 - easeOut(span(t, 4.1, 0.4))) * 40 + CGFloat(out) * 40
    let fa = easeOut(span(t, 4.1, 0.3))
    let land = 1 + 0.08 * (1 - easeOut(span(t, 4.1, 0.35)))
    face(counts[0].key, cx: topFaceX, cy: topFaceY - Double(out) * 40, size: topFace * land)
    text(attr("\(pct)%", 230, brand), x: CGFloat(topLeft + topFace + topGap), baseline: 820 - rise, alpha: CGFloat(fa))
    for (i, kv) in counts.prefix(5).enumerated() {
      let y = 960 + Double(i) * 150 + out * 40
      let p = easeOut(span(t, 4.25 + Double(i) * 0.08, 0.55))
      face(kv.key, cx: 150, cy: y, size: 100 * back(span(t, 4.2 + Double(i) * 0.08, 0.4)))
      let maxW = 700.0, w = maxW * Double(kv.value) / Double(counts[0].value) * p
      rrect(CGRect(x: 240, y: y - 22, width: max(44, w), height: 44), 22, fill: rgb(moodColor[kv.key]!, CGFloat(min(1, p * 2))))
    }
    ctx.restoreGState(); groupAlpha = 1
  }

  // 4. 사진(6.0~8.15)
  caption(C.photo, hl: C.photoHl, t: t, inAt: 6.02, outAt: 7.6)
  if t > 6.0 && t < 8.15 {
    let out = easeInOut(span(t, 7.65, 0.4))
    let cin = easeOut(span(t, 6.05, 0.45))
    ctx.saveGState(); groupAlpha = CGFloat(cin * (1 - out)); ctx.setAlpha(groupAlpha)
    let card = CGRect(x: 110, y: 1180 + (1 - cin) * 80, width: 860, height: 380)
    rrect(card, 44, fill: rgb(0xFFFFFF), shadow: true, stroke: rgb(0xEEEAF8))
    text(attr(C.cardDate, 38, sub, bold: false), x: card.minX + 56, baseline: card.minY + 96)
    for (i, l) in C.cardText.enumerated() { text(attr(l, 48, ink, bold: false), x: card.minX + 56, baseline: card.minY + 190 + CGFloat(i) * 76) }
    ctx.restoreGState(); groupAlpha = 1
    let spots: [(Double, Double, Double)] = [(300, 880, -9), (540, 820, 2), (790, 890, 9)]
    for (i, img) in photos.enumerated() {
      let p = back(span(t, 6.3 + Double(i) * 0.13, 0.5))
      let (x, y, rot) = spots[i]
      let sx = lerp([-300, 540, 1380][i], x, p), sy = lerp(1500, y, p)
      ctx.saveGState(); groupAlpha = CGFloat(clamp(p * 3) * (1 - out)); ctx.setAlpha(groupAlpha)
      ctx.translateBy(x: CGFloat(sx), y: CGFloat(sy - out * 60)); ctx.rotate(by: CGFloat((rot * p) * .pi / 180))
      let r = CGRect(x: -170, y: -170, width: 340, height: 340)
      rrect(r.insetBy(dx: -12, dy: -12), 40, fill: rgb(0xFFFFFF), shadow: true)
      image(img, r, radius: 30)
      ctx.restoreGState(); groupAlpha = 1
    }
  }

  // 5. 실제 앱과 약속(7.75~9.55)
  if t > 7.75 && t < 9.55 {
    let rise = easeOut(span(t, 7.8, 0.5)), out = easeInOut(span(t, 9.1, 0.35))
    let pw = 720.0, ph = pw * 2868 / 1320
    let r = CGRect(x: 540 - pw / 2, y: 820 + (1 - rise) * 900 + out * 1300, width: pw, height: ph)
    ctx.saveGState(); ctx.setShadow(offset: CGSize(width: 0, height: 30), blur: 80, color: rgb(0x3A2A8A, 0.28))
    rrect(r, 100, fill: rgb(0x000000)); ctx.restoreGState()
    image(home, r, radius: 100)
    ctx.saveGState(); ctx.addPath(CGPath(roundedRect: r, cornerWidth: 100, cornerHeight: 100, transform: nil)); ctx.setStrokeColor(rgb(0x3A3C44)); ctx.setLineWidth(4); ctx.strokePath(); ctx.restoreGState()
    for (i, chip) in C.chips.enumerated() {
      let p = back(span(t, 7.9 + Double(i) * 0.16, 0.42))
      let a = attr(chip, 50, ink)
      // 왼쪽 44 + 확인 표시 46 + 사이 22 + 글 + 오른쪽 52(둥근 끝이라 조금 더)
      let w = width(a) + 164, y = 396 + Double(i) * 132 - out * 40
      let x = 540 - w / 2
      ctx.saveGState(); groupAlpha = CGFloat(clamp(p * 2) * (1 - out)); ctx.setAlpha(groupAlpha)
      ctx.translateBy(x: 540, y: CGFloat(y)); ctx.scaleBy(x: CGFloat(p), y: CGFloat(p)); ctx.translateBy(x: -540, y: CGFloat(-y))
      rrect(CGRect(x: x, y: y - 52, width: w, height: 104), 52, fill: rgb(0xFFFFFF), shadow: true, stroke: rgb(0xE9E4F8))
      // 확인 표시
      ctx.setStrokeColor(rgb(brand)); ctx.setLineWidth(7); ctx.setLineCap(.round); ctx.setLineJoin(.round)
      ctx.move(to: CGPoint(x: x + 44, y: y)); ctx.addLine(to: CGPoint(x: x + 60, y: y + 16)); ctx.addLine(to: CGPoint(x: x + 90, y: y - 16)); ctx.strokePath()
      text(a, x: x + 112, baseline: CGFloat(y) + 18)
      ctx.restoreGState(); groupAlpha = 1
    }
  }

  // 6. 끝 화면(9.27~)
  if t > 9.27 {
    let e = t - 9.27
    let p = back(e / 0.5), a = easeOut(e / 0.3)
    let size = 250 * (0.7 + 0.3 * p)
    let r = CGRect(x: 540 - size / 2, y: 820 - size / 2, width: size, height: size)
    ctx.saveGState(); groupAlpha = CGFloat(a); ctx.setAlpha(groupAlpha); ctx.setShadow(offset: CGSize(width: 0, height: 20), blur: 60, color: rgb(0x3A2A8A, 0.22))
    rrect(r, size * 0.225, fill: rgb(0xFFFFFF)); ctx.restoreGState(); groupAlpha = 1
    image(icon, r, alpha: CGFloat(a), radius: size * 0.225)
    let n = easeOut((e - 0.12) / 0.4), s = easeOut((e - 0.3) / 0.4)
    centered(attr(C.name, 100, ink), cx: 540, baseline: 1090 + CGFloat(1 - n) * 30, alpha: CGFloat(n))
    let st = attr(C.stores, 44, brand)
    let w = width(st) + 80
    let pill = CGRect(x: 540 - w / 2, y: 1170 + (1 - s) * 30, width: w, height: 96)
    ctx.saveGState(); groupAlpha = CGFloat(s); ctx.setAlpha(groupAlpha); rrect(pill, 48, fill: rgb(0xEBE4FF)); ctx.restoreGState(); groupAlpha = 1
    centered(st, cx: 540, baseline: pill.minY + 63, alpha: CGFloat(s))
  }
  ctx.restoreGState(); ctx.restoreGState(); groupAlpha = 1
}

/// 첫 제목 카드: 앱 이름, 세 줄 제목(핵심 말에 형광펜), 아래에서 빼꼼 올라오는 기분 얼굴
func intro(_ e: Double) {
  guard e < INTRO + 0.05 else { return }
  let exit = easeInOut(span(e, INTRO - 0.38, 0.38))
  let x0: CGFloat = 96
  // 앱 이름
  let la = easeOut(span(e, 0.05, 0.35)) * (1 - exit)
  image(icon, CGRect(x: x0, y: 512 - CGFloat(exit) * 60, width: 68, height: 68), alpha: CGFloat(la), radius: 16)
  text(attr(C.name, 42, sub), x: x0 + 88, baseline: 560 - CGFloat(exit) * 60, alpha: CGFloat(la))
  // 세 줄 제목
  let size: CGFloat = 116
  for (i, line) in C.hero.enumerated() {
    let p = easeOut(span(e, 0.18 + Double(i) * 0.12, 0.45))
    let base = 740 + CGFloat(i) * 140 + CGFloat(1 - p) * 40 - CGFloat(exit) * 60
    let a = CGFloat(p * (1 - exit))
    if let r = line.range(of: C.heroHl) {
      let before = String(line[..<r.lowerBound])
      let hx = x0 + width(attr(before, size, ink)), hw = width(attr(C.heroHl, size, ink))
      // 칠하기 전에는 아무것도 보이지 않게, 양옆 여백까지 진행률로 늘림
      let w = (hw + 16) * CGFloat(easeInOut(span(e, 0.75, 0.4)))
      if w > 0 {
        ctx.saveGState(); ctx.setAlpha(a)
        ctx.setFillColor(rgb(0xD9CEFF)); ctx.fill(CGRect(x: hx - 8, y: base - size * 0.5, width: w, height: size * 0.6))
        ctx.restoreGState()
      }
    }
    text(attr(line, size, ink), x: x0, baseline: base, alpha: a)
  }
  // 아래에서 빼꼼 올라오는 기분 얼굴
  let bob = sin(e * 2.4) * 8
  let big = back(span(e, 0.4, 0.6)), small = back(span(e, 0.58, 0.55)), side = back(span(e, 0.72, 0.5))
  // 쿨은 웃음 얼굴 위 오른쪽 가장자리에서 반쯤 빼꼼. 웃음 얼굴 뒤에 그림
  face("cool", cx: 1010 + (1 - side) * 260, cy: 1210 + exit * 700 - bob * 0.6, size: 250)
  face("laugh", cx: 790, cy: 1650 + (1 - big) * 520 + exit * 700 + bob, size: 600)
  face("good", cx: 330, cy: 1820 + (1 - small) * 400 + exit * 700 - bob, size: 300)
}

func render(_ ft: Double) {
  scenes(max(-1, ft - INTRO))
  ctx.saveGState(); ctx.translateBy(x: 0, y: CGFloat(H)); ctx.scaleBy(x: 1, y: -1)
  intro(ft)
  ctx.restoreGState()
}


// MARK: 효과음
// 남의 음원 없이 직접 합성. 시각은 장면 시간표에서 계산(장면 시각을 바꾸면 여기도 함께)
let rate = 44_100.0
/// 배경음과 효과음을 따로 모은 뒤, 효과음이 날 때만 배경음을 낮춰 섞음
final class Bus { var l: [Float], r: [Float]; init() { l = [Float](repeating: 0, count: Int((TOTAL + 1) * rate)); r = l } }
let music = Bus(), sfx = Bus()
/// 배경음을 비켜 줄 효과음 시각
var hits: [Double] = []
/// 장면 시각(머무름 전)을 영상 시각으로
func realTime(_ t: Double) -> Double { INTRO + t + holds.filter { t > $0.0 }.map(\.1).reduce(0, +) }
func addSound(_ bus: Bus, at time: Double, pan: Double = 0, _ gen: (Double) -> Double, length: Double) {
  let start = Int(time * rate), n = Int(length * rate)
  let l = Float(sqrt(0.5 * (1 - pan))), r = Float(sqrt(0.5 * (1 + pan)))
  for i in 0..<n where start + i >= 0 && start + i < bus.l.count {
    let v = Float(gen(Double(i) / rate))
    bus.l[start + i] += v * l; bus.r[start + i] += v * r
  }
}
/// 펠트 피아노 같은 둥근 음. 낮은 배음을 두껍게, 시작은 6ms로 부드럽게
func felt(_ bus: Bus, _ at: Double, freq: Double, gain: Double, decay: Double = 4, pan: Double = 0, length: Double = 1.2) {
  addSound(bus, at: at, pan: pan, { t in
    let w = 2 * .pi * freq * t
    let tone = sin(w) + 0.5 * sin(2 * w) * exp(-t * 2.5) + 0.18 * sin(3 * w) * exp(-t * 6) + 0.15 * sin(w / 2) * exp(-t * 10)
    return tone * exp(-t * decay) * min(1, t / 0.006) * min(1, max(0, (length - t) / 0.08)) * gain
  }, length: length)
}
/// 나무 실로폰(마림바) 같은 짧고 동그란 음. 4배음을 살짝 얹어 나무 느낌
func wood(_ at: Double, freq: Double, gain: Double, decay: Double = 9, pan: Double = 0, duck: Bool = false) {
  if duck { hits.append(at) }
  addSound(sfx, at: at, pan: pan, { t in
    let w = 2 * .pi * freq * t
    return (sin(w) + 0.3 * sin(4 * w) * exp(-t * 28)) * exp(-t * decay) * min(1, t / 0.002) * gain
  }, length: 0.7)
}
/// 뽀글 하고 올라가는 방울 소리. f0에서 f1로 짧게 미끄러짐
func bubble(_ at: Double, from f0: Double, to f1: Double, gain: Double, glide: Double = 0.06, decay: Double = 22, pan: Double = 0, duck: Bool = false) {
  if duck { hits.append(at) }
  let k = log(f1 / f0)
  addSound(sfx, at: at, pan: pan, { t in
    let g = min(t, glide)
    let phase = 2 * .pi * (f0 * glide / k * (exp(k * g / glide) - 1) + f1 * max(0, t - glide))
    return (sin(phase) + 0.2 * sin(2 * phase)) * exp(-t * decay) * min(1, t / 0.002) * gain
  }, length: 0.35)
}
/// 아주 짧은 톡(타이핑)
func tick(_ at: Double, gain: Double = 0.04) {
  addSound(sfx, at: at, { t in (sin(2 * .pi * 2_400 * t) * 0.5 + sin(2 * .pi * 900 * t) * 0.5) * exp(-t * 500) * min(1, t * 6_000) * gain }, length: 0.014)
}
/// 느리게 부풀었다 빠지는 패드. 살짝 어긋난 사인 둘로 따뜻하게
func pad(_ at: Double, freq: Double, gain: Double, length: Double, attack: Double = 0.8) {
  addSound(music, at: at, { t in
    (sin(2 * .pi * freq * 0.9985 * t) + sin(2 * .pi * freq * 1.0015 * t)) * 0.5
      * min(1, t / attack) * min(1, max(0, (length - t) / 0.6)) * gain * fade(at + t)
  }, length: length)
}
func hz(_ midi: Double) -> Double { 440 * pow(2, (midi - 69) / 12) }
/// C장조 5음 음계(도레미솔라)에서 n번째 음. 0 = 가운데 도
func scale(_ n: Int) -> Double {
  let steps = [0, 2, 4, 7, 9]
  return hz(60 + Double(12 * (n / 5) + steps[n % 5]))
}
/// 로고가 뜨는 시각. 배경음은 여기서 멈추고 미-솔-도로 마무리
var endAt: Double { realTime(9.27) + 0.05 }
/// 첫 소리부터 들리되 1초 동안 조금씩 커짐
func fade(_ time: Double) -> Double { min(1, 0.35 + time / 1.5) }

/// 배경음: 장조로 앞으로 나아가는 반 박 아르페지오. 가운데 음역, 둥근 피아노, 박마다 낮은 뿌리음
func movingPiano() {
  // Cadd9 - G6 - Fmaj9 - G7(끝 화면의 도 화음으로 풀림)
  let chords: [(Double, [Double])] = [(48, [60, 62, 64, 67]), (43, [59, 62, 64, 67]), (41, [57, 60, 64, 67]), (43, [59, 62, 65, 67])]
  let bar = 2.0, beat = bar / 4, eighth = beat / 2
  let arp = [0, 1, 2, 3, 2, 1, 2, 3]
  var b = 0
  while Double(b) * bar < endAt {
    let (root, tones) = chords[b % 4], start = Double(b) * bar
    for (i, idx) in arp.enumerated() {
      let at = start + Double(i) * eighth
      if at >= endAt - 0.1 { break }
      felt(music, at, freq: hz(tones[idx]), gain: (i % 2 == 0 ? 0.03 : 0.024) * fade(at), decay: 3.2, pan: i % 2 == 0 ? -0.3 : 0.3, length: 1.0)
    }
    for i in [0, 2] where start + Double(i) * beat < endAt - 0.1 {
      let at = start + Double(i) * beat
      felt(music, at, freq: hz(root), gain: 0.05 * fade(at), decay: 2.5, length: 1.0)
    }
    for note in tones.prefix(3) { pad(start, freq: hz(note), gain: 0.005, length: min(bar + 0.6, endAt + 0.4 - start)) }
    b += 1
  }
}

func addMusic() {
  movingPiano()
  // 마무리: 미-솔-도로 올라가 높은 도에서 맺음. 남은 G7 울림은 비켜 줌
  for (i, m) in [76.0, 79, 84].enumerated() { wood(endAt + Double(i) * 0.09, freq: hz(m), gain: 0.04 + Double(i) * 0.005, decay: i == 2 ? 3.5 : 7, pan: Double(i - 1) * 0.25, duck: i == 0) }
}

func buildSound() {
  addMusic()
  // 첫 제목 카드는 배경음만
  let perChar = min(0.075, 1.3 / Double(C.typed.count))
  for k in 0..<C.typed.count { tick(realTime(0.75 + Double(k) * perChar)) }
  // 끝음은 그때 배경음 화음의 음(Fmaj9 → G7 → C → G → Fmaj9 → 도). 장면 시각이 바뀌면 다시 맞춤
  // 기분이 뜰 때 방울 셋(첫·가운데·끝 얼굴이 튀어 오르는 때). 라·도·파로 맺음
  for (i, m) in [(0, 69.0), (4, 72.0), (8, 77.0)] {
    bubble(realTime(2.2 + Double(i) * 0.03) + 0.1, from: hz(m) / 1.35, to: hz(m), gain: 0.045, pan: Double(i) / 4 - 1)
  }
  // 웃음을 고르면 솔로 뿅 올라가고 시-레-솔이 반짝 굴러감
  bubble(realTime(3.0), from: hz(67), to: hz(79), gain: 0.065, glide: 0.07, decay: 14, duck: true)
  for (i, m) in [71.0, 74, 79].enumerated() { wood(realTime(3.0) + 0.06 + Double(i) * 0.05, freq: hz(m), gain: 0.03 + Double(i) * 0.006, decay: 7, pan: Double(i - 1) * 0.3) }
  // 통계 머리 자리에 살짝 착지
  bubble(realTime(4.1), from: hz(64), to: hz(67), gain: 0.04, decay: 26)
  // 퍼센트가 오르는 만큼 음계를 따라 음도 오름(배경음과 같은 C장조). 숫자와 같은 easeOut이라 갈수록 느려짐
  let pct = Double(counts[0].value) / total * 100
  var last = -1.0
  for k in stride(from: 3.0, through: pct, by: 3) {
    let at = realTime(4.2 + 0.9 * (1 - pow(1 - k / pct, 1.0 / 3)))
    if at - last < 0.05 { continue }
    last = at
    wood(at, freq: scale(3 + Int((5 * k / pct).rounded())), gain: 0.022 + 0.012 * k / pct, decay: 18)
  }
  wood(realTime(5.1), freq: hz(79), gain: 0.045, decay: 7)
  // 사진은 작게 톡톡. 솔·시·레
  for (i, m) in [67.0, 71, 74].enumerated() { bubble(realTime(6.3 + Double(i) * 0.13) + 0.2, from: hz(m) / 1.28, to: hz(m), gain: 0.03, decay: 28, pan: Double(i - 1) * 0.5) }
  // 약속 칩은 아주 가볍게
  for (i, m) in [67.0, 72, 76].enumerated() { wood(realTime(7.9 + Double(i) * 0.16) + 0.12, freq: hz(m), gain: 0.025, decay: 12) }
  // 효과음 앞뒤로 배경음을 절반쯤 낮췄다가 0.4초에 걸쳐 되돌림
  var duck = [Float](repeating: 1, count: music.l.count)
  for h in hits {
    let s = Int((h - 0.03) * rate)
    for i in 0..<Int(0.45 * rate) where s + i >= 0 && s + i < duck.count {
      let x = Double(i) / rate
      let d = x < 0.03 ? x / 0.03 : max(0, 1 - (x - 0.05) / 0.4)
      duck[s + i] = min(duck[s + i], Float(1 - 0.45 * d))
    }
  }
  // 효과음이 배경음보다 확실히 위에 오게 비율을 두고 전체 크기를 맞춤. 끝 0.4초는 서서히 닫음
  let bed: Float = 2.0, fx: Float = 4.8
  let end = Int(TOTAL * rate), tail = Int(0.4 * rate)
  for i in 0..<music.l.count {
    let close = Float(min(1, max(0, Double(end - i) / Double(tail))))
    music.l[i] = tanhf(music.l[i] * duck[i] * bed + sfx.l[i] * fx) * 0.95 * close
    music.r[i] = tanhf(music.r[i] * duck[i] * bed + sfx.r[i] * fx) * 0.95 * close
  }
}

/// 그림은 그대로 옮기고 효과음(AAC)을 붙여 새 파일로
func muxWithSound(video: URL, out: URL) {
  buildSound()
  try? FileManager.default.removeItem(at: out)
  let asset = AVURLAsset(url: video)
  let track = asset.tracks(withMediaType: .video)[0]
  let reader = try! AVAssetReader(asset: asset)
  let vOut = AVAssetReaderTrackOutput(track: track, outputSettings: nil)
  reader.add(vOut); reader.startReading()
  let w = try! AVAssetWriter(outputURL: out, fileType: .mp4)
  let vIn = AVAssetWriterInput(mediaType: .video, outputSettings: nil, sourceFormatHint: (track.formatDescriptions.first as! CMFormatDescription))
  let aIn = AVAssetWriterInput(mediaType: .audio, outputSettings: [AVFormatIDKey: kAudioFormatMPEG4AAC, AVSampleRateKey: rate, AVNumberOfChannelsKey: 2, AVEncoderBitRateKey: 192_000])
  vIn.expectsMediaDataInRealTime = false; aIn.expectsMediaDataInRealTime = false
  w.add(vIn); w.add(aIn); w.startWriting(); w.startSession(atSourceTime: .zero)
  var asbd = AudioStreamBasicDescription(mSampleRate: rate, mFormatID: kAudioFormatLinearPCM, mFormatFlags: kAudioFormatFlagIsFloat | kAudioFormatFlagIsPacked,
                                         mBytesPerPacket: 8, mFramesPerPacket: 1, mBytesPerFrame: 8, mChannelsPerFrame: 2, mBitsPerChannel: 32, mReserved: 0)
  var fmt: CMAudioFormatDescription?
  CMAudioFormatDescriptionCreate(allocator: nil, asbd: &asbd, layoutSize: 0, layout: nil, magicCookieSize: 0, magicCookie: nil, extensions: nil, formatDescriptionOut: &fmt)
  let total = Int(TOTAL * rate), chunk = 4_096
  var pos = 0
  let group = DispatchGroup()
  group.enter(); group.enter()
  vIn.requestMediaDataWhenReady(on: DispatchQueue(label: "v")) {
    while vIn.isReadyForMoreMediaData {
      if let b = vOut.copyNextSampleBuffer() { vIn.append(b) } else { vIn.markAsFinished(); group.leave(); return }
    }
  }
  aIn.requestMediaDataWhenReady(on: DispatchQueue(label: "a")) {
    while aIn.isReadyForMoreMediaData {
      if pos >= total { aIn.markAsFinished(); group.leave(); return }
      let n = min(chunk, total - pos)
      var data = [Float](repeating: 0, count: n * 2)
      for i in 0..<n { data[i * 2] = music.l[pos + i]; data[i * 2 + 1] = music.r[pos + i] }
      var block: CMBlockBuffer?
      let bytes = n * 8
      CMBlockBufferCreateWithMemoryBlock(allocator: nil, memoryBlock: nil, blockLength: bytes, blockAllocator: nil, customBlockSource: nil, offsetToData: 0, dataLength: bytes, flags: 0, blockBufferOut: &block)
      data.withUnsafeBytes { _ = CMBlockBufferReplaceDataBytes(with: $0.baseAddress!, blockBuffer: block!, offsetIntoDestination: 0, dataLength: bytes) }
      var sb: CMSampleBuffer?
      CMAudioSampleBufferCreateReadyWithPacketDescriptions(allocator: nil, dataBuffer: block!, formatDescription: fmt!, sampleCount: n,
                                                           presentationTimeStamp: CMTime(value: CMTimeValue(pos), timescale: CMTimeScale(rate)), packetDescriptions: nil, sampleBufferOut: &sb)
      aIn.append(sb!)
      pos += n
    }
  }
  group.wait()
  let done = DispatchSemaphore(value: 0)
  w.finishWriting { done.signal() }
  done.wait()
  if w.status != .completed { print("소리 합치기 실패", w.error as Any) }
}

// 쓰기
let outURL = URL(fileURLWithPath: cfg.out)
// 그림만 먼저 쓰고, 끝에 효과음을 합쳐 cfg.out으로
let videoURL = URL(fileURLWithPath: cfg.out + ".video.mp4")
try? FileManager.default.removeItem(at: videoURL)
let writer = try! AVAssetWriter(outputURL: videoURL, fileType: .mp4)
let input = AVAssetWriterInput(mediaType: .video, outputSettings: [
  AVVideoCodecKey: AVVideoCodecType.h264, AVVideoWidthKey: W, AVVideoHeightKey: H,
  AVVideoCompressionPropertiesKey: [AVVideoAverageBitRateKey: 7_500_000, AVVideoProfileLevelKey: AVVideoProfileLevelH264HighAutoLevel, AVVideoExpectedSourceFrameRateKey: FPS, AVVideoMaxKeyFrameIntervalKey: FPS],
  AVVideoColorPropertiesKey: [AVVideoColorPrimariesKey: AVVideoColorPrimaries_ITU_R_709_2, AVVideoTransferFunctionKey: AVVideoTransferFunction_ITU_R_709_2, AVVideoYCbCrMatrixKey: AVVideoYCbCrMatrix_ITU_R_709_2],
])
let adaptor = AVAssetWriterInputPixelBufferAdaptor(assetWriterInput: input, sourcePixelBufferAttributes: [kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32BGRA, kCVPixelBufferWidthKey as String: W, kCVPixelBufferHeightKey as String: H])
writer.add(input); writer.startWriting(); writer.startSession(atSourceTime: .zero)
let onlyPreview = ProcessInfo.processInfo.environment["PREVIEW_ONLY"] != nil
let previews = Set((cfg.previewAt ?? []).map { Int(($0 * Double(FPS)).rounded()) })
let space = CGColorSpace(name: CGColorSpace.sRGB)!
for f in 0..<Int(TOTAL * Double(FPS)) {
  if onlyPreview && !previews.contains(f) { continue }
  while !input.isReadyForMoreMediaData { usleep(2000) }
  var pb: CVPixelBuffer?
  CVPixelBufferPoolCreatePixelBuffer(nil, adaptor.pixelBufferPool!, &pb)
  let px = pb!
  CVPixelBufferLockBaseAddress(px, [])
  ctx = CGContext(data: CVPixelBufferGetBaseAddress(px), width: W, height: H, bitsPerComponent: 8, bytesPerRow: CVPixelBufferGetBytesPerRow(px), space: space, bitmapInfo: CGImageAlphaInfo.premultipliedFirst.rawValue | CGBitmapInfo.byteOrder32Little.rawValue)!
  render(Double(f) / Double(FPS))
  if previews.contains(f), let img = ctx.makeImage() {
    try! NSBitmapImageRep(cgImage: img).representation(using: .jpeg, properties: [.compressionFactor: 0.85])!.write(to: URL(fileURLWithPath: cfg.out + String(format: ".%05.2f.jpg", Double(f) / Double(FPS))))
  }
  CVPixelBufferUnlockBaseAddress(px, [])
  if !onlyPreview { adaptor.append(px, withPresentationTime: CMTime(value: CMTimeValue(f), timescale: CMTimeScale(FPS))) }
}
input.markAsFinished()
let sem = DispatchSemaphore(value: 0)
writer.finishWriting { sem.signal() }
sem.wait()
if onlyPreview { try? FileManager.default.removeItem(at: videoURL); exit(0) }
muxWithSound(video: videoURL, out: outURL)
try? FileManager.default.removeItem(at: videoURL)
print("done", cfg.out, writer.status.rawValue)
