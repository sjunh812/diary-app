import com.android.build.api.dsl.TestExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.sjhstudio.diary.buildlogic.int
import org.sjhstudio.diary.buildlogic.javaVersion
import org.sjhstudio.diary.buildlogic.jvmTarget
import org.sjhstudio.diary.buildlogic.libs

// 다른 모듈의 APK를 띄워 재는 테스트 모듈(:benchmarks)에 쓴다.
class AndroidTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.test")

        extensions.configure<TestExtension> {
            compileSdk = libs.int("androidCompileSdk")

            defaultConfig {
                // Baseline Profile은 안드로이드 9부터 만들 수 있다.
                minSdk = maxOf(libs.int("androidMinSdk"), 28)
                targetSdk = libs.int("androidTargetSdk")
            }

            compileOptions {
                sourceCompatibility = libs.javaVersion
                targetCompatibility = libs.javaVersion
            }
        }

        extensions.configure<KotlinAndroidProjectExtension> {
            compilerOptions {
                jvmTarget.set(libs.jvmTarget)
            }
        }
    }
}
