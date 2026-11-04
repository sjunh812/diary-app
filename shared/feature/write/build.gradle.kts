// 쓰기와 고치기 화면.
plugins {
    alias(libs.plugins.diary.kmp.feature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.datetime)
        }
    }
}
