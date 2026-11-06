// 루트는 플러그인 버전만 고정하고 모듈에는 적용하지 않는다. 모듈 설정은 build-logic의 convention plugin이 한다.
plugins {
    alias(libs.plugins.aboutlibraries) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.android.dynamic.feature) apply false
    alias(libs.plugins.baselineprofile) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room3) apply false
    alias(libs.plugins.spotless)
    // 디자인 토큰 검사(designTokenCheck)와 git 훅 설치(installGitHooks)
    alias(libs.plugins.diary.root)
}

// NIA처럼 Spotless로 ktlint를 돌린다(research R20). 규칙은 .editorconfig에 있다.
// 레거시 app/과 확인용 빌드(scripts/)는 손대지 않는다.
spotless {
    val ktlintVersion = libs.versions.ktlint.get()
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**", "app/**", "scripts/**", ".claude/**")
        ktlint(ktlintVersion)
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**", "app/**", "scripts/**", ".claude/**")
        ktlint(ktlintVersion)
    }
}
