import androidx.room3.gradle.RoomExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.sjhstudio.diary.buildlogic.library
import org.sjhstudio.diary.buildlogic.libs

// shared/core/database용(research R4)
// 스키마 JSON을 저장소에 남겨 버전을 올릴 때 바뀐 열을 비교하고 이전 시험에 씀
class RoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("diary.kmp.library")
        pluginManager.apply("com.google.devtools.ksp")
        pluginManager.apply("androidx.room3")

        extensions.configure<RoomExtension> {
            schemaDirectory("$projectDir/schemas")
        }

        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets.getByName("commonMain").dependencies {
                implementation(libs.library("androidx-room3-runtime"))
                implementation(libs.library("androidx-sqlite-bundled"))
            }
        }

        // KMP에서 KSP는 타깃마다 따로 걸어야 함
        dependencies {
            listOf("kspAndroid", "kspIosArm64", "kspIosSimulatorArm64").forEach { configuration ->
                add(configuration, libs.library("androidx-room3-compiler"))
            }
        }
    }
}
