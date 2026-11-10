import dev.detekt.gradle.Detekt
import dev.detekt.gradle.DetektCreateBaselineTask
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.sjhstudio.diary.buildlogic.library
import org.sjhstudio.diary.buildlogic.libs

// 루트 프로젝트 전용
// 모듈마다 baseline을 두면 서른 개 가까이 되어 루트에서 소스를 한 번에 읽고 baseline은 하나만
// 설정은 config/detekt/detekt.yml, 기존 지적은 config/detekt/baseline.xml
// 포맷은 spotless(ktlint) 담당, 겹치는 규칙은 detekt.yml에서 끔
class DetektConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        check(this == rootProject) { "diary.detekt는 루트 프로젝트에만 쓴다" }
        pluginManager.apply("dev.detekt")

        val configFile = layout.projectDirectory.file("config/detekt/detekt.yml")
        val baselineFile = layout.projectDirectory.file("config/detekt/baseline.xml")
        val sourceTree =
            fileTree(layout.projectDirectory) {
                include("shared/**/src/**/*.kt", "composeApp/src/**/*.kt", "benchmarks/src/**/*.kt")
                exclude("**/build/**", "**/generated/**")
            }

        extensions.configure<DetektExtension> {
            buildUponDefaultConfig.set(true)
            parallel.set(true)
            config.setFrom(configFile)
            baseline.set(baselineFile)
            source.setFrom(sourceTree)
            // 보고서 경로를 저장소 기준으로 적어 baseline이 컴퓨터마다 달라지지 않음
            basePath.set(layout.projectDirectory)
        }

        dependencies.add("detektPlugins", libs.library("compose-rules-detekt"))

        tasks.withType<Detekt>().configureEach {
            jvmTarget.set("17")
        }
        tasks.withType<DetektCreateBaselineTask>().configureEach {
            jvmTarget.set("17")
        }
    }
}
