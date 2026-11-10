import Shared
import SwiftUI
import UIKit

// 공유 모듈(shared/app)의 Compose 화면
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        // 화면 끝까지 그리고 안전 영역은 Compose가 맞춤
        ComposeView().ignoresSafeArea()
    }
}
