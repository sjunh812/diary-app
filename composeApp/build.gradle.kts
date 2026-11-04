// 안드로이드 앱 껍데기. 패키지·서명 키·minSdk는 1.7.4 그대로다(CLAUDE.md 아키텍처, D4).
plugins {
    alias(libs.plugins.diary.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    // 릴리스를 본떠 nonMinifiedRelease(프로필을 만들 때)와 benchmarkRelease(잴 때) 빌드 타입을 만든다.
    alias(libs.plugins.baselineprofile)
    // 오픈소스 고지 목록을 만든다. 앱에 실제로 들어가는 의존성을 다 아는 곳이 여기라 여기에 단다.
    alias(libs.plugins.aboutlibraries)
}

// `./gradlew :composeApp:exportLibraryDefinitions`로 다시 만든다. 의존성을 바꾸면 같은 커밋에서 다시 만든다.
aboutLibraries {
    // 같은 라이브러리의 플랫폼별 묶음(compose-ui와 compose-ui-android 같은)은 하나로 합친다(KotlinConf 앱과 같다).
    library {
        duplicationMode = com.mikepenz.aboutlibraries.plugin.DuplicateMode.MERGE
        duplicationRule = com.mikepenz.aboutlibraries.plugin.DuplicateRule.SIMPLE
    }
    export {
        outputFile = file("../shared/feature/settings/src/commonMain/composeResources/files/aboutlibraries.json")
        variant = "release"
    }
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

    // 고를 때 받는 일기 글꼴 다섯 종(research R17). 받은 뒤 SplitCompat으로 자산을 읽는다.
    dynamicFeatures +=
        setOf(":fonts:gowun_dodum", ":fonts:gowun_batang", ":fonts:nanum_jeomggol", ":fonts:gamja_flower", ":fonts:hi_melody")

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
    // 잠금이 켜졌는지 보고 최근 앱 화면을 가린다(RecentsGuard).
    implementation(projects.shared.core.data)

    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)

    implementation(libs.androidx.activity.compose)
    // 잠금 화면의 생체 인증 창은 FragmentActivity 위에 뜬다.
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.core.ktx)
    // 시작 화면(Android 12 미만도 같은 모양, research R14)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.profileinstaller)
    // 받은 글꼴 모듈의 자산을 앱이 바로 읽게 한다(SplitCompat).
    implementation(libs.play.feature.delivery)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)

    debugImplementation(libs.compose.ui.tooling)
    // 개발 빌드의 화면 모음(ScreenCatalogActivity)이 아직 길이 없는 화면을 직접 띄운다.
    debugImplementation(projects.shared.core.designsystem)
    debugImplementation(projects.shared.core.ui)
    debugImplementation(libs.okio)
    debugImplementation(projects.shared.core.migration)
    debugImplementation(projects.shared.core.model)
    debugImplementation(projects.shared.feature.lock)
    debugImplementation(projects.shared.feature.migration)
    debugImplementation(projects.shared.feature.settings)
    debugImplementation(libs.kotlinx.datetime)
    // 목록 성능 견본(list_performance)이 견본 DB를 60편씩 읽는다.
    debugImplementation(projects.shared.core.database)
    debugImplementation(libs.androidx.paging.compose)

    // 1.7.4에서 올린 기기를 기기 안에서 확인하는 계측 시험(스펙 001 T050, scripts/legacy/upgrade-test.sh).
    androidTestImplementation(projects.shared.core.database)
    androidTestImplementation(projects.shared.core.model)
    // 잠금 키(Android Keystore)는 기기에서만 돈다(스펙 001 T056).
    androidTestImplementation(projects.shared.core.platform)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)

    baselineProfile(projects.benchmarks)
}
