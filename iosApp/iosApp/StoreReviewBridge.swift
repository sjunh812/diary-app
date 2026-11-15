import Shared
import StoreKit
import UIKit

/// App Store 리뷰 창(스펙 007). 띄울지와 결과는 App Store가 정함
/// 옛 SKStoreReviewController는 iOS 18에서 지원 중단이라 AppStore.requestReview를 씀
final class StoreReviewBridge: IosStoreReviewBridge {
    func request() {
        Task { @MainActor in
            // 앞에 나와 있는 창이 없으면 띄우지 않음
            guard let scene = UIApplication.shared.connectedScenes
                .compactMap({ $0 as? UIWindowScene })
                .first(where: { $0.activationState == .foregroundActive })
            else { return }
            AppStore.requestReview(in: scene)
        }
    }
}
