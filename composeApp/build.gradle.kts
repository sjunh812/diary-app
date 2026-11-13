import java.util.Properties

// 안드로이드 앱 껍데기
// 패키지·서명 키·minSdk는 1.7.4 그대로(CLAUDE.md 아키텍처, D4)
plugins {
    alias(libs.plugins.diary.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    // 릴리스를 본떠 nonMinifiedRelease(프로필을 만들 때)와 benchmarkRelease(잴 때) 빌드 타입을 만듦
    alias(libs.plugins.baselineprofile)
    // 오픈소스 고지 목록 생성
    // 앱에 실제로 들어가는 의존성을 다 아는 곳이라 여기에 둠
    alias(libs.plugins.aboutlibraries)
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
}

// Firebase 설정 파일은 저장소에 두지 않음
// 파일이 있는 빌드만 사용 통계와 비정상 종료 보고를 켬
val firebaseConfigured = file("google-services.json").exists()
if (firebaseConfigured) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}

// `./gradlew :composeApp:exportLibraryDefinitions`로 다시 만듦
// 의존성을 바꾸면 같은 커밋에서 다시 만듦
aboutLibraries {
    // 의존성이 아닌 것(Lucide 아이콘 경로)과 iOS에만 들어가는 SDK를 더함
    collect {
        configPath = file("../config/aboutlibraries")
    }
    // 같은 라이브러리의 플랫폼별 묶음(compose-ui와 compose-ui-android 같은)은 하나로 합침(KotlinConf 앱 방식)
    library {
        duplicationMode = com.mikepenz.aboutlibraries.plugin.DuplicateMode.MERGE
        duplicationRule = com.mikepenz.aboutlibraries.plugin.DuplicateRule.SIMPLE
    }
    export {
        outputFile = file("../shared/feature/settings/src/commonMain/composeResources/files/aboutlibraries.json")
        variant = "release"
    }
}

fun localProperty(key: String, fallback: String): String {
    val file = rootProject.file("local.properties")
    if (!file.exists()) return fallback
    return Properties().apply { file.inputStream().use { load(it) } }.getProperty(key) ?: fallback
}

/** Google 공개 시험 광고 단위. 실제 광고를 누르면 무효 트래픽이 됨 */
val testAds = mapOf(
    "adNative" to "ca-app-pub-3940256099942544/2247696110",
    "adInterstitial" to "ca-app-pub-3940256099942544/1033173712",
    "adRewarded" to "ca-app-pub-3940256099942544/5224354917"
)

// 서명 정보는 저장소에 두지 않음. CI는 환경 변수로 받고(.github/workflows/release.yml), 값이 없으면 서명 없는 빌드
val releaseKeystorePath = providers.environmentVariable("DIARY_KEYSTORE_PATH").orNull

