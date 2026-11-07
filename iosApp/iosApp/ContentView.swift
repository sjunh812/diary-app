import Shared
import SwiftUI
import UIKit

// 공유 모듈(shared/app)의 Compose 화면을 띄운다.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        // 화면 끝까지 그리고 안전 영역은 Compose 쪽에서 맞춘다.
        ComposeView().ignoresSafeArea()
    }
}
