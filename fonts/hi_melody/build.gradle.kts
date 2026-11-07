// 고를 때 받는 일기 글꼴 하나(자산만, 코드 없음). 받는 방법은 shared/core/platform의 AndroidFontInstaller가 정한다.
plugins {
    alias(libs.plugins.android.dynamic.feature)
}

android {
    namespace = "org.sjhstudio.diary.fonts.hi_melody"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.androidMinSdk.get().toInt()
    }
}

dependencies {
    implementation(projects.composeApp)
}
