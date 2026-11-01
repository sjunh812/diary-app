// 앱 전체가 같이 쓰는 불변 모델(data-model.md). 다른 모듈에 기대지 않는다.
plugins {
    alias(libs.plugins.diary.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.datetime)
        }
    }
}
