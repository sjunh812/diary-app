import Shared
import SwiftUI

@main
struct iOSApp: App {
    init() {
        // Koin을 켜고 첫 화면 전에 설정과 잠금을 읽는다(contracts/app-shell.md 시작 순서 1).
        IosStartupKt.startApp()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
