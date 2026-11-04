import GoogleSignIn
import Shared
import SwiftUI

@main
struct iOSApp: App {
    init() {
        // Koin을 켜고 첫 화면 전에 설정과 잠금을 읽는다(contracts/app-shell.md 시작 순서 1).
        IosStartupKt.startApp(driveBridge: GoogleDriveBridge())
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .privacyCover()
                // 구글 로그인 창이 앱으로 돌려보내는 주소를 받는다.
                .onOpenURL { GIDSignIn.sharedInstance.handle($0) }
        }
    }
}
