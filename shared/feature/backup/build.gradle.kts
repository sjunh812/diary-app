// 설정 > 백업, 복원, 파일로 백업하기, 홈 상태 줄과 권하기 카드
plugins {
    alias(libs.plugins.diary.kmp.feature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared.core.platform)
            implementation(libs.kotlinx.datetime)
            implementation(libs.compose.ui.backhandler)
            implementation(libs.filekit.dialogs.compose)
            implementation(libs.okio)
        }
    }
}
