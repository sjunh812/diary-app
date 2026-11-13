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
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room3) apply false
    alias(libs.plugins.spotless)
    // 디자인 토큰 검사(designTokenCheck)와 git 훅 설치(installGitHooks)
    alias(libs.plugins.diary.root)
    // 코드 냄새와 Compose 규칙 검사(detekt)
    alias(libs.plugins.diary.detekt)
}

// NIA처럼 Spotless로 ktlint를 돌린다(research R20). 규칙은 .editorconfig에 있다.
// 대상은 소스 폴더별 파일 트리로 준다. 글자 패턴("**/*.kt")으로 주면 저장소 맨 위부터 훑어
// 메인 체크아웃 안의 작업 복사본(.claude/worktrees)과 그 빌드 산출물까지 들어가 메모리가 모자란다.
// 레거시 app/과 확인용 빌드(scripts/)는 손대지 않는다.
val formattedDirs = listOf("shared", "composeApp", "benchmarks", "build-logic", "fonts")

fun sources(pattern: String) = formattedDirs.map { dir ->
    fileTree(dir) {
        include(pattern)
        exclude("**/build/**")
    }
}

spotless {
    val ktlintVersion = libs.versions.ktlint.get()
    kotlin {
        target(sources("**/*.kt"))
        ktlint(ktlintVersion)
    }
    kotlinGradle {
        target(sources("**/*.gradle.kts") + files("build.gradle.kts", "settings.gradle.kts"))
        ktlint(ktlintVersion)
    }
}
