// 설정 틀과 하위 화면(글꼴, 글자 크기, 화면 모드, 잠금, 알림, 이전 결과, 고지).
plugins {
    alias(libs.plugins.diary.kmp.feature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // 기록 이전 결과(M5)가 이전 상태와 결과를 읽는다.
            implementation(projects.shared.core.migration)
            implementation(libs.kotlinx.datetime)
        }
    }
}
