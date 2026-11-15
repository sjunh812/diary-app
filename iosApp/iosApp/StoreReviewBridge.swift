import Shared
import StoreKit
import UIKit

/// App Store 리뷰 창(스펙 007). 띄울지와 결과는 App Store가 정함
/// 옛 SKStoreReviewController는 iOS 18에서 지원 중단이라 AppStore.requestReview를 씀
final class StoreReviewBridge: IosStoreReviewBridge {
    /// 앞에 나와 있는 창을 찾아 요청했으면 참. 실제로 보일지는 iOS가 정함
    func request() -> Bool {
        // Kotlin이 화면 스레드에서 부름. 부르는 스레드가 바뀌면 리뷰가 소리 없이 사라져 개발 빌드에서 알림
        guard Thread.isMainThread else {
            assertionFailure("리뷰 다리는 화면 스레드에서 불러야 함")
            return false
        }
        return MainActor.assumeIsolated {
            // 앞에 나와 있는 창이 없으면 띄우지 않고 묻지 않은 것으로 남김
            guard let scene = UIApplication.shared.connectedScenes
                .compactMap({ $0 as? UIWindowScene })
                .first(where: { $0.activationState == .foregroundActive })
            else { return false }
            AppStore.requestReview(in: scene)
            return true
        }
    }
}
