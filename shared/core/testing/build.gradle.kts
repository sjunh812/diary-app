// 여러 시험이 함께 쓰는 대역과 견본 자료
// 시험 의존성으로만 걸어 앱에는 들어가지 않음
plugins {
    alias(libs.plugins.diary.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.shared.core.data)
            // 잠금 시험의 기기 키 대역
            api(projects.shared.core.platform)
            // 백업 시험의 드라이브 대역
            api(projects.shared.core.drive)
            api(libs.kotlinx.coroutines.test)
            // 저장소 시험이 실제 SettingsStore·LockStore를 메모리 DataStore로 감쌈
            api(projects.shared.core.datastore)
            api(libs.androidx.datastore.preferences.core)
            implementation(libs.androidx.paging.common)
        }
    }
}
