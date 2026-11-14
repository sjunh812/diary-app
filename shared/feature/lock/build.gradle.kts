// 잠금 화면, 비밀번호 정하기·바꾸기, 잊어버렸을 때, 잠금 설정
plugins {
    alias(libs.plugins.diary.kmp.feature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // 생체 인증과 폰 화면 잠금 확인(DeviceAuthenticator)
            implementation(projects.shared.core.platform)
            implementation(libs.compose.ui.backhandler)
        }
        androidMain.dependencies {
            // 생체 인증 창이 FragmentActivity 위에 뜸
            implementation(libs.androidx.biometric)
        }
    }
}
