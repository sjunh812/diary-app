// 화면이 쓰는 저장소(Repository). DB, DataStore, 플랫폼 기능을 묶는다.
plugins {
    alias(libs.plugins.diary.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.shared.core.model)
            api(libs.kotlinx.coroutines.core)
            api(libs.androidx.paging.common)
            implementation(projects.shared.core.database)
            api(projects.shared.core.datastore)
            implementation(projects.shared.core.platform)
            // 썸네일 파일(ThumbnailRepository)
            implementation(libs.okio)
            // 매일 알림 다음 시각(ReminderRepository)
            implementation(libs.kotlinx.datetime)
        }
        // core/testing이 이 모듈의 인터페이스를 구현하니 시험에서만 건다.
        // main에 걸면 서로를 물고 돈다.
        commonTest.dependencies {
            implementation(projects.shared.core.testing)
            implementation(libs.okio.fakefilesystem)
        }
    }
}
