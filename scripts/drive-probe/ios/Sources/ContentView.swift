import GoogleSignIn
import SwiftUI
import UIKit

// 버튼 순서대로 누르고, 아래 기록을 research R1에 옮겨 적는다.
struct ContentView: View {
    @State private var log = ""
    @Environment(\.scenePhase) private var scenePhase
    private let driveScope = "https://www.googleapis.com/auth/drive.file"

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                Text("드라이브 확인 (iOS)").font(.title2).bold()
                Button("1. 드라이브 연결") { connect() }
                Button("2. 시험 폴더와 파일 만들기") { withToken(create) }
                Button("3. 다른 클라이언트 파일 찾기") { withToken(find) }
                Button("4. 백그라운드 토큰 결과 보기") { write("백그라운드 결과: \(UserDefaults.standard.string(forKey: "bg_result") ?? "아직 없음")") }
                Button("기록 지우기") { log = "" }
                Text(log).font(.system(.caption, design: .monospaced)).textSelection(.enabled)
            }
            .padding()
        }
        .onAppear { write("기기 \(UIDevice.current.model), iOS \(UIDevice.current.systemVersion), 번들 \(Bundle.main.bundleIdentifier ?? "?")") }
        .onChange(of: scenePhase) { _, phase in if phase == .background { backgroundTokenCheck() } }
    }

    private func connect() {
        guard let root = UIApplication.shared.connectedScenes.compactMap({ ($0 as? UIWindowScene)?.keyWindow?.rootViewController }).first else { return }
        GIDSignIn.sharedInstance.signIn(withPresenting: root, hint: nil, additionalScopes: [driveScope]) { result, error in
            if let error { write("연결 실패: \(error.localizedDescription)"); return }
            guard let user = result?.user else { return }
            write("계정: \(user.profile?.email ?? "?")")
            write("받은 범위: \((user.grantedScopes ?? []).joined(separator: ", "))")
        }
    }

    private func create(_ token: String) async throws {
        let folder = try await DriveRest.createFolder(token: token, name: "1일 1일기 백업 확인", props: ["diary.kind": "probe-root", "probe.client": "ios"])
        let file = try await DriveRest.createTextFile(token: token, parentId: folder["id"] as! String, name: "ios-made.txt", text: "iOS 확인 빌드가 만든 파일", props: ["diary.kind": "probe-file", "probe.client": "ios"])
        write("만듦: 폴더 \(folder["name"] ?? ""), 파일 \(file["name"] ?? "") (sha256 \(String((file["sha256Checksum"] as? String ?? "").prefix(12))))")
    }

    private func find(_ token: String) async throws {
        let probe = try await DriveRest.list(token: token, query: DriveRest.probeFiles)
        write("시험 파일 \(probe.count)개")
        for f in probe {
            let by = (f["appProperties"] as? [String: String])?["probe.client"] ?? "?"
            write("  · \(f["name"] ?? "") 만든 쪽 \(by), \(f["createdTime"] ?? "")")
        }
        let legacy = try await DriveRest.list(token: token, query: DriveRest.legacyBackup)
        write("예전 앱 note.db \(legacy.count)개")
        for f in legacy { write("  · \(f["name"] ?? "") \(f["size"] ?? "")바이트, \(f["createdTime"] ?? "")") }
    }

    // 스펙 003 research O4(iOS): 앱이 백그라운드로 간 뒤 키체인에서 로그인을 복원하고 토큰을 받을 수 있는지 본다.
    private func backgroundTokenCheck() {
        var task: UIBackgroundTaskIdentifier = .invalid
        task = UIApplication.shared.beginBackgroundTask { UIApplication.shared.endBackgroundTask(task) }
        GIDSignIn.sharedInstance.restorePreviousSignIn { user, error in
            guard let user else {
                UserDefaults.standard.set("되살리기 실패: \(error?.localizedDescription ?? "?")", forKey: "bg_result")
                UIApplication.shared.endBackgroundTask(task); return
            }
            user.refreshTokensIfNeeded { user, error in
                Task {
                    let at = Date().formatted(date: .omitted, time: .standard)
                    if let token = user?.accessToken.tokenString, let files = try? await DriveRest.list(token: token, query: DriveRest.probeFiles) {
                        UserDefaults.standard.set("\(at) 백그라운드에서 토큰 받음, 시험 파일 \(files.count)개 조회 성공", forKey: "bg_result")
                    } else {
                        UserDefaults.standard.set("\(at) 실패: \(error?.localizedDescription ?? "조회 실패")", forKey: "bg_result")
                    }
                    UIApplication.shared.endBackgroundTask(task)
                }
            }
        }
    }

    private func withToken(_ block: @escaping (String) async throws -> Void) {
        guard let user = GIDSignIn.sharedInstance.currentUser else { write("먼저 1번으로 연결해 주세요."); return }
        user.refreshTokensIfNeeded { user, error in
            guard let token = user?.accessToken.tokenString else { write("토큰 실패: \(error?.localizedDescription ?? "?")"); return }
            Task {
                do { try await block(token) } catch { write("실패: \(error.localizedDescription)") }
            }
        }
    }

    private func write(_ line: String) {
        DispatchQueue.main.async { log += line + "\n" }
    }
}
