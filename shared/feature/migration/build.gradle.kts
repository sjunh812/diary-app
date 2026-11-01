// 기록 이전 진행, 결과, 실패 화면.
plugins {
    alias(libs.plugins.diary.kmp.feature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared.core.migration)
        }
    }
}
