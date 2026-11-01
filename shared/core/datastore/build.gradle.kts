// 설정과 잠금 저장소(DataStore 파일 둘, research R16).
plugins {
    alias(libs.plugins.diary.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.shared.core.model)
            api(libs.kotlinx.coroutines.core)
            implementation(libs.androidx.datastore.preferences.core)
        }
    }
}
