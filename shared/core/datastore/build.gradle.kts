// 설정과 잠금 저장소(DataStore 파일 둘, research R16).
plugins {
    alias(libs.plugins.diary.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.shared.core.model)
            api(libs.kotlinx.coroutines.core)
            // SettingsStore·LockStore가 DataStore를 받으므로 쓰는 쪽도 그 타입을 본다.
            api(libs.androidx.datastore.preferences.core)
        }
    }
}
