// 여러 시험이 같이 쓰는 대역과 고정 자료. 시험 의존성으로만 걸고 앱에는 들어가지 않는다.
plugins {
    alias(libs.plugins.diary.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.shared.core.data)
            api(libs.kotlinx.coroutines.test)
        }
    }
}
