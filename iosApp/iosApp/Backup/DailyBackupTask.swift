import BackgroundTasks
import Shared
import UIKit

/// 하루 한 번 자동 백업(research R15).
/// 실행이 보장되지 않아서 앱이 뒤로 갈 때와 열릴 때도 본다.
/// 언제 도는지는 시스템이 정하고, 돌 때가 됐는지는 공통 코드가 정한다.
enum DailyBackupTask {
    private static let identifier = "org.sjhstudio.diary.backup.daily"
    private static let day: TimeInterval = 24 * 60 * 60

    /// 앱 시작이 끝나기 전에 등록해야 한다.
    static func register() {
        BGTaskScheduler.shared.register(forTaskWithIdentifier: identifier, using: nil) { task in
            guard let task = task as? BGProcessingTask else { return }
            run(task)
        }
    }

    /// 뒤로 갈 때 다음 작업을 맡기고, 남은 시간 안에서 밀린 백업을 시작한다.
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

    private static func schedule() {
        let request = BGProcessingTaskRequest(identifier: identifier)
        request.requiresNetworkConnectivity = true
        request.earliestBeginDate = Date(timeIntervalSinceNow: day)
        // 시뮬레이터는 지원하지 않아 실패하지만 앱을 열 때 다시 본다.
        try? BGTaskScheduler.shared.submit(request)
    }

    private static func run(_ task: BGProcessingTask) {
        schedule()
        let handle = IosStartupKt.runDailyBackup { task.setTaskCompleted(success: true) }
        task.expirationHandler = { handle.cancel() }
    }
}
