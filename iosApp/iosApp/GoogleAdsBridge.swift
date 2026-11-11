import GoogleMobileAds
import Shared
import UIKit

/// 구글 광고(AdMob) 구현. Android AdMobPlayer와 같은 규칙
/// 광고 요청에 일기 내용을 넣지 않음(헌법 II)
final class GoogleAdsBridge: NSObject, IosAdBridge {
    /// 구글 공개 시험 광고 단위. 개발 빌드는 늘 이것만 받음(실제 광고를 눌러 시험하지 않음)
    private enum TestUnit {
        static let native = "ca-app-pub-3940256099942544/3986624511"
        static let interstitial = "ca-app-pub-3940256099942544/4411468910"
        static let rewarded = "ca-app-pub-3940256099942544/1712485313"
    }

    /// 들고 있는 네이티브 광고 수의 끝. 메모리를 많이 써 오래된 것부터 치움
    private static let maxNatives = 5
    /// 받은 전면 광고는 1시간이 지나면 띄우지 않음(구글 안내). 조금 앞서 버림
    private static let interstitialFresh: TimeInterval = 55 * 60

    private let nativeUnit: String
    private let interstitialUnit: String
    private let rewardedUnit: String

    private var started = false
    private var interstitial: InterstitialAd?
    private var interstitialAt = Date.distantPast
    private var loadingInterstitial = false
    private var natives: [String: NativeAd] = [:]
    private var nativeOrder: [String] = []
    private var loaders: [String: NativeLoader] = [:]
    private var presenting: FullScreenHandler?

    override init() {
        #if DEBUG
            nativeUnit = TestUnit.native
            interstitialUnit = TestUnit.interstitial
            rewardedUnit = TestUnit.rewarded
        #else
            // 출시 빌드는 Local.xcconfig의 값. 없으면 광고를 부르지 않음
            let info = Bundle.main.infoDictionary
            nativeUnit = info?["DiaryAdNative"] as? String ?? ""
            interstitialUnit = info?["DiaryAdInterstitial"] as? String ?? ""
            rewardedUnit = info?["DiaryAdRewarded"] as? String ?? ""
        #endif
    }

    var available: Bool { !nativeUnit.isEmpty }

    /// 처음 광고를 부를 때 한 번 켬. 등급은 첫 요청보다 먼저
    func warmUp() {
        onMain { [self] in
            guard available, !started else { return }
            started = true
            MobileAds.shared.requestConfiguration.maxAdContentRating = .teen
            MobileAds.shared.start(completionHandler: nil)
        }
    }

    func prepareInterstitial() {
        onMain { [self] in
            guard available, !loadingInterstitial, !interstitialIsFresh else { return }
            warmUp()
            interstitial = nil
            loadingInterstitial = true
            InterstitialAd.load(with: interstitialUnit, request: Request()) { [weak self] ad, _ in
                guard let self else { return }
                loadingInterstitial = false
                interstitial = ad
                if ad != nil { interstitialAt = Date() }
            }
        }
    }

    private var interstitialIsFresh: Bool {
        interstitial != nil && Date().timeIntervalSince(interstitialAt) < Self.interstitialFresh
    }

    func showInterstitial(onImpression: @escaping () -> Void, onDone: @escaping (KotlinBoolean) -> Void) {
        onMain { [self] in
            guard let ad = interstitial, interstitialIsFresh, let screen = topScreen() else {
                onDone(false)
                return
            }
            interstitial = nil
            let handler = FullScreenHandler(onImpression: onImpression) { [weak self] shown in
                self?.presenting = nil
                onDone(KotlinBoolean(bool: shown))
            }
            presenting = handler
            ad.fullScreenContentDelegate = handler
            ad.present(from: screen)
        }
    }

