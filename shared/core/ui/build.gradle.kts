// 창 폭 등급, 공통 상태 화면, 미리보기 도구.
plugins {
    alias(libs.plugins.diary.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.shared.core.designsystem)
            api(libs.compose.material3.adaptive)
            // 일기 사진 칸. 앱 저장소 파일만 읽는다(네트워크 없음).
            implementation(libs.coil.compose)
        }
    }
}

compose.resources {
    publicResClass = false
    packageOfResClass = "org.sjhstudio.diary.core.ui.resources"
    generateResClass = always
}
