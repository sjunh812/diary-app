// 개발자의 편지, 온보딩, 새 소식
plugins {
    alias(libs.plugins.diary.kmp.feature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared.core.platform)
            implementation(libs.compose.ui.backhandler)
        }
    }
}
