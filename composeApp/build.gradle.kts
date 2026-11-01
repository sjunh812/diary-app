// 안드로이드 앱 껍데기. 패키지·서명 키·minSdk는 1.7.4 그대로다(CLAUDE.md 아키텍처, D4).
plugins {
    alias(libs.plugins.diary.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    // 릴리스를 본떠 nonMinifiedRelease(프로필을 만들 때)와 benchmarkRelease(잴 때) 빌드 타입을 만든다.
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "org.sjhstudio.diary"

    defaultConfig {
        applicationId = "org.sjhstudio.diary"
        // 36은 만들고 내지 않은 1.7.5가 썼다. 2.0.0은 37부터다.
        versionCode = 37
        versionName = "2.0.0"
        // 업그레이드 계측 시험(UpgradeVerificationTest)을 돌린다.
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    androidResources {
        // 앱별 언어 설정(Android 13 이상)에 세 언어를 올린다(docs/design.md 여러 언어).
        generateLocaleConfig = true
        // 라이브러리에 든 다른 언어 문구를 빼서 화면이 한 언어로 보이게 한다.
        localeFilters += listOf("en", "ko", "ja")
    }

    buildFeatures {
        compose = true
        // 개발 빌드에서 사용 통계 고정 표를 엄격하게 본다(BuildConfig.DEBUG).
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

baselineProfile {
    // 프로필은 :composeApp:generateBaselineProfile로 손수 만들어 저장소에 둔다(research R10).
    // 릴리스를 빌드할 때마다 만들면 CI와 로컬 빌드가 기기를 요구한다.
    automaticGenerationDuringBuild = false
    mergeIntoMain = true
    dexLayoutOptimization = true
}

dependencies {
    implementation(projects.shared.app)

    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.profileinstaller)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)

    debugImplementation(libs.compose.ui.tooling)
    // 개발 빌드의 화면 모음(ScreenCatalogActivity)이 아직 길이 없는 화면을 직접 띄운다.
    debugImplementation(projects.shared.core.data)
    debugImplementation(projects.shared.core.designsystem)
    debugImplementation(projects.shared.core.ui)
    debugImplementation(libs.okio)
    debugImplementation(projects.shared.core.migration)
    debugImplementation(projects.shared.core.model)
    debugImplementation(projects.shared.feature.migration)
    debugImplementation(projects.shared.feature.settings)
    debugImplementation(libs.kotlinx.datetime)

    // 1.7.4에서 올린 기기를 기기 안에서 확인하는 계측 시험(스펙 001 T050, scripts/legacy/upgrade-test.sh).
    androidTestImplementation(projects.shared.core.database)
    androidTestImplementation(projects.shared.core.model)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)

    baselineProfile(projects.benchmarks)
}
