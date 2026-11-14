import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
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
import org.w3c.dom.Element

// 화면 문구가 세 언어에 다 있는지 검사(docs/design.md 여러 언어, research R18)
// values(영어) 기준으로 values-ko, values-ja의 이름·꼴·%1$s 자리가 같은지 비교
// 개수 문구는 영어 one·other, 한국어·일본어 other가 있어야 함
// Compose 리소스는 \' 와 \" 를 풀지 않아 역슬래시가 화면에 그대로 보여 막음
@CacheableTask
abstract class StringResourceCheckTask : DefaultTask() {
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
        val defaults = sources.files.filter { it.parentFile.name == "values" }.sorted()
        val problems =
            defaults.flatMap { english ->
                val reference = parse(english)
                val where = english.relativeTo(base).invariantSeparatorsPath
                val ownProblems = reference.plurals("one", where) + reference.plurals("other", where) + reference.escapes(where)
                ownProblems +
                    LOCALES.flatMap { locale ->
                        val file = File(english.parentFile.parentFile, "values-$locale/${english.name}")
                        if (!file.exists()) {
                            listOf("$where: values-$locale/${english.name}이 없다")
                        } else {
                            compare(reference, parse(file), file.relativeTo(base).invariantSeparatorsPath)
                        }
                    }
            }
        report.get().asFile.writeText(problems.joinToString(separator = "\n", postfix = if (problems.isEmpty()) "" else "\n"))
        if (problems.isNotEmpty()) {
            throw GradleException("문자열 검사에서 ${problems.size}곳이 걸렸다.\n" + problems.joinToString(separator = "\n"))
        }
    }

    private fun compare(reference: Strings, translated: Strings, where: String): List<String> = buildList {
        (reference.entries.keys - translated.entries.keys).forEach { add("$where: $it 이 없다") }
        (translated.entries.keys - reference.entries.keys).forEach { add("$where: $it 은 영어(values)에 없다") }
        reference.entries.forEach { (name, english) ->
            val other = translated.entries[name] ?: return@forEach
            if (other.kind != english.kind) add("$where: $name 이 영어는 ${english.kind}, 여기는 ${other.kind}다")
            if (other.placeholders != english.placeholders) {
                add("$where: $name 의 자리 ${other.placeholders.sorted()}가 영어 ${english.placeholders.sorted()}와 다르다")
            }
        }
        addAll(translated.plurals("other", where))
        addAll(translated.escapes(where))
    }

    private fun Strings.plurals(quantity: String, where: String): List<String> =
        entries.filter { it.value.kind == "plurals" && quantity !in it.value.quantities }
            .map { "$where: ${it.key} 에 quantity=\"$quantity\"가 없다" }

    private fun Strings.escapes(where: String): List<String> = if ("composeResources" !in where) {
        emptyList()
    } else {
        entries.filter { it.value.escaped }.map { "$where: ${it.key} 에 역슬래시 따옴표가 있다. 그냥 따옴표로 쓴다" }
    }

    private fun parse(file: File): Strings {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val root = document.documentElement
        val entries = mutableMapOf<String, Entry>()
        val children = root.childNodes
        for (i in 0 until children.length) {
            val element = children.item(i) as? Element ?: continue
            val name = element.getAttribute("name")
            val items = element.getElementsByTagName("item")
            val texts = if (items.length == 0) listOf(element.textContent) else (0 until items.length).map { items.item(it).textContent }
            val quantities = (0 until items.length).map { (items.item(it) as Element).getAttribute("quantity") }.toSet()
            entries[name] =
                Entry(
                    element.tagName,
                    texts.flatMap {
                        PLACEHOLDER.findAll(it).map { m -> m.value }.toList()
                    }.toSet(),
                    quantities,
                    texts.any(ESCAPED::containsMatchIn)
                )
        }
        return Strings(entries)
    }

    private class Strings(val entries: Map<String, Entry>)

    private class Entry(val kind: String, val placeholders: Set<String>, val quantities: Set<String>, val escaped: Boolean)

    private companion object {
        val LOCALES = listOf("ko", "ja")
        val PLACEHOLDER = Regex("""%\d+\$[sd]""")
        val ESCAPED = Regex("""\\['"]""")
    }
}
