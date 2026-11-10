import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.resources.ResourcesExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.sjhstudio.diary.buildlogic.diaryNamespace
import org.sjhstudio.diary.buildlogic.library
import org.sjhstudio.diary.buildlogic.libs

// shared/feature 아래 모듈 공통
// 기능 모듈은 core에만 기대고 서로를 모름
// 화면 이동은 shared/app이 정함
class KmpFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("diary.kmp.library")
        pluginManager.apply("org.jetbrains.compose")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets.getByName("commonMain").dependencies {
                implementation(project(":shared:core:model"))
                implementation(project(":shared:core:data"))
                implementation(project(":shared:core:designsystem"))
                implementation(project(":shared:core:ui"))
                implementation(libs.library("jb-lifecycle-runtime-compose"))
                implementation(libs.library("jb-lifecycle-viewmodel"))
                implementation(libs.library("jb-lifecycle-viewmodel-compose"))
                // 앱이 꺼졌다 돌아와도 보던 조건 유지(SavedStateHandle)
                implementation(libs.library("jb-lifecycle-viewmodel-savedstate"))
                implementation(libs.library("androidx-navigation3-runtime"))
                implementation(project.dependencies.platform(libs.library("koin-bom")))
                implementation(libs.library("koin-compose-viewmodel"))
            }
            sourceSets.getByName("androidMain").dependencies {
                implementation(libs.library("compose-ui-tooling"))
            }
            sourceSets.getByName("commonTest").dependencies {
                implementation(project(":shared:core:testing"))
            }
        }

        // 화면 문구는 모듈마다 composeResources/values/strings.xml에(research R18)
        // 앱이 모든 모듈 리소스를 패키지 이름 폴더로 모아 모듈마다 패키지가 달라야 함
        val resourcePackage = "$diaryNamespace.resources"
        extensions.configure<ComposeExtension> {
            (this as ExtensionAware).extensions.configure(ResourcesExtension::class.java) {
                publicResClass = false
                packageOfResClass = resourcePackage
                // compose-resources가 designsystem을 거쳐 들어와 기본값(auto)으로는 Res 클래스가 안 생김
                generateResClass = ResourcesExtension.ResourceClassGeneration.Always
            }
        }
    }
}