    func showRewarded(onDone: @escaping (KotlinBoolean) -> Void) {
        onMain { [self] in
            guard available else {
                onDone(false)
                return
            }
            warmUp()
            RewardedAd.load(with: rewardedUnit, request: Request()) { [weak self] ad, _ in
                guard let self, let ad, let screen = topScreen() else {
                    onDone(false)
                    return
                }
                var earned = false
                let handler = FullScreenHandler(onImpression: {}) { [weak self] _ in
                    self?.presenting = nil
                    onDone(KotlinBoolean(bool: earned))
                }
                presenting = handler
                ad.fullScreenContentDelegate = handler
                ad.present(from: screen) { earned = true }
            }
        }
    }

    func loadNative(key: String, onLoaded: @escaping () -> Void) {
        onMain { [self] in
            guard available, natives[key] == nil, loaders[key] == nil else { return }
            warmUp()
            let loader = NativeLoader(unit: nativeUnit, screen: topScreen()) { [weak self] ad in
                guard let self else { return }
                loaders[key] = nil
                guard let ad else { return }
                keep(key, ad)
                onLoaded()
            }
            loaders[key] = loader
            loader.load()
        }
    }

    /// 새 광고를 두고 끝을 넘으면 가장 오래된 광고를 치움. 치운 자리가 다시 보이면 새로 받음
    private func keep(_ key: String, _ ad: NativeAd) {
        natives[key] = ad
        nativeOrder.append(key)
        while nativeOrder.count > Self.maxNatives {
            natives[nativeOrder.removeFirst()] = nil
        }
    }

    func nativeView(key: String, card: Bool, style: IosAdStyle) -> UIView? {
        guard let ad = natives[key] else { return nil }
        return NativeAdCardView(ad: ad, card: card, style: style)
    }

    func fittingHeight(view: UIView, width: Double) -> Double {
        let size = view.systemLayoutSizeFitting(
            CGSize(width: width, height: UIView.layoutFittingCompressedSize.height),
            withHorizontalFittingPriority: .required,
            verticalFittingPriority: .fittingSizeLevel
        )
        return Double(ceil(size.height))
    }

    private func onMain(_ work: @escaping () -> Void) {
        if Thread.isMainThread { work() } else { DispatchQueue.main.async(execute: work) }
    }

    /// 지금 맨 위 화면. 광고는 여기서 띄움
    private func topScreen() -> UIViewController? {
        let scene = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }.first { $0.activationState == .foregroundActive }
        var top = scene?.windows.first { $0.isKeyWindow }?.rootViewController
        while let next = top?.presentedViewController { top = next }
        return top
    }
}

/// 전면·보상형 광고의 열림과 닫힘
private final class FullScreenHandler: NSObject, FullScreenContentDelegate {
    private let onImpression: () -> Void
    private let onClose: (Bool) -> Void

    init(onImpression: @escaping () -> Void, onClose: @escaping (Bool) -> Void) {
        self.onImpression = onImpression
        self.onClose = onClose
    }

    func adDidRecordImpression(_ ad: FullScreenPresentingAd) { onImpression() }

    func adDidDismissFullScreenContent(_ ad: FullScreenPresentingAd) { onClose(true) }

    func ad(_ ad: FullScreenPresentingAd, didFailToPresentFullScreenContentWithError error: Error) { onClose(false) }
}

/// 네이티브 광고 하나를 받는 동안 붙잡아 둠
private final class NativeLoader: NSObject, NativeAdLoaderDelegate {
    private let loader: AdLoader
    private let done: (NativeAd?) -> Void

    init(unit: String, screen: UIViewController?, done: @escaping (NativeAd?) -> Void) {
        // 영상은 소리 없이 시작(`docs/business.md`)
        let video = VideoOptions()
        video.shouldStartMuted = true
        // 기본 자리는 오른쪽 위라 앱의 ×와 겹침. AdChoices는 가리면 안 되어 오른쪽 아래로
        let choices = NativeAdViewAdOptions()
        choices.preferredAdChoicesPosition = .bottomRightCorner
        loader = AdLoader(adUnitID: unit, rootViewController: screen, adTypes: [.native], options: [video, choices])
        self.done = done
        super.init()
        loader.delegate = self
    }

