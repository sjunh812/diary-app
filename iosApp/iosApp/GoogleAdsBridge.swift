import AppTrackingTransparency
import CoreText
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
    /// 추적 허용 답을 기다리는 광고 칸. nil이면 묻는 중이 아님
    private var trackingWaiters: [() -> Void]?

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

    /// 앱 시작 때 미리 받아 둔 광고라 추적 허용은 띄우기 직전에 물음. 다음 광고부터 답이 반영됨
    func showInterstitial(onImpression: @escaping () -> Void, onDone: @escaping (KotlinBoolean) -> Void) {
        onMain { [self] in
            guard interstitial != nil, interstitialIsFresh else {
                onDone(false)
                return
            }
            askTrackingOnce { [self] in
                guard let ad = interstitial, let screen = topScreen() else {
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
    }

    func showRewarded(onDone: @escaping (KotlinBoolean) -> Void) {
        onMain { [self] in
            guard available else {
                onDone(false)
                return
            }
            warmUp()
            askTrackingOnce { [self] in
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
            // 잠금·온보딩이 끝나고 광고를 볼 사람에게만 옴
            askTrackingOnce { loader.load() }
        }
    }

    /// 앱 추적 허용(ATT)을 아직 묻지 않았으면 묻고 답을 받은 뒤 [then]. 거절해도 광고는 광고 ID 없이 받음
    /// 목록 광고, 전면 광고, 보상형 광고 중 먼저 보이는 광고 앞에서 한 번
    /// 앱이 앞에 있어야 창이 뜸. 아니면 묻지 않고 넘어가 다음 광고 칸에서 다시 봄
    private func askTrackingOnce(then: @escaping () -> Void) {
        guard ATTrackingManager.trackingAuthorizationStatus == .notDetermined,
              UIApplication.shared.applicationState == .active
        else {
            then()
            return
        }
        // 답을 기다리는 동안 온 광고 칸은 같은 요청에 모음. 창은 한 번만
        if trackingWaiters != nil {
            trackingWaiters?.append(then)
            return
        }
        trackingWaiters = [then]
        ATTrackingManager.requestTrackingAuthorization { [weak self] _ in
            DispatchQueue.main.async {
                let waiters = self?.trackingWaiters ?? []
                self?.trackingWaiters = nil
                waiters.forEach { $0() }
            }
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
        return NativeAdFrame(ad: NativeAdCardView(ad: ad, card: card, style: style), style: style)
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

/// 둥근 면. 광고 뷰를 안쪽 8에 두고 오른쪽은 앱의 × 자리(48)만큼 비움
/// 광고 뷰가 × 밑까지 오면 앱 콘텐츠가 광고를 덮음(AdMob 네이티브 정책)
/// SDK가 광고 뷰 구석에 넣는 AdChoices가 모서리 곡선에 잘리지 않음(AdMob 정책: 잘 보여야 함)
private final class NativeAdFrame: UIView {
    static let inset: CGFloat = 8
    static let closeColumn: CGFloat = 48

    init(ad: NativeAdCardView, style: IosAdStyle) {
        super.init(frame: .zero)
        backgroundColor = UIColor(argb: style.surface)
        layer.cornerRadius = CGFloat(style.corner)
        clipsToBounds = true
        ad.translatesAutoresizingMaskIntoConstraints = false
        addSubview(ad)
        NSLayoutConstraint.activate([
            ad.topAnchor.constraint(equalTo: topAnchor, constant: Self.inset),
            ad.leadingAnchor.constraint(equalTo: leadingAnchor, constant: Self.inset),
            ad.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -Self.closeColumn),
            ad.bottomAnchor.constraint(equalTo: bottomAnchor, constant: -Self.inset)
        ])
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { nil }
}

/// Android NativeAdCard와 같은 꼴: 면 안쪽 16(면 8 + 광고 뷰 8), 오른쪽 여백은 광고 뷰 밖의 × 자리
/// 글자는 앱 글꼴과 글자 단계, 행동 버튼은 앱의 작은 연보라 버튼 모양
private final class NativeAdCardView: NativeAdView {
    private enum Size {
        static let padding: CGFloat = 8
        static let gap: CGFloat = 12
        // 영상 광고는 그림 자리가 가로세로 120 이상이어야 들어옴(AdMob 네이티브 정책)
        static let media: CGFloat = 120
        static let mediaCorner: CGFloat = 12
        // 얇은 줄의 광고 아이콘(1:1). 없는 광고는 글만
        static let icon: CGFloat = 40
        static let iconCorner: CGFloat = 8
        // SDK가 오른쪽 아래에 얹는 AdChoices 자리. 행동 버튼이 그 밑에 깔리지 않음
        static let adChoices: CGFloat = 20
        static let actionHeight: CGFloat = 32
        static let actionPadding: CGFloat = 12
        static let actionCorner: CGFloat = 8
    }

    init(ad: NativeAd, card: Bool, style: IosAdStyle) {
        super.init(frame: .zero)

        // bodyStrong 15/22 SemiBold. AdMob이 제목 앞부분을 자르지 말라고 해 두 줄까지 둠
        let headline = Self.label(AdFont.make(style.semiBoldFont, 15, .semibold), lineHeight: 22, color: UIColor(argb: style.title))
        headline.numberOfLines = 2
        headline.lineBreakMode = .byTruncatingTail
        headline.text = ad.headline
        // caption 13/18. 일기 줄의 시각·날씨처럼 보조 글자
        let mark = Self.label(AdFont.make(style.regularFont, 13, .regular), lineHeight: 18, color: UIColor(argb: style.body))
        mark.text = style.label
        let titles = UIStackView(arrangedSubviews: [headline, mark])
        titles.axis = .vertical
        titles.alignment = .leading

        let action = ad.callToAction.map { Self.actionButton($0, style: style) }

        let content: UIView
        if card {
            let media = MediaView()
            media.contentMode = .scaleAspectFit
            media.mediaContent = ad.mediaContent
            media.backgroundColor = UIColor(argb: style.mediaFill)
            media.layer.cornerRadius = Size.mediaCorner
            media.clipsToBounds = true
            media.translatesAutoresizingMaskIntoConstraints = false
            NSLayoutConstraint.activate([
                media.widthAnchor.constraint(equalToConstant: Size.media),
                media.heightAnchor.constraint(equalToConstant: Size.media)
            ])
            mediaView = media
            // 제목과 "광고"는 위에, 행동 버튼은 그림 아래 끝선에 맞춤
            let column = UIView()
            titles.translatesAutoresizingMaskIntoConstraints = false
            column.addSubview(titles)
            var constraints = [
                titles.topAnchor.constraint(equalTo: column.topAnchor),
                titles.leadingAnchor.constraint(equalTo: column.leadingAnchor),
                titles.trailingAnchor.constraint(lessThanOrEqualTo: column.trailingAnchor),
                column.heightAnchor.constraint(greaterThanOrEqualToConstant: Size.media)
            ]
            if let action {
                action.translatesAutoresizingMaskIntoConstraints = false
                column.addSubview(action)
                constraints += [
                    action.topAnchor.constraint(greaterThanOrEqualTo: titles.bottomAnchor, constant: Size.padding),
                    action.leadingAnchor.constraint(equalTo: column.leadingAnchor),
                    action.bottomAnchor.constraint(equalTo: column.bottomAnchor)
                ]
            } else {
                constraints.append(titles.bottomAnchor.constraint(lessThanOrEqualTo: column.bottomAnchor))
            }
            NSLayoutConstraint.activate(constraints)
            let row = UIStackView(arrangedSubviews: [media, column])
            row.axis = .horizontal
            row.alignment = .top
            row.spacing = Size.gap
            content = row
        } else {
            // 얇은 줄: 아이콘, 제목과 "광고", 행동 버튼을 세로 가운데로
            var items: [UIView] = []
            if let image = ad.icon?.image {
                let icon = UIImageView(image: image)
                icon.contentMode = .scaleAspectFill
                icon.layer.cornerRadius = Size.iconCorner
                icon.clipsToBounds = true
                icon.translatesAutoresizingMaskIntoConstraints = false
                NSLayoutConstraint.activate([
                    icon.widthAnchor.constraint(equalToConstant: Size.icon),
                    icon.heightAnchor.constraint(equalToConstant: Size.icon)
                ])
                iconView = icon
                items.append(icon)
            }
            items.append(titles)
            titles.setContentHuggingPriority(.defaultLow, for: .horizontal)
            titles.setContentCompressionResistancePriority(.defaultLow, for: .horizontal)
            if let action { items.append(action) }
            let row = UIStackView(arrangedSubviews: items)
            row.axis = .horizontal
            row.alignment = .center
            row.spacing = Size.gap
            row.isLayoutMarginsRelativeArrangement = true
            row.directionalLayoutMargins = NSDirectionalEdgeInsets(top: 0, leading: 0, bottom: 0, trailing: Size.adChoices)
            content = row
        }
        content.translatesAutoresizingMaskIntoConstraints = false
        addSubview(content)
        NSLayoutConstraint.activate([
            content.topAnchor.constraint(equalTo: topAnchor, constant: Size.padding),
            content.leadingAnchor.constraint(equalTo: leadingAnchor, constant: Size.padding),
            content.trailingAnchor.constraint(equalTo: trailingAnchor),
            content.bottomAnchor.constraint(equalTo: bottomAnchor, constant: -Size.padding)
        ])
        headlineView = headline
        callToActionView = action
        nativeAd = ad
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { nil }

    /// captionStrong 13/18 Medium, `accentText` 글자에 `selected` 면
    /// 눌림은 AdMob이 세야 해서 버튼 부품이 아니라 모양만 맞춘 뷰(구글 안내: 행동 뷰는 사용자 입력을 받지 않음)
    private static func actionButton(_ text: String, style: IosAdStyle) -> UIView {
        let label = Self.label(AdFont.make(style.mediumFont, 13, .medium), lineHeight: 18, color: UIColor(argb: style.action))
        label.text = text
        let box = UIView()
        box.backgroundColor = UIColor(argb: style.actionFill)
        box.layer.cornerRadius = Size.actionCorner
        box.isUserInteractionEnabled = false
        label.translatesAutoresizingMaskIntoConstraints = false
        box.addSubview(label)
        NSLayoutConstraint.activate([
            label.leadingAnchor.constraint(equalTo: box.leadingAnchor, constant: Size.actionPadding),
            label.trailingAnchor.constraint(equalTo: box.trailingAnchor, constant: -Size.actionPadding),
            label.centerYAnchor.constraint(equalTo: box.centerYAnchor),
            label.topAnchor.constraint(greaterThanOrEqualTo: box.topAnchor),
            box.heightAnchor.constraint(greaterThanOrEqualToConstant: Size.actionHeight)
        ])
        box.setContentHuggingPriority(.required, for: .horizontal)
        box.setContentCompressionResistancePriority(.required, for: .horizontal)
        return box
    }

    /// 기기 글자 크기를 따름(Android sp와 같음). 줄 높이도 같은 비율로 늘림
    private static func label(_ font: UIFont, lineHeight: CGFloat, color: UIColor) -> UILabel {
        let label = UILabel()
        let metrics = UIFontMetrics(forTextStyle: .body)
        label.font = metrics.scaledFont(for: font)
        label.adjustsFontForContentSizeCategory = true
        label.textColor = color
        let height = metrics.scaledValue(for: lineHeight)
        label.heightAnchor.constraint(greaterThanOrEqualToConstant: height).isActive = true
        return label
    }
}

/// 앱과 같은 글꼴. 앱에 넣은 글꼴은 번들의 Compose 리소스에서, 받은 글꼴은 그 경로에서 읽음
/// 굵기가 하나뿐인 글꼴은 그 글꼴 그대로. 파일을 읽지 못하면 시스템 글꼴
private enum AdFont {
    private static var names: [String: String] = [:]
    private static let bundled: [String: URL] = {
        guard let root = Bundle.main.resourceURL?.appendingPathComponent("compose-resources"),
              let files = FileManager.default.enumerator(at: root, includingPropertiesForKeys: nil)
        else { return [:] }
        var found: [String: URL] = [:]
        for case let url as URL in files where url.path.contains("/font/") {
            found[url.lastPathComponent] = url
        }
        return found
    }()

    static func make(_ file: String?, _ size: CGFloat, _ weight: UIFont.Weight) -> UIFont {
        guard let file, let name = name(of: file), let font = UIFont(name: name, size: size) else {
            return .systemFont(ofSize: size, weight: weight)
        }
        return font
    }

    private static func name(of file: String) -> String? {
        if let known = names[file] { return known }
        let url = file.hasPrefix("/") ? URL(fileURLWithPath: file) : bundled[file]
        guard let url,
              let descriptors = CTFontManagerCreateFontDescriptorsFromURL(url as CFURL) as? [CTFontDescriptor],
              let first = descriptors.first,
              let name = CTFontDescriptorCopyAttribute(first, kCTFontNameAttribute) as? String
        else { return nil }
        // 이미 등록된 파일이면 실패를 돌려주지만 이름으로 쓸 수 있음
        CTFontManagerRegisterFontsForURL(url as CFURL, .process, nil)
        names[file] = name
        return name
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
