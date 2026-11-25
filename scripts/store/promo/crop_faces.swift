import AppKit
// 기분 시트 스크린샷(iPhone 17 Pro Max, 1320×2868)에서 얼굴 9개를 원으로 오려 PNG로
// 쓰는 법: swift scripts/store/promo/crop_faces.swift <시트 스크린샷.png> docs/store/promo/assets
let a = CommandLine.arguments
let img = NSImage(contentsOfFile: a[1])!.cgImage(forProposedRect: nil, context: nil, hints: nil)!
// 표시 좌표(921 폭) 기준 중심, 지름 92 → 원본 배율 1320/921
let s = 1320.0 / 921.0
let names = ["good","laugh","cool","okay","tired","sad","sick","angry","bad"]
let cx = [118.0, 275, 432, 589, 747, 118, 275, 432, 589]
let cy = [1535.0, 1535, 1535, 1535, 1535, 1728, 1728, 1728, 1728]
let d = 92.0 * s
for i in 0..<9 {
  let r = CGRect(x: cx[i] * s - d / 2, y: cy[i] * s - d / 2, width: d, height: d)
  let crop = img.cropping(to: r)!
  let n = 400
  let ctx = CGContext(data: nil, width: n, height: n, bitsPerComponent: 8, bytesPerRow: 0, space: CGColorSpace(name: CGColorSpace.sRGB)!, bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
  ctx.interpolationQuality = .high
  ctx.addEllipse(in: CGRect(x: 2, y: 2, width: n - 4, height: n - 4)); ctx.clip()
  ctx.draw(crop, in: CGRect(x: 0, y: 0, width: n, height: n))
  try! NSBitmapImageRep(cgImage: ctx.makeImage()!).representation(using: .png, properties: [:])!.write(to: URL(fileURLWithPath: a[2] + "/mood-\(names[i]).png"))
}
