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

// 안 쓰는 import 검사
// detekt UnusedImport는 타입 분석이 있어야 돌고, Spotless의 ktlint도 이 규칙을 잡지 못해 따로 둠
// 이름이 본문(주석·KDoc 포함)에 한 번도 없으면 안 쓰는 것으로 봄
// 이름이 드러나지 않는 연산자·위임 함수는 건너뜀
@CacheableTask
abstract class UnusedImportCheckTask : DefaultTask() {
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
                unusedImports(file.readText()).map { (line, name) -> "${file.relativeTo(base).invariantSeparatorsPath}:$line: $name" }
            }
        report.get().asFile.writeText(violations.joinToString(separator = "\n", postfix = if (violations.isEmpty()) "" else "\n"))
        if (violations.isNotEmpty()) {
            throw GradleException("안 쓰는 import ${violations.size}줄.\n" + violations.joinToString(separator = "\n"))
        }
    }

    companion object {
        private val IMPORT = Regex("""^import\s+([\w.`]+?)(?:\s+as\s+(\w+))?\s*;?\s*$""")

        /** 코드에 이름 없이 쓰이는 연산자·위임 함수 */
        private val IMPLICIT =
            setOf(
                "getValue", "setValue", "provideDelegate", "invoke", "get", "set", "contains", "iterator", "next", "hasNext",
                "plus", "minus", "times", "div", "rem", "unaryPlus", "unaryMinus", "not", "inc", "dec", "compareTo", "equals",
                "rangeTo", "rangeUntil", "plusAssign", "minusAssign", "timesAssign", "divAssign", "remAssign"
            )

        /** (줄 번호, 이름) */
        fun unusedImports(text: String): List<Pair<Int, String>> {
            val lines = text.lines()
            val imports =
                lines.mapIndexedNotNull { index, line ->
                    IMPORT.find(line.trim())?.let { match ->
                        val path = match.groupValues[1]
                        if (path.endsWith(".*")) return@mapIndexedNotNull null
                        val name = match.groupValues[2].ifEmpty { path.substringAfterLast('.') }.trim('`')
                        index to name
                    }
                }
            val importLines = imports.mapTo(HashSet()) { it.first }
            val body = lines.filterIndexed { index, line ->
                index !in importLines && !line.trimStart().startsWith("package ")
            }.joinToString("\n")
            return imports
                .filter { (_, name) -> name !in IMPLICIT && !name.startsWith("component") }
                .filterNot { (_, name) -> Regex("""(?<![\w])${Regex.escape(name)}(?![\w])""").containsMatchIn(body) }
                .map { (index, name) -> index + 1 to name }
        }
    }
}
