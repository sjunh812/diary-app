import GoogleSignIn
import SwiftUI

// 드라이브 확인용 iOS 빌드(스펙 003 quickstart 1).
// Android 확인 빌드와 같은 질의로 서로 만든 파일과 예전 앱 note.db가 보이는지 본다.
@main
struct DriveCheckApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in GIDSignIn.sharedInstance.handle(url) }
        }
    }
}
