import Foundation
import Security

/// 재부팅 뒤 아직 한 번도 잠금을 풀지 않아 키체인을 읽을 수 없는지
/// 구글 로그인 정보와 같은 보호 등급(AfterFirstUnlockThisDeviceOnly)의 작은 표지로 확인
/// 화면이 잠겨 있어도 처음 잠금을 푼 뒤에는 읽히므로 isProtectedDataAvailable로는 가를 수 없음
enum KeychainLock {
    private static let account = "keychain-lock-probe"

    /// 앱을 열 때 표지를 남김. 이미 있으면 그대로
    static func prepare() {
        let query = baseQuery()
        guard SecItemCopyMatching(query as CFDictionary, nil) == errSecItemNotFound else { return }
        var add = query
        add[kSecValueData as String] = Data([1])
        add[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        SecItemAdd(add as CFDictionary, nil)
    }

    /// 표지를 읽을 수 없으면 참. 이때는 로그인 정보도 읽을 수 없음
    static var isLocked: Bool {
        var query = baseQuery()
        query[kSecReturnData as String] = true
        return SecItemCopyMatching(query as CFDictionary, nil) == errSecInteractionNotAllowed
    }

    private static func baseQuery() -> [String: Any] {
        [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: Bundle.main.bundleIdentifier ?? "diary",
            kSecAttrAccount as String: account
        ]
    }
}
