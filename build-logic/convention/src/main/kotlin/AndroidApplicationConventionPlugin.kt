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
            }
        }

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
