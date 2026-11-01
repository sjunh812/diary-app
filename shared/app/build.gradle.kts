import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

// 시작 관문(시작 → 잠금 → 이전)과 화면 전환 뼈대, Koin 모듈 모음.
// 안드로이드는 composeApp이 감싸고, iOS는 이 모듈을 static framework Shared로 받는다(research R22).
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
            implementation(libs.okio)
            implementation(libs.kotlinx.datetime)
            implementation(projects.shared.feature.lock)
            implementation(projects.shared.feature.migration)
            implementation(projects.shared.feature.settings)

            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.resources)
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
