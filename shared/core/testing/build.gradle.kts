// 여러 시험이 같이 쓰는 대역과 견본 자료. 시험 의존성으로만 걸고 앱에는 들어가지 않는다.
plugins {
    alias(libs.plugins.diary.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.shared.core.data)
            api(libs.kotlinx.coroutines.test)
            // 저장소 시험이 실제 SettingsStore·LockStore를 메모리 DataStore로 감싼다.
            api(projects.shared.core.datastore)
            api(libs.androidx.datastore.preferences.core)
        }
    }
}
