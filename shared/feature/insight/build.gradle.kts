// 돌아보기(기분 통계) 화면.
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
