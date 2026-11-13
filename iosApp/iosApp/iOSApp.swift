import GoogleSignIn
import Shared
import SwiftUI
import UserNotifications

// 누른 알림을 공유 모듈에 전달
// 앱이 꺼져 있을 때 누른 알림도 받으려면 앱이 뜨기 전에 위임자를 걸어야 함
// 시스템이 위임자를 약하게 잡아 앱 종료까지 살려 둠
final class NotificationTapDelegate: NSObject, UNUserNotificationCenterDelegate {
    static let shared = NotificationTapDelegate()

    /// 앱을 쓰는 중에도 알림 표시
    /// 없으면 시스템이 조용히 버림
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .sound])
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        if response.actionIdentifier == UNNotificationDefaultActionIdentifier {
            IosStartupKt.onNotificationTapped(identifier: response.notification.request.identifier)
        }
        completionHandler()
    }
}

@main
struct iOSApp: App {
    @Environment(\.scenePhase) private var scenePhase

    init() {
        // 첫 화면 전에 Koin 시작, 설정과 잠금 읽기(contracts/app-shell.md 시작 순서 1)
        IosStartupKt.startApp(
            driveBridge: GoogleDriveBridge(),
            firebase: FirebaseReporter.start(),
            ads: GoogleAdsBridge()
        )
        DailyBackupTask.register()
        UNUserNotificationCenter.current().delegate = NotificationTapDelegate.shared
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .privacyCover()
                // 구글 로그인 창이 앱으로 돌려보내는 주소
                .onOpenURL { GIDSignIn.sharedInstance.handle($0) }
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .background { DailyBackupTask.enteredBackground() }
        }
    }
}
