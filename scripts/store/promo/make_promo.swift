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
  let grid: String, gridHl: String
  let top: String, topHl: String
  let photo: String, photoHl: String
  let color: String
  let chips: [String]
  let typed: String, placeholder: String
  let month: String, weekdays: [String]
  let cardDate: String, cardText: [String]
  let name: String, stores: String
}
struct Config: Decodable { let out: String; let assets: String; let font: String; let icon: String; let home: String; let copy: Copy; let writeEmpty: String; let writeDone: String; let previewAt: [Double]?; let capScale: Double? }
let cfg = try! JSONDecoder().decode(Config.self, from: Data(contentsOf: URL(fileURLWithPath: CommandLine.arguments[1])))
let C = cfg.copy
let W = 1080, H = 1920, FPS = 60
let TOTAL = 16.0 + [0.8, 0.55, 0.4, 0.35, 0.9].reduce(0, +)
/// 기분 고르기에서 읽을 시간을 더 주는 구간
/// (장면 시각, 머무는 길이): 기분을 다 띄운 뒤, 웃음을 고른 뒤
let holds: [(Double, Double)] = [(2.79, 0.8), (3.3, 0.55), (8.0, 0.4), (9.9, 0.35), (13.75, 0.9)]

// 글꼴과 색
CTFontManagerRegisterFontsForURL(URL(fileURLWithPath: cfg.font) as CFURL, .process, nil)
let fontName: String = {
  let d = CTFontManagerCreateFontDescriptorsFromURL(URL(fileURLWithPath: cfg.font) as CFURL) as! [CTFontDescriptor]
  return CTFontDescriptorCopyAttribute(d[0], kCTFontNameAttribute) as! String
}()
func font(_ s: CGFloat) -> CTFont { CTFontCreateWithName(fontName as CFString, s, nil) }
func rgb(_ h: UInt32, _ a: CGFloat = 1) -> CGColor { CGColor(srgbRed: CGFloat((h >> 16) & 255) / 255, green: CGFloat((h >> 8) & 255) / 255, blue: CGFloat(h & 255) / 255, alpha: a) }
let ink: UInt32 = 0x1B1D22, sub: UInt32 = 0x6B7080, brand: UInt32 = 0x6550B9, dark: UInt32 = 0x16171B
let themes: [UInt32] = [0x6E56CF, 0x88569D, 0xAA4A60, 0xA2552C, 0x407552, 0x406E96, 0x2B2D33]
let moodColor: [String: UInt32] = ["good": 0xFFD3A5, "laugh": 0xFBF68C, "cool": 0xA9E0FA, "okay": 0xDDD5E5, "tired": 0xF9C3F1, "sad": 0xC6F1FA, "sick": 0xC2E8B8, "angry": 0xFFA9A9, "bad": 0x8A8A8A]
func load(_ p: String) -> CGImage { NSImage(contentsOfFile: p)!.cgImage(forProposedRect: nil, context: nil, hints: nil)! }
var faces: [String: CGImage] = [:]
for m in moodColor.keys { faces[m] = load("\(cfg.assets)/mood-\(m).png") }
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
  image(faces[m]!, CGRect(x: cx - size / 2, y: cy - size / 2, width: size, height: size), alpha: CGFloat(alpha))
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

// 한 달 기분(11월 1일은 일요일, 25일이 오늘)
let month: [String?] = ["okay", "laugh", "laugh", "cool", "sad", nil, "laugh", "laugh", "okay", "good", "tired", "cool", "good",
                        "good", nil, "laugh", "cool", "good", "sick", "laugh", nil, "okay", "laugh", "good", "laugh"]
let cell = 136.0, gx = 64.0, gy = 820.0, rowStep = 172.0
func cellCenter(_ day: Int) -> (Double, Double) { let i = day - 1; return (gx + Double(i % 7) * cell + cell / 2, gy + Double(i / 7) * rowStep + 100) }

// 기분 고르기 원 위치
let ring = ["good", "laugh", "cool", "okay", "tired", "sad", "sick", "angry", "bad"]
/// 앱 기분 시트처럼 5개, 4개 두 줄
func ringPos(_ i: Int) -> (Double, Double) {
  i < 5 ? (540 + Double(i - 2) * 172, 1110) : (540 + (Double(i - 5) - 1.5) * 172, 1290)
}

