import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

// 화면 코드가 디자인 시스템 토큰과 문자열 리소스만 쓰는지 본다(FR-028, FR-030, research R18).
// 디자인 시스템 모듈과 미리보기 파일은 루트 등록에서 뺀다.
// 줄 단위 문자열 검사라 주석 줄은 건너뛴다.
@CacheableTask
abstract class DesignTokenCheckTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    @get:Internal
    abstract val baseDirectory: DirectoryProperty

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun check() {
        val base = baseDirectory.get().asFile
        val violations =
            sources.files.sorted().flatMap { file ->
                file.readLines().mapIndexedNotNull { index, line ->
                    val code = line.trimStart()
                    if (code.startsWith("//") || code.startsWith("*") || code.startsWith("/*")) return@mapIndexedNotNull null
                    val rule = RULES.firstOrNull { it.pattern.containsMatchIn(line) } ?: return@mapIndexedNotNull null
                    "${file.relativeTo(base).invariantSeparatorsPath}:${index + 1}: ${rule.message}"
                }
            }
        report.get().asFile.writeText(violations.joinToString(separator = "\n", postfix = if (violations.isEmpty()) "" else "\n"))
        if (violations.isNotEmpty()) {
            throw GradleException(
                "디자인 토큰 검사에서 ${violations.size}곳이 걸렸다.\n" + violations.joinToString(separator = "\n")
            )
        }
    }

    private class Rule(val pattern: Regex, val message: String)

    private companion object {
        val RULES =
            listOf(
                Rule(Regex("""Color\(0x"""), "색은 DiaryTheme 색 토큰으로 쓴다"),
                Rule(Regex("""(?<![\w.])\d+(\.\d+)?f?\.(dp|sp)\b"""), "크기는 간격·모서리·글자 토큰으로 쓴다"),
                Rule(Regex("""RoundedCornerShape\(\s*\d"""), "모서리는 DiaryRadius 토큰으로 쓴다"),
                Rule(Regex("""MaterialTheme\.colorScheme"""), "MaterialTheme 대신 DiaryTheme 색을 쓴다"),
                Rule(Regex("""MaterialTheme\.typography"""), "MaterialTheme 대신 DiaryTheme 글자를 쓴다"),
                Rule(Regex("""\bText\(\s*""""), "화면 문구는 문자열 리소스에 둔다")
            )
    }
}
