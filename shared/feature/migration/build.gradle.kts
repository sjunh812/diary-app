// 기록 이전 진행, 결과, 실패 화면
plugins {
    alias(libs.plugins.diary.kmp.feature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared.core.migration)
            implementation(libs.compose.ui.backhandler)
            implementation(libs.kotlinx.datetime)
        }
        androidMain.dependencies {
            // 원본 내보내기 저장 창(SAF)과 메일 앱 열기
            implementation(libs.androidx.activity.compose)
        }
    }
}
