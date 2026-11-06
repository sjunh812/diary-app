import FirebaseAnalytics
import FirebaseCore
import FirebaseCrashlytics
import Foundation
import Shared

/// 고정 표를 지난 이벤트와 키만 받는다(research R21). 일기 내용은 여기까지 오지 않는다.
final class FirebaseReporter: FirebaseReporting {
    /// 설정 파일이 없는 빌드는 Firebase를 켜지 않는다. `configure`는 파일이 없으면 앱을 끝낸다.
    static func start() -> FirebaseReporter? {
        guard Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil else { return nil }
        FirebaseApp.configure()
        #if DEBUG
            Analytics.setAnalyticsCollectionEnabled(false)
            Crashlytics.crashlytics().setCrashlyticsCollectionEnabled(false)
        #endif
        return FirebaseReporter()
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
