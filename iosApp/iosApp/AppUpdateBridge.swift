import Shared
import StoreKit
import UIKit

/// 앱 위 App Store 상품 화면(스펙 009). 업데이트는 사용자가 그 화면에서 누름
/// 상품을 못 읽으면 App Store 앱의 이 앱 화면을 엶
final class AppUpdateBridge: NSObject, IosUpdateBridge, SKStoreProductViewControllerDelegate {
    func present(appId: String) -> Bool {
        // Kotlin이 화면 스레드에서 부름
        guard Thread.isMainThread else {
            assertionFailure("App Store 다리는 화면 스레드에서 불러야 함")
            return false
        }
        return MainActor.assumeIsolated {
            guard let top = Self.topController(), let id = Int(appId) else { return openStore(appId) }
            let store = SKStoreProductViewController()
            store.delegate = self
            store.loadProduct(withParameters: [SKStoreProductParameterITunesItemIdentifier: NSNumber(value: id)]) { [weak self] loaded, _ in
                guard !loaded else { return }
                store.dismiss(animated: true)
                _ = self?.openStore(appId)
            }
            top.present(store, animated: true)
            return true
        }
    }

    func productViewControllerDidFinish(_ viewController: SKStoreProductViewController) {
        viewController.dismiss(animated: true)
    }

    @MainActor
    private func openStore(_ appId: String) -> Bool {
        guard let url = URL(string: "itms-apps://apps.apple.com/app/id\(appId)") else { return false }
        UIApplication.shared.open(url)
        return true
    }

    /// 앞에 나와 있는 창의 맨 위 화면. 시트가 떠 있으면 그 위에 띄움
    @MainActor
    private static func topController() -> UIViewController? {
        let scene = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first { $0.activationState == .foregroundActive }
        var top = scene?.keyWindow?.rootViewController
        while let presented = top?.presentedViewController {
            top = presented
        }
        return top
    }
}
