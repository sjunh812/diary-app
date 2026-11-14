import kotlin.test.Test
import kotlin.test.assertEquals

// 안 쓰는 import 검사: 이름이 본문에 없으면 걸리고, 별칭·KDoc·연산자 위임은 통과
class UnusedImportCheckTaskTest {
    private fun unused(text: String) = UnusedImportCheckTask.unusedImports(text.trimIndent()).map { it.second }

    @Test
    fun an_import_whose_name_never_appears_is_unused() {
        val text =
            """
            package a

            import java.io.File
            import kotlinx.coroutines.delay

            suspend fun wait() = delay(1)
            """
        assertEquals(listOf("File"), unused(text))
    }

    @Test
    fun an_alias_is_checked_by_its_alias() {
        val text =
            """
            import a.b.Long as Big
            import a.b.Short as Small

            val x: Big = 1
            """
        assertEquals(listOf("Small"), unused(text))
    }

    @Test
    fun kdoc_references_and_delegates_count_as_used() {
        val text =
            """
            import androidx.compose.runtime.getValue
            import a.b.Helper

            /** [Helper]를 씀 */
            val x by lazy { 1 }
            """
        assertEquals(emptyList(), unused(text))
    }

    @Test
    fun a_name_inside_a_longer_word_is_not_a_use() {
        val text =
            """
            import a.b.Sink

            val bufferedSinkName = "x"
            """
        assertEquals(listOf("Sink"), unused(text))
    }

    /** 정상 코드를 막지 않음: 확장 속성, 문자열 틀, 참조, 백틱 이름, 연산자 */
    @Test
    fun valid_uses_are_not_flagged() {
        val text =
            """
            import kotlin.time.Duration.Companion.seconds
            import a.b.label
            import a.b.helper
            import a.b.`odd name`
            import a.b.invoke
            import a.b.Holder

            val wait = 5.seconds
            val text = "${'$'}label"
            val ref = ::helper
            val odd = `odd name`()
            val run = Holder()()
            """
        assertEquals(emptyList(), unused(text))
    }
}
