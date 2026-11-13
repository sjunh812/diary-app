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
}
