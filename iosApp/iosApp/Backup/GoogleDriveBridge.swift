import GoogleSignIn
import Shared
import UIKit

/// 공유 모듈의 드라이브 권한 다리를 GoogleSignIn으로 구현(스펙 003 research R2)
/// 범위는 `drive.file`뿐, 토큰은 GoogleSignIn이 이 기기 키체인에 보관
final class GoogleDriveBridge: NSObject, GoogleDrive {
    private let driveScope = "https://www.googleapis.com/auth/drive.file"

    private static var hasClientID: Bool {
        (Bundle.main.object(forInfoDictionaryKey: "GIDClientID") as? String)?.isEmpty == false
    }

    func connect(hint: String?, done: @escaping (String?, String?, String?, String?, KotlinLong) -> Void) {
        let fail = { (kind: String, report: Report?) in done(nil, nil, kind, report?.domain, KotlinLong(value: report?.code ?? 0)) }
        DispatchQueue.main.async {
            // 클라이언트 ID 없이 부르면 GoogleSignIn이 앱을 종료함
            guard Self.hasClientID, let root = UIApplication.shared.connectedScenes
                .compactMap({ ($0 as? UIWindowScene)?.keyWindow?.rootViewController }).first
            else {
                fail("failed", Report(domain: Self.hasClientID ? "app.noPresenter" : "app.noClientID"))
                return
            }
            GIDSignIn.sharedInstance.signIn(withPresenting: root, hint: hint, additionalScopes: [self.driveScope]) { result, error in
                if let error {
                    let kind = Self.kind(error)
                    fail(kind, kind == "failed" ? Report(error) : nil)
                    return
                }
                guard let user = result?.user, let key = user.userID, let email = user.profile?.email else {
                    fail("failed", Report(domain: "app.noProfile"))
                    return
                }
                // 동의 화면에서 드라이브 권한을 뺀 경우
                // 연결됐다고 하면 곧바로 다시 연결하라는 안내가 뜸
                guard user.grantedScopes?.contains(self.driveScope) == true else {
                    GIDSignIn.sharedInstance.signOut()
                    fail("failed", Report(domain: "app.noDriveScope"))
                    return
                }
                done(key, email, nil, nil, KotlinLong(value: 0))
            }
        }
    }

    func accessToken(done: @escaping (String?, String?, String?, String?, String?, KotlinLong) -> Void) {
        let fail = { (kind: String, report: Report?) in done(nil, nil, nil, kind, report?.domain, KotlinLong(value: report?.code ?? 0)) }
        DispatchQueue.main.async {
            GIDSignIn.sharedInstance.restorePreviousSignIn { user, error in
                guard let user else {
                    let kind = error.map { Self.restoreKind($0) } ?? "needsUser"
                    fail(kind, kind == "offline" ? nil : error.map { Report($0) } ?? Report(domain: "app.noUser"))
                    return
                }
                guard user.grantedScopes?.contains(self.driveScope) == true else {
                    fail("needsUser", Report(domain: "app.noDriveScope"))
                    return
                }
                user.refreshTokensIfNeeded { refreshed, error in
                    guard let refreshed, let key = refreshed.userID, let email = refreshed.profile?.email else {
                        let kind = error.map { Self.refreshKind($0) } ?? "failed"
                        fail(kind, kind == "offline" ? nil : error.map { Report($0) } ?? Report(domain: "app.noProfile"))
                        return
                    }
                    done(refreshed.accessToken.tokenString, key, email, nil, nil, KotlinLong(value: 0))
                }
            }
        }
    }

    /// 안드로이드와 같이 구글 권한은 거두지 않음
    /// drive.file 권한을 거두면 다시 연결해도 앱이 만든 백업이 안 보일 수 있음
    func disconnect(done: @escaping () -> Void) {
        GIDSignIn.sharedInstance.signOut()
        done()
    }

    private static func kind(_ error: Error) -> String {
        let nsError = error as NSError
        if nsError.domain == kGIDSignInErrorDomain, nsError.code == GIDSignInError.canceled.rawValue { return "cancelled" }
        return isOffline(error) ? "offline" : "failed"
    }

    /// 다시 로그인해야 풀리는 것만 다시 연결: 키체인에 로그인이 없음, 키체인을 읽지 못함, 기업 관리(EMM) 거절
    /// 서버의 일시 오류까지 다시 연결로 보면 자동 백업이 멈춤
    private static let needsSignIn: Set<Int> = [
        GIDSignInError.hasNoAuthInKeychain.rawValue,
        GIDSignInError.keychain.rawValue,
        GIDSignInError.EMM.rawValue
    ]

    /// 로그인 정보는 처음 잠금 해제 뒤에는 잠겨도 읽힘(GTMAppAuth AfterFirstUnlock)
    /// 재부팅 뒤 아직 풀지 않아 못 읽은 것이면 다시 연결로 보지 않고 다음에 다시 시도
    private static func restoreKind(_ error: Error) -> String {
        let nsError = error as NSError
        if nsError.domain == kGIDSignInErrorDomain, needsSignIn.contains(nsError.code) {
            return KeychainLock.isLocked ? "failed" : "needsUser"
        }
        return refreshKind(error)
    }

    /// 토큰 재발급을 구글이 거절하면 다시 연결해야 함
    /// 그 밖의 실패는 다음에 재시도
    private static func refreshKind(_ error: Error) -> String {
        if (error as NSError).domain == "org.openid.appauth.oauth_token" { return "needsUser" }
        return kind(error)
    }

    /// 토큰 갱신의 네트워크 실패는 AppAuth가 감싸서 옴
    /// 겉만 보면 연결이 끊긴 것으로 잘못 알아 자동 백업이 멈춤
    private static func isOffline(_ error: Error) -> Bool {
        var current: NSError? = error as NSError
        while let found = current {
            if found.domain == NSURLErrorDomain { return true }
            current = found.userInfo[NSUnderlyingErrorKey] as? NSError
        }
        return false
    }

    /// Crashlytics에 남길 오류 종류 이름과 번호. 메시지와 userInfo에는 계정 정보가 섞일 수 있어 담지 않음
    private struct Report {
        let domain: String
        let code: Int64

        init(domain: String, code: Int64 = 0) {
            self.domain = domain
            self.code = code
        }

        init(_ error: Error) {
            let nsError = error as NSError
            self.init(domain: nsError.domain, code: Int64(nsError.code))
        }
    }
}
