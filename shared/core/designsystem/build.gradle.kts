// docs/design.md의 토큰과 컴포넌트
// 화면 코드는 이 모듈의 DiaryTheme만 씀(헌법 V)
plugins {
    alias(libs.plugins.diary.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.compose.runtime)
            api(libs.compose.foundation)
            api(libs.compose.ui)
            api(libs.compose.resources)
            api(libs.compose.ui.toolingPreview)
            // 시트·대화상자 같은 Material 컴포넌트를 우리 토큰으로 감싸는 용도로만
            implementation(libs.compose.material3)
        }
        androidMain.dependencies {
            implementation(libs.compose.ui.tooling)
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "org.sjhstudio.diary.core.designsystem.resources"
    generateResClass = always
}
