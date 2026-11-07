import Shared
import SwiftUI

// 잠금이 켜져 있으면 앱이 비활성일 때(앱 전환기, 알림 센터, Face ID 창) 화면을 덮어 일기가 비치지 않게 한다
// (스펙 001 FR-018, research R13).
// 잠금이 꺼져 있으면 덮지 않는다.
struct PrivacyCover: ViewModifier {
    @Environment(\.scenePhase) private var scenePhase
    @State private var covered = false

    func body(content: Content) -> some View {
        content
            .overlay {
                // 시작 화면 색 에셋(LaunchBackground)이 생기면 그 색으로 바꾼다(스펙 001 US4).
                if covered { Color(uiColor: .systemBackground).ignoresSafeArea() }
            }
            .onChange(of: scenePhase) { _, phase in
                covered = phase != .active && IosStartupKt.isPrivacyCoverNeeded()
            }
    }
}

extension View {
    func privacyCover() -> some View { modifier(PrivacyCover()) }
}
