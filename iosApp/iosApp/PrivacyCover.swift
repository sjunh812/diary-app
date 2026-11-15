import Shared
import SwiftUI
import UIKit

// 잠금이 켜져 있으면 앱 비활성 때(앱 전환기, 알림 센터, Face ID 창) 화면을 덮어 일기를 가림
// (스펙 001 FR-018, research R13)
// 잠금이 꺼져 있으면 덮지 않음
// 공유 시트처럼 앱 위에 띄운 화면까지 덮도록 맨 위 창으로 덮음
struct PrivacyCover: ViewModifier {
    @Environment(\.scenePhase) private var scenePhase

    func body(content: Content) -> some View {
        content.onChange(of: scenePhase) { _, phase in
            if phase != .active && IosStartupKt.isPrivacyCoverNeeded() {
                CoverWindow.shared.show()
            } else {
                CoverWindow.shared.hide()
            }
        }
    }
}

/// 앱 창들 위에 뜨는 가림막 창
@MainActor
private final class CoverWindow {
    static let shared = CoverWindow()
    private var window: UIWindow?

    func show() {
        guard window == nil,
              let scene = UIApplication.shared.connectedScenes.compactMap({ $0 as? UIWindowScene }).first
        else { return }
        let cover = UIWindow(windowScene: scene)
        cover.windowLevel = .alert + 1
        // 따로 띄운 창이라 앱이 고른 화면 모드를 물려받지 못함. 시스템과 다르면 다른 색이 번쩍임
        cover.overrideUserInterfaceStyle = scene.windows.first { $0.windowLevel == .normal }?.overrideUserInterfaceStyle ?? .unspecified
        let screen = UIViewController()
        // 시작 화면과 같은 바탕(라이트·다크)
        screen.view.backgroundColor = UIColor(named: "LaunchBackground")
        cover.rootViewController = screen
        cover.isHidden = false
        window = cover
    }

    func hide() {
        window?.isHidden = true
        window = nil
    }
}

extension View {
    func privacyCover() -> some View { modifier(PrivacyCover()) }
}
