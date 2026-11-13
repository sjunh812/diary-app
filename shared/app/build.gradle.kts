import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.TestExecutable
import org.jetbrains.kotlin.konan.target.HostManager

// 시작 관문(시작 → 잠금 → 이전)과 화면 전환 뼈대, Koin 모듈 모음
// 안드로이드는 composeApp이 감싸고, iOS는 이 모듈을 static framework Shared로 받음(research R22)
plugins {
    alias(libs.plugins.diary.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

/** 지금 고른 Xcode의 Swift 라이브러리 폴더. CI는 DEVELOPER_DIR로 Xcode를 고름 */
val swiftToolchainLibs: Provider<String> =
    providers.exec { commandLine("xcode-select", "-p") }.standardOutput.asText.map {
        "${it.trim()}/Toolchains/XcodeDefault.xctoolchain/usr/lib/swift"
    }

kotlin {
    targets.withType<KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
        // RevenueCat KMP SDK가 자기 빌드 맥의 Xcode 경로를 박아 시험 실행 파일이 Swift 호환 라이브러리를 못 찾음
        // 앱은 Xcode가 링크해 상관없음. SDK가 고치면 지움(github.com/RevenueCat/purchases-kmp/issues/1067)
        if (HostManager.hostIsMac) {
            val platform = if (konanTarget.name.contains("simulator") || konanTarget.name.endsWith("x64")) "iphonesimulator" else "iphoneos"
            binaries.withType<TestExecutable>().configureEach { linkerOpts("-L${swiftToolchainLibs.get()}/$platform") }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared.core.model)
            implementation(projects.shared.core.data)
            implementation(projects.shared.core.designsystem)
            implementation(projects.shared.core.ui)
            implementation(projects.shared.core.platform)
            implementation(projects.shared.core.database)
            implementation(projects.shared.core.datastore)
            implementation(projects.shared.core.migration)
            implementation(projects.shared.core.backup)
            implementation(projects.shared.core.drive)
            implementation(libs.okio)
            implementation(libs.kotlinx.datetime)
            implementation(projects.shared.feature.lock)
            implementation(projects.shared.feature.migration)
            implementation(projects.shared.feature.settings)
            implementation(projects.shared.feature.write)
            implementation(projects.shared.feature.home)
            implementation(projects.shared.feature.calendar)
            implementation(projects.shared.feature.search)
            implementation(projects.shared.feature.insight)
            implementation(projects.shared.feature.trash)
            implementation(projects.shared.feature.backup)
            implementation(projects.shared.feature.welcome)
            implementation(projects.shared.feature.plus)

            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.resources)
            implementation(libs.compose.ui.backhandler)
            implementation(libs.androidx.navigation3.runtime)
            implementation(libs.jb.navigation3.ui)
            implementation(libs.jb.lifecycle.runtime.compose)
            implementation(libs.jb.lifecycle.viewmodel.compose)
            implementation(libs.jb.lifecycle.viewmodel.navigation3)
            implementation(libs.kotlinx.serialization.core)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
        }
        androidMain.dependencies {
            implementation(libs.androidx.navigation3.ui)
            // 화면 모드를 상태 막대와 창 바탕에 알림(PlatformTheme.android)
            implementation(libs.androidx.activity.compose)
            implementation(libs.koin.android)
            // 자동 백업(AndroidBackupScheduler)
            implementation(libs.androidx.work.runtime)
            // 광고(AdMobPlayer, NativeAdCard). R1은 안드로이드만 냄
            implementation(libs.play.services.ads)
            // 사용 통계와 비정상 종료 보고(FirebaseTelemetrySink, research R21)
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.analytics)
            implementation(libs.firebase.crashlytics)
            // 전면 광고를 앱 업데이트 없이 끔(FirebaseAdSwitch)
            implementation(libs.firebase.config)
            // 구매와 구독 확인(RevenueCatBilling). R1은 안드로이드만 내서 KMP 묶음 대신 안드로이드 SDK
            implementation(libs.revenuecat.purchases)
        }
        iosMain.dependencies {
            // 구매와 구독 확인(IosBilling). iOS SDK가 들어 있어 Xcode에 따로 넣지 않음
            implementation(libs.revenuecat.kmp.core)
        }
        commonTest.dependencies {
            implementation(projects.shared.core.testing)
        }
    }
}

compose.resources {
    publicResClass = false
    packageOfResClass = "org.sjhstudio.diary.app.resources"
    generateResClass = always
}
