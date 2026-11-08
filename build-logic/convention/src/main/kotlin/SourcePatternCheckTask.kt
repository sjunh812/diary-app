import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

// 코드에서 금지한 꼴을 줄 단위로 찾는 검사의 공통 틀.
// 디자인 토큰 검사와 코루틴 안전 검사가 규칙만 바꿔 쓴다.
// 줄 단위 문자열 검사라 주석 줄은 건너뛴다.
abstract class SourcePatternCheckTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    @get:Internal
    abstract val baseDirectory: DirectoryProperty

    @get:OutputFile
    abstract val report: RegularFileProperty

    /** 실패 문구 앞에 붙는 검사 이름. */
    @get:Internal
    protected abstract val checkName: String

    @get:Internal
    protected abstract val rules: List<PatternRule>

    @TaskAction
    fun check() {
        val base = baseDirectory.get().asFile
        val violations =
            sources.files.sorted().flatMap { file ->
                file.readLines().mapIndexedNotNull { index, line ->
                    val code = line.trimStart()
                    if (code.startsWith("//") || code.startsWith("*") || code.startsWith("/*")) return@mapIndexedNotNull null
                    val rule = rules.firstOrNull { it.pattern.containsMatchIn(line) } ?: return@mapIndexedNotNull null
                    "${file.relativeTo(base).invariantSeparatorsPath}:${index + 1}: ${rule.message}"
                }
            }
        report.get().asFile.writeText(violations.joinToString(separator = "\n", postfix = if (violations.isEmpty()) "" else "\n"))
        if (violations.isNotEmpty()) {
            throw GradleException("$checkName 검사에서 ${violations.size}곳이 걸렸다.\n" + violations.joinToString(separator = "\n"))
        }
    }
}

class PatternRule(val pattern: Regex, val message: String)
