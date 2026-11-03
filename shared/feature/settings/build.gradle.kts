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
            // 설정을 바꾼 사용 통계(Telemetry)
            implementation(projects.shared.core.platform)
        }
        androidMain.dependencies {
            // 매일 알림을 켤 때 알림 권한 창을 띄운다(안드로이드 13부터).
            implementation(libs.androidx.activity.compose)
        }
    }
}