    func load() { loader.load(Request()) }

    func adLoader(_ adLoader: AdLoader, didReceive nativeAd: NativeAd) { done(nativeAd) }

    func adLoader(_ adLoader: AdLoader, didFailToReceiveAdWithError error: Error) { done(nil) }
}

/// Android NativeAdCard와 같은 꼴: 안쪽 16, 오른쪽은 ×와 AdChoices 자리 28, 그림 120, 사이 12
private final class NativeAdCardView: NativeAdView {
    private enum Size {
        static let padding: CGFloat = 16
        static let gap: CGFloat = 12
        // 영상 광고는 그림 자리가 가로세로 120 이상이어야 들어옴(AdMob 네이티브 정책)
        static let media: CGFloat = 120
        static let closeRoom: CGFloat = 28
        static let title: CGFloat = 15
        static let small: CGFloat = 13
    }

    init(ad: NativeAd, card: Bool, style: IosAdStyle) {
        super.init(frame: .zero)
        backgroundColor = UIColor(argb: style.surface)
        layer.cornerRadius = CGFloat(style.corner)
        clipsToBounds = true

        // AdMob이 제목 앞부분을 자르지 말라고 해 두 줄까지 둠
        let headline = Self.label(Size.title, .bold, UIColor(argb: style.title))
        headline.numberOfLines = 2
        headline.text = ad.headline
        let mark = Self.label(Size.small, .regular, UIColor(argb: style.body))
        mark.text = style.label
        let action = Self.label(Size.small, .bold, UIColor(argb: style.title))
        action.text = ad.callToAction
        // 누름은 광고 뷰가 받음(구글 안내)
        action.isUserInteractionEnabled = false

        let column = UIStackView(arrangedSubviews: [headline, mark, action])
        column.axis = .vertical
        column.alignment = .leading
        let row = UIStackView(arrangedSubviews: [column])
        row.axis = .horizontal
        row.alignment = .top
        row.spacing = Size.gap
        if card {
            let media = MediaView()
            media.contentMode = .scaleAspectFit
            media.mediaContent = ad.mediaContent
            media.translatesAutoresizingMaskIntoConstraints = false
            NSLayoutConstraint.activate([
                media.widthAnchor.constraint(equalToConstant: Size.media),
                media.heightAnchor.constraint(equalToConstant: Size.media)
            ])
            row.insertArrangedSubview(media, at: 0)
            mediaView = media
        }
        row.translatesAutoresizingMaskIntoConstraints = false
        addSubview(row)
        NSLayoutConstraint.activate([
            row.topAnchor.constraint(equalTo: topAnchor, constant: Size.padding),
            row.leadingAnchor.constraint(equalTo: leadingAnchor, constant: Size.padding),
            row.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -Size.closeRoom),
            row.bottomAnchor.constraint(equalTo: bottomAnchor, constant: -Size.padding)
        ])
        headlineView = headline
        callToActionView = action
        nativeAd = ad
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { nil }

    /// 기기 글자 크기를 따름(Android sp와 같음)
    private static func label(_ size: CGFloat, _ weight: UIFont.Weight, _ color: UIColor) -> UILabel {
        let label = UILabel()
        label.font = UIFontMetrics.default.scaledFont(for: .systemFont(ofSize: size, weight: weight))
        label.adjustsFontForContentSizeCategory = true
        label.textColor = color
        return label
    }
}

private extension UIColor {
    convenience init(argb: Int64) {
        let value = UInt32(truncatingIfNeeded: argb)
        self.init(
            red: CGFloat((value >> 16) & 0xFF) / 255,
            green: CGFloat((value >> 8) & 0xFF) / 255,
            blue: CGFloat(value & 0xFF) / 255,
            alpha: CGFloat((value >> 24) & 0xFF) / 255
        )
    }
}