func render(_ real: Double) {
  // 기분 고르기 뒤의 모든 장면을 holdLen만큼 미룸. 배경과 떠 있는 얼굴은 실제 시간으로 움직임
  var t = real, drift = 0.0, pulse = 0.0, acc = 0.0
  for (n, (at, len)) in holds.enumerated() {
    if real < at + acc { t = real - acc; break }
    if real < at + acc + len {
      t = at
      let k = sin((real - at - acc) / len * .pi)
      if n == 0 { drift = k } else { pulse = k }
      break
    }
    acc += len; t = real - acc
  }
  ctx.saveGState(); ctx.translateBy(x: 0, y: CGFloat(H)); ctx.scaleBy(x: 1, y: -1)
  paper(real)
  ctx.saveGState(); ctx.translateBy(x: 0, y: 110)

  // 1. 실제 쓰기 화면 윗부분을 카드로(0~4)
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

  // 2. 기분 고르기(2.1~4.4)
  caption(C.mood, hl: C.moodHl, t: t, inAt: 2.25, outAt: 3.35)
  if t > 2.15 && t < 4.3 {
    for (i, m) in ring.enumerated() {
      let (x, y) = ringPos(i)
      let pop = back(span(t, 2.2 + Double(i) * 0.03, 0.35))
      if m == "laugh" {
        // 고른 얼굴: 커졌다가 오늘 칸으로 날아감
        let pick = easeOut(span(t, 3.0, 0.28))
        let fly = easeInOut(span(t, 3.5, 0.7))
        let (tx, ty) = cellCenter(25)
        let s0 = 136 * pop * (1 + 0.5 * pick) * (1 + 0.06 * drift + 0.07 * pulse)
        let size = lerp(s0, 92, fly)
        let cx = lerp(lerp(x, 540, pick), tx, fly), cy = lerp(lerp(y, 1180, pick), ty + 20, fly) - sin(fly * .pi) * 160
        if pick > 0 && fly < 1 {
          ctx.saveGState(); groupAlpha = CGFloat(0.35 * pick * (1 - fly)); ctx.setAlpha(groupAlpha)
          ctx.setFillColor(rgb(moodColor[m]!)); ctx.fillEllipse(in: CGRect(x: cx - size * 0.7, y: cy - size * 0.7, width: size * 1.4, height: size * 1.4)); ctx.restoreGState(); groupAlpha = 1
        }
        if t < 4.2 { face(m, cx: cx, cy: cy, size: size) }
      } else {
        let gone = easeIn(span(t, 2.82 + Double(i) * 0.015, 0.18))
        face(m, cx: x, cy: y + sin(real * 2.6 + Double(i)) * 9 * drift, size: 136 * pop * (1 - gone), alpha: 1 - gone)
      }
    }
  }

  // 3. 달력(4.0~6.8)
  caption(C.grid, hl: C.gridHl, t: t, inAt: 3.65, outAt: 6.35)
  if t > 3.62 && t < 7.1 {
    let gin = easeOut(span(t, 3.62, 0.4)), gout = easeInOut(span(t, 6.4, 0.35))
    let zoom = 1 + 0.035 * easeInOut(span(t, 3.65, 3.0))
    ctx.saveGState()
    ctx.translateBy(x: 540, y: 1100); ctx.scaleBy(x: CGFloat(zoom * (1 - 0.1 * gout)), y: CGFloat(zoom * (1 - 0.1 * gout))); ctx.translateBy(x: -540, y: -1100)
    groupAlpha = CGFloat(gin * (1 - gout)); ctx.setAlpha(groupAlpha)
    text(attr(C.month, 64, ink), x: gx + 16, baseline: gy - 130)
    for (i, w) in C.weekdays.enumerated() {
      let c: UInt32 = i == 0 ? 0xE5484D : (i == 6 ? 0x3E63DD : sub)
      centered(attr(w, 34, c, bold: false), cx: CGFloat(gx + Double(i) * cell + cell / 2), baseline: CGFloat(gy - 40))
    }
    for d in 1...30 {
      let (cx, cy) = cellCenter(d)
      let row = (d - 1) / 7, col = (d - 1) % 7
      let appear = easeOut(span(t, 3.6 + Double(row + col) * 0.04, 0.3))
      let isToday = d == 25
      // 오늘 칸: 앱처럼 연한 칸 위에 채운 원 숫자
      if isToday {
        // 숫자 원 위와 얼굴 아래 여백을 14로 같게
        rrect(CGRect(x: cx - cell / 2 + 6, y: cy - 96, width: cell - 12, height: 168), 30, fill: rgb(0xEBE4FF, CGFloat(appear)))
        ctx.setFillColor(rgb(brand, CGFloat(appear))); ctx.fillEllipse(in: CGRect(x: cx - 27, y: cy - 82, width: 54, height: 54))
      }
      let numColor: UInt32 = isToday ? 0xFFFFFF : (col == 0 ? 0xE5484D : (col == 6 ? 0x3E63DD : (d > 25 ? 0xB8BBC4 : 0x3A3D45)))
      centered(attr("\(d)", 34, numColor, bold: isToday), cx: CGFloat(cx), baseline: CGFloat(cy - 43), alpha: CGFloat(appear))
      if d <= 24, let m = month[d - 1] {
        let p = back(span(t, 3.8 + Double(row + col) * 0.055, 0.38))
        face(m, cx: cx, cy: cy + 20, size: 76 * p)
      }
      if isToday && t >= 4.2 {
        let land = back(span(t, 4.2, 0.32))
        face("laugh", cx: cx, cy: cy + 20, size: 76 * (0.8 + 0.2 * land) + 16 * (1 - land))
      }
    }
    ctx.restoreGState(); groupAlpha = 1
  }

  // 4. 가장 많은 기분(6.7~8.9)
  caption(C.top, hl: C.topHl, t: t, inAt: 6.75, outAt: 8.45)
  if t > 6.6 && t < 9.2 {
    let counts = Dictionary(grouping: month.compactMap { $0 }, by: { $0 }).mapValues { $0.count }.sorted { $0.value > $1.value || ($0.value == $1.value && $0.key < $1.key) }
    let total = Double(month.compactMap { $0 }.count)
    let out = easeInOut(span(t, 8.5, 0.3))
    let n = easeOut(span(t, 6.95, 0.9))
    let pct = Int((Double(counts[0].value) / total * 100 * n).rounded())
    ctx.saveGState(); groupAlpha = CGFloat(1 - out); ctx.setAlpha(groupAlpha)
    // 가장 많은 기분 얼굴과 비율을 한 줄로
    let big = attr("\(counts[0].value * 100 / Int(total))%", 230, brand)
    let shown = attr("\(pct)%", 230, brand)
    let bw = width(big), fs = 190.0, gap = 36.0
    let left = 540 - (fs + gap + Double(bw)) / 2
    let rise = CGFloat(1 - easeOut(span(t, 6.85, 0.4))) * 40 + CGFloat(out) * 40
    let fa = easeOut(span(t, 6.85, 0.3))
    face(counts[0].key, cx: left + fs / 2, cy: 745 - Double(rise), size: fs * back(span(t, 6.85, 0.45)), alpha: fa)
    text(shown, x: CGFloat(left + fs + gap), baseline: 820 - rise, alpha: CGFloat(fa))
    for (i, kv) in counts.prefix(5).enumerated() {
      let y = 960 + Double(i) * 150 + out * 40
      let p = easeOut(span(t, 7.0 + Double(i) * 0.08, 0.55))
      face(kv.key, cx: 150, cy: y, size: 100 * back(span(t, 6.95 + Double(i) * 0.08, 0.4)))
      let maxW = 700.0, w = maxW * Double(kv.value) / Double(counts[0].value) * p
      rrect(CGRect(x: 240, y: y - 22, width: max(44, w), height: 44), 22, fill: rgb(moodColor[kv.key]!, CGFloat(min(1, p * 2))))
    }
    ctx.restoreGState(); groupAlpha = 1
  }

  // 5. 사진(8.8~10.7)
  caption(C.photo, hl: C.photoHl, t: t, inAt: 8.9, outAt: 10.35)
  if t > 8.75 && t < 10.9 {
    let out = easeInOut(span(t, 10.4, 0.4))
    let cin = easeOut(span(t, 8.95, 0.45))
    ctx.saveGState(); groupAlpha = CGFloat(cin * (1 - out)); ctx.setAlpha(groupAlpha)
    let card = CGRect(x: 110, y: 1180 + (1 - cin) * 80, width: 860, height: 380)
    rrect(card, 44, fill: rgb(0xFFFFFF), shadow: true, stroke: rgb(0xEEEAF8))
    text(attr(C.cardDate, 38, sub, bold: false), x: card.minX + 56, baseline: card.minY + 96)
    for (i, l) in C.cardText.enumerated() { text(attr(l, 48, ink, bold: false), x: card.minX + 56, baseline: card.minY + 190 + CGFloat(i) * 76) }
    ctx.restoreGState(); groupAlpha = 1
    let spots: [(Double, Double, Double)] = [(300, 880, -9), (540, 820, 2), (790, 890, 9)]
    for (i, img) in photos.enumerated() {
      let p = back(span(t, 9.05 + Double(i) * 0.13, 0.5))
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

  ctx.restoreGState(); groupAlpha = 1

  // 6. 테마 색으로 덮기(10.6~12.7)
  if t > 10.6 {
    // 흑백 차례는 다크 검정으로 덮어 어두운 색이 두 번 바뀌지 않게
    let steps = Array(themes.dropLast()) + [dark]
    for (i, c) in steps.enumerated() {
      let start = 10.6 + Double(i) * 0.28
      let p = easeInOut(span(t, start, 0.34))
      if p <= 0 { continue }
      let r = p * 1250
      ctx.setFillColor(rgb(c)); ctx.fillEllipse(in: CGRect(x: 540 - r, y: 1060 - r, width: r * 2, height: r * 2))
    }
  }
  caption(C.color, hl: "", t: t, inAt: 10.75, outAt: 12.7, baseline: 880, color: 0xFFFFFF)
  if t > 10.7 && t < 13.0 {
    // 고른 색 점이 차례로 커짐
    let out = easeIn(span(t, 12.65, 0.3))
    for (i, c) in themes.enumerated() {
      let x = 540 + (Double(i) - 3) * 118
      let p = back(span(t, 10.8 + Double(i) * 0.03, 0.4))
      // 지금 덮는 색 점만 부드럽게 커짐
      let k = easeInOut(1 - min(1, abs((t - 10.6) / 0.28 - Double(i) - 0.6) / 0.8))
      let s = 74 * p * (1 + 0.28 * k) * (1 - out)
      ctx.setFillColor(rgb(c)); ctx.fillEllipse(in: CGRect(x: x - s / 2, y: 1060 - s / 2, width: s, height: s))
      ctx.setStrokeColor(rgb(0xFFFFFF, CGFloat(0.55 + 0.45 * k))); ctx.setLineWidth(CGFloat(4 + 3 * k))
      ctx.strokeEllipse(in: CGRect(x: x - s / 2, y: 1060 - s / 2, width: s, height: s))
    }
  }

  // 7. 실제 앱과 약속(12.6~14.3)
  if t > 12.8 && t < 14.6 {
    let rise = easeOut(span(t, 12.85, 0.5)), out = easeInOut(span(t, 14.15, 0.35))
    let pw = 720.0, ph = pw * 2868 / 1320
    let r = CGRect(x: 540 - pw / 2, y: 820 + (1 - rise) * 900 + out * 1300, width: pw, height: ph)
    ctx.saveGState(); ctx.setShadow(offset: CGSize(width: 0, height: 30), blur: 80, color: rgb(0x000000, 0.5))
    rrect(r, 100, fill: rgb(0x000000)); ctx.restoreGState(); groupAlpha = 1
    image(home, r, radius: 100)
    ctx.saveGState(); ctx.addPath(CGPath(roundedRect: r, cornerWidth: 100, cornerHeight: 100, transform: nil)); ctx.setStrokeColor(rgb(0x3A3C44)); ctx.setLineWidth(4); ctx.strokePath(); ctx.restoreGState(); groupAlpha = 1
    for (i, chip) in C.chips.enumerated() {
      let p = back(span(t, 12.95 + Double(i) * 0.16, 0.42))
      let a = attr(chip, 50, 0xFFFFFF)
      let w = width(a) + 136, y = 400 + Double(i) * 126 - out * 40
      let x = 540 - w / 2
      ctx.saveGState(); groupAlpha = CGFloat(clamp(p * 2) * (1 - out)); ctx.setAlpha(groupAlpha)
      ctx.translateBy(x: 540, y: CGFloat(y)); ctx.scaleBy(x: CGFloat(p), y: CGFloat(p)); ctx.translateBy(x: -540, y: CGFloat(-y))
      rrect(CGRect(x: x, y: y - 52, width: w, height: 104), 52, fill: rgb(0x2A2C35), stroke: rgb(0x3D3F4A))
      // 확인 표시
      ctx.setStrokeColor(rgb(0xBAA7FF)); ctx.setLineWidth(7); ctx.setLineCap(.round); ctx.setLineJoin(.round)
      ctx.move(to: CGPoint(x: x + 46, y: y)); ctx.addLine(to: CGPoint(x: x + 62, y: y + 16)); ctx.addLine(to: CGPoint(x: x + 92, y: y - 16)); ctx.strokePath()
      text(a, x: x + 110, baseline: CGFloat(y) + 18)
      ctx.restoreGState(); groupAlpha = 1
    }
  }

  // 8. 끝 화면(14.2~16)
  if t > 14.32 {
    let e = t - 14.32
    let p = back(e / 0.5), a = easeOut(e / 0.3)
    let size = 250 * (0.7 + 0.3 * p)
    let r = CGRect(x: 540 - size / 2, y: 820 - size / 2, width: size, height: size)
    ctx.saveGState(); groupAlpha = CGFloat(a); ctx.setAlpha(groupAlpha); ctx.setShadow(offset: CGSize(width: 0, height: 20), blur: 60, color: rgb(0x000000, 0.45))
    rrect(r, size * 0.225, fill: rgb(0xFFFFFF)); ctx.restoreGState(); groupAlpha = 1
    image(icon, r, alpha: CGFloat(a), radius: size * 0.225)
    let n = easeOut((e - 0.12) / 0.4), s = easeOut((e - 0.3) / 0.4)
    centered(attr(C.name, 100, 0xFFFFFF), cx: 540, baseline: 1090 + CGFloat(1 - n) * 30, alpha: CGFloat(n))
    let st = attr(C.stores, 44, 0xBAA7FF)
    let w = width(st) + 80
    let pill = CGRect(x: 540 - w / 2, y: 1170 + (1 - s) * 30, width: w, height: 96)
    ctx.saveGState(); groupAlpha = CGFloat(s); ctx.setAlpha(groupAlpha); rrect(pill, 48, fill: rgb(0x2A2440)); ctx.restoreGState(); groupAlpha = 1
    centered(st, cx: 540, baseline: pill.minY + 63, alpha: CGFloat(s))
  }
  ctx.restoreGState(); groupAlpha = 1
}

// 쓰기
let outURL = URL(fileURLWithPath: cfg.out)
try? FileManager.default.removeItem(at: outURL)
let writer = try! AVAssetWriter(outputURL: outURL, fileType: .mp4)
let input = AVAssetWriterInput(mediaType: .video, outputSettings: [
  AVVideoCodecKey: AVVideoCodecType.h264, AVVideoWidthKey: W, AVVideoHeightKey: H,
  AVVideoCompressionPropertiesKey: [AVVideoAverageBitRateKey: 14_000_000, AVVideoProfileLevelKey: AVVideoProfileLevelH264HighAutoLevel, AVVideoExpectedSourceFrameRateKey: FPS, AVVideoMaxKeyFrameIntervalKey: FPS],
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
print("done", cfg.out, writer.status.rawValue)
