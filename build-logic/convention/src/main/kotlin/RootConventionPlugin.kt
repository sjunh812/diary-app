import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Exec
import org.gradle.kotlin.dsl.register

// 루트 프로젝트에만 쓴다(research R3).
// 디자인 토큰 검사와 git 훅 설치를 등록한다.
class RootConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        check(this == rootProject) { "diary.root는 루트 프로젝트에만 쓴다" }
        pluginManager.apply("base")

        val designTokenCheck =
            tasks.register<DesignTokenCheckTask>("designTokenCheck") {
                group = "verification"
                description = "화면 코드에 원시 색, 숫자 크기, 코드 속 문구가 없는지 본다"
                sources.from(
                    listOf("shared/feature", "shared/app").map { dir ->
                        fileTree(dir) {
                            include("**/*.kt")
                            exclude("**/build/**", "**/preview/**", "**/*Preview.kt")
                        }
                    }
                )
                baseDirectory.set(layout.projectDirectory)
                report.set(layout.buildDirectory.file("reports/designTokenCheck.txt"))
            }
        tasks.named("check").configure { dependsOn(designTokenCheck) }

        // 커밋 형식 검사 훅을 쓰게 한다(FR-031, research R19).
        // 소스 묶음처럼 git 저장소가 아닌 곳에서는 건너뛴다.
        val gitDirectory = layout.projectDirectory.dir(".git").asFile
        tasks.register<Exec>("installGitHooks") {
            group = "build setup"
            description = "git이 .githooks의 커밋 형식 검사를 쓰게 한다"
            onlyIf { gitDirectory.exists() }
            commandLine("git", "config", "core.hooksPath", ".githooks")
        }
    }
}
