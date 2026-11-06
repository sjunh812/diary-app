plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    // 실제 앱과 같은 패키지로 시험한다.
    // 디버그 키 지문은 클라우드 콘솔의 "1일 1일기 Debug (2026)" 클라이언트에 등록돼 있다.
    namespace = "org.sjhstudio.diary.drivecheck"
    compileSdk = 35

    defaultConfig {
        applicationId = "org.sjhstudio.diary"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "drive-check"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("com.google.android.gms:play-services-auth:22.0.0")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.work:work-runtime-ktx:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
}