android {
    namespace = "org.sjhstudio.diary"

    defaultConfig {
        applicationId = "org.sjhstudio.diary"
        // 36은 만들고 내지 않은 1.7.5가 씀
        // 2.0.0은 37부터. 37은 첫 내부 테스트에 올려 다시 못 씀
        versionCode = 40
        versionName = "2.0.0"
        // 업그레이드 계측 시험(UpgradeVerificationTest)을 돌림
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 광고 ID는 저장소에 두지 않고 local.properties에서 읽음
        // 단위가 없으면 앱이 광고를 부르지 않음
        // 앱 ID는 없으면 SDK가 앱을 끄므로 Google 시험 ID를 기본으로 둠
        manifestPlaceholders += mapOf(
            "adAppId" to localProperty("admob.app.id", "ca-app-pub-3940256099942544~3347511713"),
            "adNative" to localProperty("admob.native", ""),
            "adInterstitial" to localProperty("admob.interstitial", ""),
            "adRewarded" to localProperty("admob.rewarded", ""),
            // 값이 없으면 구매가 없는 빌드
            "revenueCatKey" to localProperty("revenuecat.android", ""),
            // 개발 빌드는 보내지 않음
            // 확인할 때만 local.properties의 firebase.debug=true로 켬
            "firebaseCollection" to "true"
        )
    }

    androidResources {
        // 앱별 언어 설정(Android 13 이상)에 세 언어를 올림(docs/design.md 여러 언어)
        generateLocaleConfig = true
        // 라이브러리에 든 다른 언어 문구를 빼서 화면이 한 언어로 보이게 함
        localeFilters += listOf("en", "ko", "ja")
    }

    // 언어별로 나누면 기기 언어 조각만 깔려, 앱 언어를 따로 고른 사람의 알림·앱 이름이 기기 언어로 나옴
    // 세 언어뿐이라 함께 넣음
    bundle {
        language {
            enableSplit = false
        }
    }

    // 고를 때 받는 일기 글꼴 다섯 종(research R17)
    // 받은 뒤 SplitCompat으로 자산을 읽음
    dynamicFeatures +=
        setOf(":fonts:gowun_batang", ":fonts:kimjungchul_myungjo", ":fonts:nanum_jeomggol", ":fonts:sejong_geulggot")

    // adb로 깐 개발 빌드는 Play에서 글꼴 모듈을 받을 수 없어 iOS 개발 빌드처럼 앱 안에 담음
    // 출시 빌드에는 들어가지 않고 Play 필요할 때 설치 모듈로 받음
    sourceSets.getByName("debug").assets.directories.addAll(
        listOf("gowun_batang", "kimjungchul_myungjo", "nanum_jeomggol", "sejong_geulggot").map {
            "../fonts/$it/src/main/assets"
        }
    )

    buildFeatures {
        compose = true
        // 개발 빌드에서 사용 통계 고정 표를 엄격하게 봄(BuildConfig.DEBUG)
        buildConfig = true
    }

    signingConfigs {
        if (releaseKeystorePath != null) {
            create("release") {
                storeFile = file(releaseKeystorePath)
                storePassword = providers.environmentVariable("DIARY_KEYSTORE_PASSWORD").get()
                keyAlias = providers.environmentVariable("DIARY_KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("DIARY_KEY_PASSWORD").get()
            }
        }
    }

    buildTypes {
        // 개발 빌드는 Google 시험 광고만 받음
        // 실제 광고를 누르면 무효 트래픽이 됨
        debug {
            manifestPlaceholders += testAds + mapOf("firebaseCollection" to localProperty("firebase.debug", "false"))
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release")
            // DB 네이티브 라이브러리에서 꺼지면 Play가 스택의 함수 이름을 읽을 수 있게 기호 표를 올림
            ndk { debugSymbolLevel = "SYMBOL_TABLE" }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

baselineProfile {
    // 프로필은 :composeApp:generateBaselineProfile로 손수 만들어 저장소에 둠(research R10)
    // 릴리스를 빌드할 때마다 만들면 CI와 로컬 빌드가 기기를 요구함
    automaticGenerationDuringBuild = false
    mergeIntoMain = true
    dexLayoutOptimization = true
}

dependencies {
    implementation(projects.shared.app)
    // 잠금이 켜졌는지 보고 최근 앱 화면을 가림(RecentsGuard)
    implementation(projects.shared.core.data)
    // 구글 드라이브 동의 창을 띄움
    implementation(projects.shared.core.platform)

    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)

    implementation(libs.androidx.activity.compose)
    // 잠금 화면의 생체 인증 창은 FragmentActivity 위에 뜸
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.core.ktx)
    // 시작 화면(Android 12 미만도 같은 모양, research R14)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.profileinstaller)
    // 받은 글꼴 모듈의 자산을 앱이 바로 읽게 함(SplitCompat)
    implementation(libs.play.feature.delivery)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)

    debugImplementation(libs.compose.ui.tooling)
    // 개발 빌드의 화면 모음(ScreenCatalogActivity)이 아직 길이 없는 화면을 직접 띄움
    debugImplementation(projects.shared.core.datastore)
    debugImplementation(projects.shared.core.designsystem)
    debugImplementation(projects.shared.core.ui)
    debugImplementation(libs.okio)
    debugImplementation(projects.shared.core.migration)
    debugImplementation(projects.shared.core.model)
    debugImplementation(projects.shared.feature.backup)
    debugImplementation(projects.shared.feature.lock)
    debugImplementation(projects.shared.feature.migration)
    debugImplementation(projects.shared.feature.settings)
    debugImplementation(projects.shared.feature.welcome)
    debugImplementation(libs.kotlinx.datetime)
    // 목록 성능 견본(list_performance)이 견본 DB를 60편씩 읽음
    debugImplementation(projects.shared.core.database)
    debugImplementation(libs.androidx.paging.compose)

    // 1.7.4에서 올린 기기를 기기 안에서 확인하는 계측 시험(스펙 001 T050, scripts/legacy/upgrade-test.sh)
    androidTestImplementation(projects.shared.core.database)
    androidTestImplementation(projects.shared.core.model)
    // 잠금 키(Android Keystore) 시험은 기기에서만 돌아감(스펙 001 T056)
    androidTestImplementation(projects.shared.core.platform)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)

    baselineProfile(projects.benchmarks)
}

// 측정·프로필 생성 빌드(Baseline Profile 플러그인이 만듦)도 시험 광고만, 사용 통계는 보내지 않음
// 출시 빌드를 본떠 실제 광고 ID를 물려받으면 시험 기기에서 실제 광고가 뜸
// 플러그인이 출시 설정을 나중에 덮어써서 변형 단계에서 바꿈
androidComponents {
    onVariants { variant ->
        if (variant.buildType == "benchmarkRelease" || variant.buildType == "nonMinifiedRelease") {
            (testAds + mapOf("firebaseCollection" to "false")).forEach { (key, value) -> variant.manifestPlaceholders.put(key, value) }
        }
    }
}
