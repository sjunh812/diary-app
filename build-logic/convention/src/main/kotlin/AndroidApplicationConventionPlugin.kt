import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.sjhstudio.diary.buildlogic.int
import org.sjhstudio.diary.buildlogic.javaVersion
import org.sjhstudio.diary.buildlogic.jvmTarget
import org.sjhstudio.diary.buildlogic.libs

// composeApp에 쓴다.
// AGP 9부터 코틀린 지원이 플러그인에 들어와 있어 kotlin-android를 따로 적용하지 않는다.
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")

        extensions.configure<ApplicationExtension> {
            compileSdk = libs.int("androidCompileSdk")

            defaultConfig {
                minSdk = libs.int("androidMinSdk")
                targetSdk = libs.int("androidTargetSdk")
            }

            compileOptions {
                sourceCompatibility = libs.javaVersion
                targetCompatibility = libs.javaVersion
                // minSdk 24를 지키는데 kotlinx-datetime이 Android 8(API 26)부터 있는 java.time을 쓴다.
                // 켜지 않으면 Android 7 기기에서 앱이 켜자마자 꺼진다(2026-11-02 API 24 업그레이드 시험에서 찾음).
                isCoreLibraryDesugaringEnabled = true
            }
        }

        dependencies.add("coreLibraryDesugaring", libs.findLibrary("android-desugar-jdk-libs").get())

        extensions.configure<KotlinAndroidProjectExtension> {
            compilerOptions {
                jvmTarget.set(libs.jvmTarget)
            }
        }

        // 처음 빌드할 때 커밋 형식 검사 훅이 켜지게 한다(research R19).
        tasks.matching { it.name == "preBuild" }.configureEach {
            dependsOn(":installGitHooks")
        }
    }
}
