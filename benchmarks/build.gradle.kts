// 앱을 따로 띄워 시작 시간과 목록 넘기기를 재고 Baseline Profile을 만든다(research R10).
// 앱의 릴리스를 본뜬 nonMinifiedRelease와 benchmarkRelease 변형이 이 모듈에도 생기고 같은 이름의 앱 빌드를 띄운다.
plugins {
    alias(libs.plugins.diary.android.test)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "org.sjhstudio.diary.benchmarks"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":composeApp"
    // 재는 앱을 죽였다 켤 때 시험 APK가 같이 죽지 않게 따로 띄운다.
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

baselineProfile {
    // 붙어 있는 기기에서 만든다. ANDROID_SERIAL로 에뮬레이터만 고른다.
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.uiautomator)
}
