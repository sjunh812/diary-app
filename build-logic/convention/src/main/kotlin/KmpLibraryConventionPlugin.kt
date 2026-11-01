import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.sjhstudio.diary.buildlogic.diaryNamespace
import org.sjhstudio.diary.buildlogic.int
import org.sjhstudio.diary.buildlogic.jvmTarget
import org.sjhstudio.diary.buildlogic.library
import org.sjhstudio.diary.buildlogic.libs

// shared 아래 모든 모듈이 쓴다.
// AGP 9부터 KMP 공유 모듈은 com.android.library 대신 com.android.kotlin.multiplatform.library를 쓴다.
class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        pluginManager.apply("com.android.kotlin.multiplatform.library")

        val moduleNamespace = diaryNamespace
        val compileSdkVersion = libs.int("androidCompileSdk")
        val minSdkVersion = libs.int("androidMinSdk")
        val jvmTargetVersion = libs.jvmTarget

        extensions.configure<KotlinMultiplatformExtension> {
            (this as ExtensionAware)
                .extensions
                .configure(KotlinMultiplatformAndroidLibraryExtension::class.java) {
                    namespace = moduleNamespace
                    compileSdk = compileSdkVersion
                    minSdk = minSdkVersion
                    // 이 플러그인은 단위 시험이 opt-in이다.
                    // 켜지 않으면 androidHostTest가 생기지 않는다.
                    withHostTestBuilder {}.configure {}
                    // 안드로이드 리소스도 opt-in이다.
                    // 켜지 않으면 Compose 문구가 APK에 들어가지 않아 실행 중에 MissingResourceException이 난다.
                    androidResources {
                        enable = true
                    }
                }

            // Apple Silicon 맥만 쓰므로 iosX64는 두지 않는다.
            // 기기(iosArm64)는 실기기 배포와 위젯에 필요하다(research R3).
            iosArm64()
            iosSimulatorArm64()

            sourceSets.getByName("commonTest").dependencies {
                implementation(kotlin("test"))
                implementation(libs.library("kotlinx-coroutines-test"))
                implementation(libs.library("turbine"))
            }
        }

        // Compose를 쓰는 모듈은 모델과 날짜 타입을 안정 타입으로 본다(research R10).
        // 그래야 화면 상태가 바뀌지 않은 줄을 다시 그리지 않는다.
        pluginManager.withPlugin("org.jetbrains.kotlin.plugin.compose") {
            extensions.configure<ComposeCompilerGradlePluginExtension> {
                stabilityConfigurationFiles.add(
                    rootProject.layout.projectDirectory.file("compose_compiler_config.conf")
                )
            }
        }

        // KMP 안드로이드 타깃은 DSL에 compilerOptions가 없어 컴파일 작업에서 잡는다.
        tasks.withType<KotlinJvmCompile>().configureEach {
            compilerOptions.jvmTarget.set(jvmTargetVersion)
        }
    }
}
