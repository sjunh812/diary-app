// 시뮬레이터 녹화에서 한 프레임을 JPG로 꺼냄(쓰기 화면 카드, 다크 홈 재료)
// 쓰는 법: swift scripts/store/promo/grab_frame.swift <녹화.mp4> <초> <저장.jpg>
import AVFoundation
import AppKit
import VideoToolbox
let asset = AVURLAsset(url: URL(fileURLWithPath: CommandLine.arguments[1]))
let track = asset.tracks(withMediaType: .video)[0]
let r = try! AVAssetReader(asset: asset)
let o = AVAssetReaderTrackOutput(track: track, outputSettings: [kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32BGRA])
r.timeRange = CMTimeRange(start: CMTime(seconds: Double(CommandLine.arguments[2])!, preferredTimescale: 600), end: CMTime(seconds: 100, preferredTimescale: 600))
r.add(o); r.startReading()
let b = o.copyNextSampleBuffer()!
var img: CGImage?
VTCreateCGImageFromCVPixelBuffer(CMSampleBufferGetImageBuffer(b)!, options: nil, imageOut: &img)
try! NSBitmapImageRep(cgImage: img!).representation(using: .jpeg, properties: [:])!.write(to: URL(fileURLWithPath: CommandLine.arguments[3]))
