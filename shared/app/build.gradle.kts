import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

// 시작 관문(시작 → 잠금 → 이전)과 화면 전환 뼈대, Koin 모듈 모음
// 안드로이드는 composeApp이 감싸고, iOS는 이 모듈을 static framework Shared로 받음(research R22)
plugins {
    alias(libs.plugins.diary.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    targets.withType<KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = "Shared"
            isStatic = true
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
