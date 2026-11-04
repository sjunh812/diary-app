import GoogleSignIn
import Shared
import UIKit

/// 공유 모듈의 드라이브 권한 다리를 GoogleSignIn으로 구현한다(스펙 003 research R2).
/// 범위는 `drive.file`뿐이고 토큰은 GoogleSignIn이 이 기기 키체인에 둔다.
final class GoogleDriveBridge: NSObject, IosDriveBridge {
    private let driveScope = "https://www.googleapis.com/auth/drive.file"

    func connect(hint: String?, done: @escaping (String?, String?, String?) -> Void) {
        DispatchQueue.main.async {
            guard let root = UIApplication.shared.connectedScenes
                .compactMap({ ($0 as? UIWindowScene)?.keyWindow?.rootViewController }).first
            else {
                done(nil, nil, "failed")
                return
            }
            GIDSignIn.sharedInstance.signIn(withPresenting: root, hint: hint, additionalScopes: [self.driveScope]) { result, error in
                if let error {
                    done(nil, nil, Self.kind(error))
                    return
                }
                guard let user = result?.user, let key = user.userID, let email = user.profile?.email else {
                    done(nil, nil, "failed")
                    return
                }
                done(key, email, nil)
            }
        }
    }

    func accessToken(done: @escaping (String?, String?, String?, String?) -> Void) {
        DispatchQueue.main.async {
            GIDSignIn.sharedInstance.restorePreviousSignIn { user, error in
                guard let user else {
                    done(nil, nil, nil, error.map { Self.kind($0) == "offline" ? "offline" : "needsUser" } ?? "needsUser")
                    return
                }
                guard user.grantedScopes?.contains(self.driveScope) == true else {
                    done(nil, nil, nil, "needsUser")
                    return
                }
                user.refreshTokensIfNeeded { refreshed, error in
                    guard let refreshed, let key = refreshed.userID, let email = refreshed.profile?.email else {
                        done(nil, nil, nil, error.map { Self.kind($0) } ?? "failed")
                        return
                    }
                    done(refreshed.accessToken.tokenString, key, email, nil)
                }
            }
        }
    }

    func disconnect(done: @escaping () -> Void) {
        GIDSignIn.sharedInstance.disconnect { _ in done() }
    }

    private static func kind(_ error: Error) -> String {
        let nsError = error as NSError
        if nsError.domain == kGIDSignInErrorDomain, nsError.code == GIDSignInError.canceled.rawValue { return "cancelled" }
        if nsError.domain == NSURLErrorDomain { return "offline" }
        return "failed"
    }
}
