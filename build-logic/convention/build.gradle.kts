import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "org.sjhstudio.diary.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

// 플러그인은 각 모듈이 적용하고 여기서는 설정 타입만 쓴다.
// 런타임까지 끌고 가면 플러그인 버전이 두 벌로 갈린다.
dependencies {
    compileOnly(libs.agp.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.composeCompiler.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.room3.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "diary.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidTest") {
            id = "diary.android.test"
            implementationClass = "AndroidTestConventionPlugin"
        }
        register("kmpLibrary") {
            id = "diary.kmp.library"
            implementationClass = "KmpLibraryConventionPlugin"
        }
        register("kmpFeature") {
            id = "diary.kmp.feature"
            implementationClass = "KmpFeatureConventionPlugin"
        }
        register("room") {
            id = "diary.room"
            implementationClass = "RoomConventionPlugin"
        }
        register("root") {
            id = "diary.root"
            implementationClass = "RootConventionPlugin"
        }
    }
}
