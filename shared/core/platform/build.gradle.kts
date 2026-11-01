// 플랫폼마다 다르게 만드는 것(expect/actual): 파일 자리, 잠금 키, 생체 인증, 알림, 사용 통계.
plugins {
    alias(libs.plugins.diary.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.shared.core.model)
            api(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            implementation(libs.androidx.biometric)
            implementation(libs.androidx.lifecycle.process)
        }
    }
}
