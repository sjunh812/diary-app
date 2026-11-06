import GoogleSignIn
import Shared
import SwiftUI
import UserNotifications

// 알림을 누르면 공유 모듈에 알린다.
// 위임자는 앱이 뜨기 전에 걸어야 앱이 꺼져 있을 때 누른 알림도 받는다.
final class NotificationTapDelegate: NSObject, UNUserNotificationCenterDelegate {
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
    private let notificationTaps = NotificationTapDelegate()

    init() {
        // Koin을 켜고 첫 화면 전에 설정과 잠금을 읽는다(contracts/app-shell.md 시작 순서 1).
        IosStartupKt.startApp(driveBridge: GoogleDriveBridge(), firebase: FirebaseReporter.start())
        DailyBackupTask.register()
        UNUserNotificationCenter.current().delegate = notificationTaps
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .privacyCover()
                // 구글 로그인 창이 앱으로 돌려보내는 주소를 받는다.
                .onOpenURL { GIDSignIn.sharedInstance.handle($0) }
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .background { DailyBackupTask.enteredBackground() }
        }
    }
}
