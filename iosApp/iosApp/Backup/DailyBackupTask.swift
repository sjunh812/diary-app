import BackgroundTasks
import Shared
import UIKit

/// 하루 한 번 자동 백업(research R15)
/// 실행이 보장되지 않아 앱이 뒤로 갈 때와 열릴 때도 확인
/// 언제 돌지는 시스템이, 돌 때가 됐는지는 공통 코드가 정함
enum DailyBackupTask {
    private static let identifier = "org.sjhstudio.diary.backup.daily"
    private static let day: TimeInterval = 24 * 60 * 60

    /// 앱 시작이 끝나기 전에 등록해야 함
    static func register() {
        BGTaskScheduler.shared.register(forTaskWithIdentifier: identifier, using: nil) { task in
            guard let task = task as? BGProcessingTask else { return }
            run(task)
        }
    }

    /// 뒤로 갈 때 다음 작업을 맡기고 남은 시간 안에 밀린 백업 시작
    static func enteredBackground() {
        schedule()
        var background = UIBackgroundTaskIdentifier.invalid
        var handle: BackupTaskHandle?
        let finish = {
            guard background != .invalid else { return }
            UIApplication.shared.endBackgroundTask(background)
            background = .invalid
        }
        background = UIApplication.shared.beginBackgroundTask(withName: identifier) {
            handle?.cancel()
            finish()
        }
        handle = IosStartupKt.runDailyBackup { DispatchQueue.main.async(execute: finish) }
    }

    /// 이미 맡긴 작업이 있으면 그대로 둠
    /// 뒤로 갈 때마다 새로 맡기면 시작 시각이 계속 하루씩 밀림
    private static func schedule() {
        BGTaskScheduler.shared.getPendingTaskRequests { pending in
            guard !pending.contains(where: { $0.identifier == identifier }) else { return }
            let request = BGProcessingTaskRequest(identifier: identifier)
            request.requiresNetworkConnectivity = true
            request.earliestBeginDate = Date(timeIntervalSinceNow: day)
            // 시뮬레이터는 미지원이라 실패하지만 앱을 열 때 다시 확인
            try? BGTaskScheduler.shared.submit(request)
        }
    }

    private static func run(_ task: BGProcessingTask) {
        schedule()
        let handle = IosStartupKt.runDailyBackup { task.setTaskCompleted(success: true) }
        task.expirationHandler = { handle.cancel() }
    }
}
