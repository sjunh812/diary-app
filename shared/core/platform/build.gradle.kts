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
            // 고를 때 받는 일기 글꼴(AndroidFontInstaller)
            implementation(libs.play.feature.delivery)
            // 구글 드라이브 권한 받기(AndroidDriveAuthorizer)
            implementation(libs.play.services.auth)
            // 현재 위치 가져오기(AndroidPlaceFinder)
            implementation(libs.play.services.location)
            implementation(libs.androidx.core.ktx)
        }
    }
}
