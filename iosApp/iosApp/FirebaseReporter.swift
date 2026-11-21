import FirebaseAnalytics
import FirebaseCore
import FirebaseCrashlytics
import FirebaseRemoteConfig
import Foundation
import Shared

/// 고정 표를 거친 이벤트와 키만 받음(research R21)
/// 일기 내용은 여기까지 오지 않음
final class FirebaseReporter: FirebaseReporting {
    private static let interstitialKey = "interstitial_enabled"
    /// 무료 한도를 넘기지 않게 값은 12시간에 한 번만 받음
    private static let fetchInterval: TimeInterval = 12 * 60 * 60

    /// 설정 파일 없는 빌드는 Firebase를 켜지 않음
    /// `configure`는 파일이 없으면 앱을 종료함
    static func start() -> FirebaseReporter? {
        guard Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil else { return nil }
        FirebaseApp.configure()
        #if DEBUG
            Analytics.setAnalyticsCollectionEnabled(false)
            Crashlytics.crashlytics().setCrashlyticsCollectionEnabled(false)
        #endif
        let config = RemoteConfig.remoteConfig()
        let settings = RemoteConfigSettings()
        settings.minimumFetchInterval = fetchInterval
        config.configSettings = settings
        config.setDefaults([interstitialKey: true as NSObject])
        config.fetchAndActivate()
        return FirebaseReporter()
    }

    /// 받기에 실패하면 마지막 값, 처음이면 켜짐
    func interstitialEnabled() -> Bool {
        RemoteConfig.remoteConfig().configValue(forKey: Self.interstitialKey).boolValue
    }

    /// 새 버전 안내 원격 값(스펙 009). 없는 키는 빈 문자열
    func remoteString(key: String) -> String {
        RemoteConfig.remoteConfig().configValue(forKey: key).stringValue
    }

    func log(event: String, params: [String: String]) {
        if event == AnalyticsEventScreenView, let screen = params["screen"] {
            Analytics.logEvent(event, parameters: [AnalyticsParameterScreenName: screen, AnalyticsParameterScreenClass: screen])
        } else {
            Analytics.logEvent(event, parameters: params)
        }
    }

    func setCustomKey(key: String, value: String) {
        Crashlytics.crashlytics().setCustomValue(value, forKey: key)
    }

    /// 메시지는 일기 글이 섞일 수 있어 빼고 종류와 호출 위치만
    /// 주소로 보내 dSYM으로 이름이 풀림
    func recordCaught(kind: String, addresses: [KotlinLong]) {
        let model = ExceptionModel(name: kind, reason: "")
        model.stackTrace = addresses.map { StackFrame(address: UInt(truncatingIfNeeded: $0.int64Value)) }
        Crashlytics.crashlytics().record(exceptionModel: model)
    }

    /// 종류 이름과 번호만으로 만든 오류. 같은 종류·번호끼리 묶이고 건마다 기기와 시각이 붙음
    func recordError(domain: String, code: Int64) {
        Crashlytics.crashlytics().record(error: NSError(domain: domain, code: Int(code), userInfo: nil))
    }
}
