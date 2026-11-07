// 찾기 화면.
plugins {
    alias(libs.plugins.diary.kmp.feature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.androidx.paging.common)
            implementation(libs.androidx.paging.compose)
            implementation(libs.kotlinx.datetime)
            // 글꼴 파일 자리
            implementation(projects.shared.core.platform)
        }
    }
}
