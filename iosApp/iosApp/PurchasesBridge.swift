import Foundation
import RevenueCat
import Shared

/// RevenueCat 구현. Android RevenueCatClient와 같은 규칙
/// 오류를 까닭으로 바꾸기만 하고, 무엇을 언제 믿을지는 공통 코드(StoreBilling)가 정함
final class PurchasesBridge: NSObject, IosStoreBridge, PurchasesDelegate {
    private typealias Answer = (Set<String>?, IosStoreError?) -> Void

    private var listener: ((Set<String>) -> Void)?

    /// 키 없이 만든 빌드는 구매가 없는 무료 앱
    static func start() -> PurchasesBridge? {
        guard let key = Bundle.main.object(forInfoDictionaryKey: "RevenueCatKey") as? String, !key.isEmpty else { return nil }
        Purchases.configure(withAPIKey: key)
        let bridge = PurchasesBridge()
        // 위임자는 약하게 잡힘. 공통 코드가 다리를 앱 종료까지 들고 있음
        Purchases.shared.delegate = bridge
        return bridge
    }

    func entitlements(done: @escaping (Set<String>?, IosStoreError?) -> Void) {
        Purchases.shared.getCustomerInfo { info, error in Self.answer(info, error, done) }
    }

    func sync(done: @escaping (Set<String>?, IosStoreError?) -> Void) {
        Purchases.shared.syncPurchases { info, error in Self.answer(info, error, done) }
    }

    func prices(storeIds: [String], done: @escaping ([String: String]) -> Void) {
        Purchases.shared.getProducts(storeIds) { products in
            let prices = Dictionary(
                products.map { ($0.productIdentifier, $0.localizedPriceString) },
                uniquingKeysWith: { first, _ in first }
            )
            Self.onMain { done(prices) }
        }
    }

    func purchase(storeId: String, done: @escaping (Set<String>?, IosStoreError?) -> Void) {
        Purchases.shared.getProducts([storeId]) { products in
            guard let product = products.first(where: { $0.productIdentifier == storeId }) else {
                Self.onMain { done(nil, .other) }
                return
            }
            Purchases.shared.purchase(product: product) { _, info, error, cancelled in
                if cancelled {
                    Self.onMain { done(nil, .cancelled) }
                } else {
                    Self.answer(info, error, done)
                }
            }
        }
    }

    func restore(done: @escaping (Set<String>?, IosStoreError?) -> Void) {
        Purchases.shared.restorePurchases { info, error in Self.answer(info, error, done) }
    }

    func onUpdate(listener: @escaping (Set<String>) -> Void) {
        self.listener = listener
    }

    func purchases(_ purchases: Purchases, receivedUpdated customerInfo: CustomerInfo) {
        let active = Self.active(customerInfo)
        Self.onMain { [weak self] in self?.listener?(active) }
    }

    private static func active(_ info: CustomerInfo) -> Set<String> {
        Set(info.entitlements.active.keys)
    }

    private static func answer(_ info: CustomerInfo?, _ error: NSError?, _ done: @escaping Answer) {
        onMain {
            if let info {
                done(active(info), nil)
            } else {
                done(nil, reason(error))
            }
        }
    }

    private static func reason(_ error: NSError?) -> IosStoreError {
        guard let error, error.domain == ErrorCode.errorDomain, let code = ErrorCode(rawValue: error.code) else { return .other }
        switch code {
        case .purchaseCancelledError: return .cancelled
        case .paymentPendingError: return .pending
        case .productAlreadyPurchasedError: return .alreadyOwned
        default: return .other
        }
    }

    /// 공통 코드는 화면 스레드에서만 요금제를 바꿈
    private static func onMain(_ work: @escaping () -> Void) {
        if Thread.isMainThread { work() } else { DispatchQueue.main.async(execute: work) }
    }
}
